package com.besp.pki.service;

import com.besp.pki.dto.*;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.CertificateType;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.repository.UserRepository;
import com.besp.pki.x509.CsrParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        
        // Set fingerprint
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
            
            // Create mock PEM content for now
            String pemContent = createMockPemContent(cert);
            
            // Write PEM file to keystores directory
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(keystoreDir));
            java.nio.file.Files.write(java.nio.file.Path.of(pemFilePath), pemContent.getBytes());
            
            // Set metadata - pointing to .pem file instead of .p12
            cert.setKeystorePath(pemFilePath);
            cert.setKeystoreAlias(alias);
            cert.setEncKeystorePass(cryptoSealService.seal(keystorePass));
            
            // Store PEM content in database as well
            cert.setCertificatePem(pemContent);
            
            log.info("Created EE certificate as PEM file: {}", pemFilePath);
            
        } catch (Exception e) {
            log.error("Failed to create PEM file: {}", e.getMessage());
            throw new RuntimeException("Failed to create PEM file", e);
        }
        
        return cert;
    }
    
    
    private String createMockPemContent(CertificateRecord cert) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN CERTIFICATE-----\n");
        pem.append("MOCK EE CERTIFICATE\n");
        pem.append("Serial Number: ").append(cert.getSerialNumber()).append("\n");
        pem.append("Subject: ").append(cert.getSubjectDn()).append("\n");
        pem.append("Issuer: ").append(cert.getIssuerDn()).append("\n");
        pem.append("Not Before: ").append(cert.getNotBefore()).append("\n");
        pem.append("Not After: ").append(cert.getNotAfter()).append("\n");
        pem.append("Key Algorithm: ").append(cert.getPublicKeyAlgorithm()).append("\n");
        pem.append("Key Size: ").append(cert.getKeySize()).append("\n");
        pem.append("Signature Algorithm: ").append(cert.getSignatureAlgorithm()).append("\n");
        pem.append("Fingerprint: ").append(cert.getFingerprintSha256()).append("\n");
        pem.append("-----END CERTIFICATE-----\n");
        return pem.toString();
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
