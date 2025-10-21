package com.besp.pki.x509;

import com.besp.pki.dto.CsrData;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

public class CsrParser {
    
    private static final Logger log = LoggerFactory.getLogger(CsrParser.class);
    
    public static CsrData parseCsr(String pemContent) throws Exception {
        try {
            // Parse PEM content to PKCS10CertificationRequest
            PKCS10CertificationRequest csr = parsePemContent(pemContent);
            
            // Extract X500Name data
            X500Name subject = csr.getSubject();
            String commonName = extractRDN(subject, BCStyle.CN);
            String organization = extractRDN(subject, BCStyle.O);
            String organizationalUnit = extractRDN(subject, BCStyle.OU);
            String locality = extractRDN(subject, BCStyle.L);
            String state = extractRDN(subject, BCStyle.ST);
            String country = extractRDN(subject, BCStyle.C);
            String emailAddress = extractRDN(subject, BCStyle.EmailAddress);
            
            // Extract public key info
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PublicKey publicKey = converter.getPublicKey(csr.getSubjectPublicKeyInfo());
            
            String publicKeyAlgorithm = publicKey.getAlgorithm();
            int publicKeySize = getKeySize(publicKey);
            String publicKeyPem = convertPublicKeyToPem(publicKey);
            
            // Extract extensions (basic implementation)
            List<String> subjectAlternativeNames = extractSubjectAlternativeNames(csr);
            String keyUsage = extractKeyUsage(csr);
            String extendedKeyUsage = extractExtendedKeyUsage(csr);
            
            return new CsrData(
                commonName, organization, organizationalUnit,
                locality, state, country, emailAddress,
                publicKeyAlgorithm, publicKeySize, publicKeyPem,
                subjectAlternativeNames, keyUsage, extendedKeyUsage
            );
            
        } catch (Exception e) {
            log.error("Failed to parse CSR: {}", e.getMessage());
            throw new Exception("Failed to parse CSR: " + e.getMessage(), e);
        }
    }
    
    private static PKCS10CertificationRequest parsePemContent(String pemContent) throws Exception {
        try {
            // Remove PEM headers and decode base64
            String base64Content = pemContent
                .replaceAll("-----BEGIN CERTIFICATE REQUEST-----", "")
                .replaceAll("-----END CERTIFICATE REQUEST-----", "")
                .replaceAll("\\s", "");
            
            byte[] csrBytes = java.util.Base64.getDecoder().decode(base64Content);
            
            // Create PKCS10CertificationRequest from bytes
            return new PKCS10CertificationRequest(csrBytes);
            
        } catch (Exception e) {
            throw new Exception("Invalid PEM format: " + e.getMessage(), e);
        }
    }
    
    private static String extractRDN(X500Name subject, org.bouncycastle.asn1.ASN1ObjectIdentifier oid) {
        try {
            return subject.getRDNs(oid)[0].getFirst().getValue().toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private static List<String> extractSubjectAlternativeNames(PKCS10CertificationRequest csr) {
        // Basic implementation - can be extended
        List<String> sans = new ArrayList<>();
        // TODO: Implement SAN extraction from CSR extensions
        return sans;
    }
    
    private static String extractKeyUsage(PKCS10CertificationRequest csr) {
        // Basic implementation - can be extended
        // TODO: Implement Key Usage extraction from CSR extensions
        return "digitalSignature, keyEncipherment";
    }
    
    private static String extractExtendedKeyUsage(PKCS10CertificationRequest csr) {
        // Basic implementation - can be extended
        // TODO: Implement Extended Key Usage extraction from CSR extensions
        return "serverAuth, clientAuth";
    }
    
    private static int getKeySize(PublicKey publicKey) {
        try {
            if (publicKey.getAlgorithm().equals("RSA")) {
                java.security.interfaces.RSAPublicKey rsaKey = (java.security.interfaces.RSAPublicKey) publicKey;
                return rsaKey.getModulus().bitLength();
            } else if (publicKey.getAlgorithm().equals("EC")) {
                java.security.interfaces.ECPublicKey ecKey = (java.security.interfaces.ECPublicKey) publicKey;
                return ecKey.getParams().getOrder().bitLength();
            }
        } catch (Exception e) {
            log.warn("Could not determine key size: {}", e.getMessage());
        }
        return 0;
    }
    
    private static String convertPublicKeyToPem(PublicKey publicKey) {
        try {
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(publicKey.getEncoded());
            String base64 = java.util.Base64.getEncoder().encodeToString(spec.getEncoded());
            
            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN PUBLIC KEY-----\n");
            
            // Split base64 into 64-character lines
            for (int i = 0; i < base64.length(); i += 64) {
                int end = Math.min(i + 64, base64.length());
                pem.append(base64.substring(i, end)).append("\n");
            }
            
            pem.append("-----END PUBLIC KEY-----\n");
            return pem.toString();
        } catch (Exception e) {
            log.error("Failed to convert public key to PEM: {}", e.getMessage());
            return "Error converting public key to PEM";
        }
    }
}
