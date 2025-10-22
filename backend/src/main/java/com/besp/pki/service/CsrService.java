package com.besp.pki.service;

import com.besp.pki.dto.*;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.CertificateType;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.repository.UserRepository;
import com.besp.pki.x509.CsrParser;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CsrService {
    
    private static final Logger log = LoggerFactory.getLogger(CsrService.class);
    
    private final CertificateRecordRepository certificateRecordRepository;
    private final UserRepository userRepository;
    private final KeyStoreService keyStoreService;
    private final CryptoSealService cryptoSealService;
    
    @Value("${pki.keystore-dir:./data/keystores}")
    private String keystoreDir;
    
    private static final SecureRandom RNG = new SecureRandom();
    
    public CsrService(CertificateRecordRepository certificateRecordRepository,
                      UserRepository userRepository,
                      KeyStoreService keyStoreService,
                      CryptoSealService cryptoSealService) {
        this.certificateRecordRepository = certificateRecordRepository;
        this.userRepository = userRepository;
        this.keyStoreService = keyStoreService;
        this.cryptoSealService = cryptoSealService;
    }
    
    public CsrUploadResponse parseCsr(String pemContent) {
        try {
            log.info("Parsing CSR content");
            
            // Parse CSR content
            CsrData csrData = CsrParser.parseCsr(pemContent);
            
            // Validate CSR data
            validateCsrData(csrData);
            
            log.info("CSR parsed successfully for CN: {}", csrData.getCommonName());
            
            return CsrUploadResponse.success("CSR parsed successfully", csrData);
            
        } catch (Exception e) {
            log.error("Failed to parse CSR: {}", e.getMessage());
            return CsrUploadResponse.error("Failed to parse CSR: " + e.getMessage());
        }
    }
    
    public List<CaIssuerDto> getAvailableCaCertificates() {
        log.info("Getting available CA certificates");
        
        Instant now = Instant.now();
        
        // Get all active CA certificates that can be used for signing
        List<CertificateRecord> caCertificates = certificateRecordRepository.findAll()
            .stream()
            .filter(cert -> cert.isCa())
            .filter(cert -> cert.getStatus() == CertificateStatus.ACTIVE)
            .filter(cert -> cert.getNotBefore().isBefore(now))
            .filter(cert -> cert.getNotAfter().isAfter(now))
            .collect(Collectors.toList());
        
        // Convert to DTO
        return caCertificates.stream()
            .map(cert -> new CaIssuerDto(
                cert.getId(),
                cert.getSubjectDn(),
                cert.getIssuerDn(),
                cert.getSerialNumber(),
                cert.isCa(),
                cert.getNotBefore(),
                cert.getNotAfter()
            ))
            .collect(Collectors.toList());
    }
    
    public CertificateIssueResponse issueCertificate(CertificateIssueRequest request, String currentUserEmail) {
        try {
            log.info("Issuing certificate for CSR with CN: {}", request.getCsrData().getCommonName());
            
            // Validate CA certificate
            Optional<CertificateRecord> caCertOpt = certificateRecordRepository.findById(request.getCaCertificateId());
            if (caCertOpt.isEmpty()) {
                return CertificateIssueResponse.error("CA certificate not found");
            }
            
            CertificateRecord caCert = caCertOpt.get();
            
            // Validate CA certificate is active and not expired
            Instant now = Instant.now();
            if (caCert.getStatus() != CertificateStatus.ACTIVE) {
                return CertificateIssueResponse.error("CA certificate is not active");
            }
            
            if (caCert.getNotAfter().isBefore(now)) {
                return CertificateIssueResponse.error("CA certificate has expired");
            }
            
            // Validate validity period doesn't exceed CA certificate validity
            Instant requestedNotAfter = now.plus(request.getValidityDays(), ChronoUnit.DAYS);
            if (requestedNotAfter.isAfter(caCert.getNotAfter())) {
                return CertificateIssueResponse.error("Requested validity period exceeds CA certificate validity");
            }
            
                // Create certificate record and store in keystore
                CertificateRecord newCert = createEndEntityCertificate(request, caCert, currentUserEmail);
            CertificateRecord savedCert = certificateRecordRepository.save(newCert);
            
            log.info("Certificate issued successfully with ID: {}", savedCert.getId());
            
            return CertificateIssueResponse.success(
                "Certificate issued successfully",
                savedCert.getId(),
                savedCert.getSerialNumber(),
                savedCert.getSubjectDn(),
                savedCert.getIssuerDn(),
                savedCert.getNotBefore(),
                savedCert.getNotAfter(),
                savedCert.getFingerprintSha256()
            );
            
        } catch (Exception e) {
            log.error("Failed to issue certificate: {}", e.getMessage());
            return CertificateIssueResponse.error("Failed to issue certificate: " + e.getMessage());
        }
    }
    
    private CertificateRecord createEndEntityCertificate(CertificateIssueRequest request, CertificateRecord caCert, String currentUserEmail) {
        CertificateRecord cert = new CertificateRecord();
        
        // Generate serial number
        cert.setSerialNumber("EE-" + System.currentTimeMillis());
        
        // Set certificate type
        cert.setType(CertificateType.END_ENTITY);
        
        // Set subject DN from CSR data
        StringBuilder subjectDn = new StringBuilder();
        CsrData csrData = request.getCsrData();
        
        if (csrData.getCommonName() != null) {
            subjectDn.append("CN=").append(csrData.getCommonName());
        }
        if (csrData.getOrganization() != null) {
            subjectDn.append(",O=").append(csrData.getOrganization());
        }
        if (csrData.getOrganizationalUnit() != null) {
            subjectDn.append(",OU=").append(csrData.getOrganizationalUnit());
        }
        if (csrData.getLocality() != null) {
            subjectDn.append(",L=").append(csrData.getLocality());
        }
        if (csrData.getState() != null) {
            subjectDn.append(",ST=").append(csrData.getState());
        }
        if (csrData.getCountry() != null) {
            subjectDn.append(",C=").append(csrData.getCountry());
        }
        if (csrData.getEmailAddress() != null) {
            subjectDn.append(",EMAILADDRESS=").append(csrData.getEmailAddress());
        }
        
        cert.setSubjectDn(subjectDn.toString());
        cert.setIssuerDn(caCert.getSubjectDn());
        
        // Set validity period
        Instant now = Instant.now();
        cert.setNotBefore(now);
        cert.setNotAfter(now.plus(request.getValidityDays(), ChronoUnit.DAYS));
        
        // Set fingerprint (will be updated with real certificate)
        cert.setFingerprintSha256("ee-fingerprint-" + System.currentTimeMillis());
        
        // Set signature algorithm
        cert.setSignatureAlgorithm("SHA256withRSA");
        
        // Set public key info from CSR
        cert.setPublicKeyAlgorithm(csrData.getPublicKeyAlgorithm());
        cert.setKeySize(csrData.getPublicKeySize());
        
        // Set as non-CA certificate
        cert.setCa(false);
        
        // Set status
        cert.setStatus(CertificateStatus.ACTIVE);
        
        // Set issuer reference
        cert.setIssuer(caCert);
        
            // Set audit fields
            cert.setCreatedAt(now);
            cert.setCreatedBy("CSR-ISSUE");
            
            // Set owner to current user (who uploaded CSR)
            if (currentUserEmail != null) {
                userRepository.findByEmail(currentUserEmail).ifPresent(user -> {
                    cert.setOwner(user);
                    log.info("Setting owner to current user: {} (ID: {})", currentUserEmail, user.getId());
                });
            }
        
        // Create keystore entry - SAME AS ROOT/INTERMEDIATE
        String alias = "ee-" + System.currentTimeMillis();
        String keystorePass = generateRandomPassword(24);
        
        try {
            // Create .pem file in keystores directory
            String pemFileName = alias + ".pem";
            String pemFilePath = keystoreDir + "/" + pemFileName;
            
            // Create real X.509 certificate
            String pemContent = createRealX509Certificate(request, caCert);
            
            // Write PEM file to keystores directory
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(keystoreDir));
            java.nio.file.Files.write(java.nio.file.Path.of(pemFilePath), pemContent.getBytes());
            
            // Set metadata - pointing to .pem file instead of .p12
            cert.setKeystorePath(pemFilePath);
            cert.setKeystoreAlias(alias);
            cert.setEncKeystorePass(cryptoSealService.seal(keystorePass));
            
            // Store PEM content in database as well
            cert.setCertificatePem(pemContent);
            
            // Update fingerprint with real certificate fingerprint
            try {
                X509Certificate realCert = parseCertificateFromPem(pemContent);
                cert.setFingerprintSha256(calculateFingerprint(realCert));
            } catch (Exception e) {
                log.warn("Could not calculate real fingerprint: {}", e.getMessage());
            }
            
            log.info("Created EE certificate as PEM file: {}", pemFilePath);
            
        } catch (Exception e) {
            log.error("Failed to create PEM file: {}", e.getMessage());
            throw new RuntimeException("Failed to create PEM file", e);
        }
        
        return cert;
    }
    
    
    private String createRealX509Certificate(CertificateIssueRequest request, CertificateRecord caCert) throws Exception {
        try {
            // Get CA private key and certificate
            char[] caPass = cryptoSealService.unseal(caCert.getEncKeystorePass()).toCharArray();
            PrivateKey caPrivateKey = keyStoreService.readPrivateKey(caCert.getKeystorePath(), caCert.getKeystoreAlias(), caPass);
            X509Certificate caCertificate = keyStoreService.readCertificate(caCert.getKeystorePath(), caCert.getKeystoreAlias(), caPass);
            
            // Extract public key from CSR
            CsrData csrData = request.getCsrData();
            PublicKey subjectPublicKey = extractPublicKeyFromCsr(csrData.getPublicKeyPem());
            
            // Build subject DN from CSR data
            X500Name subjectDN = buildSubjectDN(csrData);
            X500Name issuerDN = X500Name.getInstance(caCertificate.getSubjectX500Principal().getEncoded());
            
            // Generate serial number
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
            
            // Set validity period
            Instant now = Instant.now();
            Date notBefore = Date.from(now);
            Date notAfter = Date.from(now.plus(request.getValidityDays(), ChronoUnit.DAYS));
            
            // Create certificate builder
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerDN, serial, notBefore, notAfter, subjectDN, subjectPublicKey);
            
            // Add extensions
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            builder.addExtension(org.bouncycastle.asn1.x509.Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(subjectPublicKey));
            builder.addExtension(org.bouncycastle.asn1.x509.Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(caCertificate));
            
            // Add Key Usage extension
            builder.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage, false,
                new org.bouncycastle.asn1.x509.KeyUsage(
                    org.bouncycastle.asn1.x509.KeyUsage.digitalSignature |
                    org.bouncycastle.asn1.x509.KeyUsage.keyEncipherment));
            
            // Create content signer
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(caPrivateKey);
            
            // Build and convert certificate
            X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer));
            
            // Verify certificate
            certificate.verify(caCertificate.getPublicKey());
            
            // Convert to PEM format
            return com.besp.pki.x509.PemUtil.toPem(certificate);
            
        } catch (Exception e) {
            log.error("Failed to create real X.509 certificate: {}", e.getMessage(), e);
            throw new Exception("Failed to create certificate: " + e.getMessage(), e);
        }
    }
    
    private PublicKey extractPublicKeyFromCsr(String publicKeyPem) throws Exception {
        try {
            // Remove PEM headers and decode base64
            String base64Content = publicKeyPem
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            
            byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Content);
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
            
        } catch (Exception e) {
            throw new Exception("Failed to extract public key from CSR: " + e.getMessage(), e);
        }
    }
    
    private X500Name buildSubjectDN(CsrData csrData) {
        StringBuilder dn = new StringBuilder();
        
        if (csrData.getCommonName() != null) {
            dn.append("CN=").append(csrData.getCommonName());
        }
        if (csrData.getOrganization() != null) {
            if (dn.length() > 0) dn.append(",");
            dn.append("O=").append(csrData.getOrganization());
        }
        if (csrData.getOrganizationalUnit() != null) {
            if (dn.length() > 0) dn.append(",");
            dn.append("OU=").append(csrData.getOrganizationalUnit());
        }
        if (csrData.getLocality() != null) {
            if (dn.length() > 0) dn.append(",");
            dn.append("L=").append(csrData.getLocality());
        }
        if (csrData.getState() != null) {
            if (dn.length() > 0) dn.append(",");
            dn.append("ST=").append(csrData.getState());
        }
        if (csrData.getCountry() != null) {
            if (dn.length() > 0) dn.append(",");
            dn.append("C=").append(csrData.getCountry());
        }
        if (csrData.getEmailAddress() != null) {
            if (dn.length() > 0) dn.append(",");
            dn.append("EMAILADDRESS=").append(csrData.getEmailAddress());
        }
        
        return new X500Name(dn.toString());
    }
    
    private String convertCertificateToPem(X509Certificate certificate) throws Exception {
        try {
            byte[] certBytes = certificate.getEncoded();
            String base64 = java.util.Base64.getEncoder().encodeToString(certBytes);
            
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN CERTIFICATE-----\n");
            
            // Split base64 into 64-character lines
            for (int i = 0; i < base64.length(); i += 64) {
                int end = Math.min(i + 64, base64.length());
                pem.append(base64.substring(i, end)).append("\n");
            }
            
        pem.append("-----END CERTIFICATE-----\n");
        return pem.toString();
            
        } catch (Exception e) {
            throw new Exception("Failed to convert certificate to PEM: " + e.getMessage(), e);
        }
    }
    
    private X509Certificate parseCertificateFromPem(String pemContent) throws Exception {
        try {
            // Remove PEM headers and decode base64
            String base64Content = pemContent
                .replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
            
            byte[] certBytes = java.util.Base64.getDecoder().decode(base64Content);
            java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
            
        } catch (Exception e) {
            throw new Exception("Failed to parse certificate from PEM: " + e.getMessage(), e);
        }
    }
    
    private String calculateFingerprint(X509Certificate certificate) throws Exception {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getEncoded());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new Exception("Failed to calculate fingerprint: " + e.getMessage(), e);
        }
    }
    
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(RNG.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    private void validateCsrData(CsrData csrData) throws Exception {
        if (csrData.getCommonName() == null || csrData.getCommonName().trim().isEmpty()) {
            throw new Exception("Common Name (CN) is required");
        }
        
        if (csrData.getPublicKeyPem() == null || csrData.getPublicKeyPem().trim().isEmpty()) {
            throw new Exception("Public key is required");
        }
        
        // Additional validations can be added here
        log.debug("CSR data validation passed");
    }
    
    public List<CaIssuerDto> getMyCertificates(String userEmail) {
        log.info("Getting certificates for user: {}", userEmail);
        
        return userRepository.findByEmail(userEmail)
            .map(user -> {
                log.info("Found user: {} with ID: {}", userEmail, user.getId());
                List<CaIssuerDto> certificates = certificateRecordRepository.findByOwnerAndType(user, CertificateType.END_ENTITY);
                log.info("Found {} End-Entity certificates for user", certificates.size());
                return certificates;
            })
            .orElse(List.of());
    }
}
