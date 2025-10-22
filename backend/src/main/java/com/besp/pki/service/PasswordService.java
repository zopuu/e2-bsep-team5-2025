package com.besp.pki.service;

import com.besp.pki.dto.PasswordEntryDto;
import com.besp.pki.dto.PasswordEntryRequest;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.entity.PasswordEntry;
import com.besp.pki.entity.User;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.repository.PasswordEntryRepository;
import com.besp.pki.repository.UserRepository;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.io.StringReader;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PasswordService {
    
    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final SecureRandom RNG = new SecureRandom();
    
    private final PasswordEntryRepository passwordEntryRepository;
    private final UserRepository userRepository;
    private final CertificateRecordRepository certificateRepository;
    private final KeyStoreService keyStoreService;
    private final CryptoSealService cryptoSealService;
    
    public PasswordService(PasswordEntryRepository passwordEntryRepository,
                          UserRepository userRepository,
                          CertificateRecordRepository certificateRepository,
                          KeyStoreService keyStoreService,
                          CryptoSealService cryptoSealService) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.userRepository = userRepository;
        this.certificateRepository = certificateRepository;
        this.keyStoreService = keyStoreService;
        this.cryptoSealService = cryptoSealService;
    }
    
    public List<PasswordEntryDto> getPasswordsForUser(String userEmail) {
        log.info("Getting passwords for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        List<PasswordEntry> entries = passwordEntryRepository.findByOwnerOrderByCreatedAtDesc(user);
        
        return entries.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public PasswordEntryDto createPassword(PasswordEntryRequest request, String userEmail) {
        log.info("Creating password entry for user: {}, site: {}", userEmail, request.getSiteName());
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        CertificateRecord certificate = certificateRepository.findById(request.getCertificateId())
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + request.getCertificateId()));
        
        // Verify certificate belongs to user
        if (!certificate.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Certificate does not belong to user");
        }
        
        // Encrypt password using public key from certificate
        String encryptedPassword = encryptPassword(request.getPassword(), certificate);
        
        PasswordEntry entry = new PasswordEntry();
        entry.setSiteName(request.getSiteName());
        entry.setUsername(request.getUsername());
        entry.setEncryptedPassword(encryptedPassword);
        entry.setNotes(request.getNotes());
        entry.setOwner(user);
        entry.setCertificate(certificate);
        
        PasswordEntry savedEntry = passwordEntryRepository.save(entry);
        
        log.info("Password entry created successfully with ID: {}", savedEntry.getId());
        
        return convertToDto(savedEntry);
    }
    
    public void deletePassword(Long passwordId, String userEmail) {
        log.info("Deleting password entry ID: {} for user: {}", passwordId, userEmail);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        PasswordEntry entry = passwordEntryRepository.findByIdAndOwner(passwordId, user)
                .orElseThrow(() -> new IllegalArgumentException("Password entry not found or access denied"));
        
        passwordEntryRepository.delete(entry);
        
        log.info("Password entry deleted successfully");
    }
    
    public List<PasswordEntryDto> searchPasswords(String userEmail, String searchTerm) {
        log.info("Searching passwords for user: {}, term: {}", userEmail, searchTerm);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        List<PasswordEntry> entries = passwordEntryRepository.findByOwnerAndSearchTerm(user, searchTerm);
        
        return entries.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private String encryptPassword(String password, CertificateRecord certificate) {
        try {
            // Extract public key from certificate PEM
            PublicKey publicKey = extractPublicKeyFromCertificate(certificate);
            
            // Use RSA encryption with OAEP padding
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes());
            
            String encrypted = java.util.Base64.getEncoder().encodeToString(encryptedBytes);
            
            log.debug("Password encrypted successfully for certificate: {}", certificate.getId());
            return encrypted;
            
        } catch (Exception e) {
            log.error("Failed to encrypt password: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }
    
    private PublicKey extractPublicKeyFromCertificate(CertificateRecord certificate) {
        try {
            // For End Entity certificates, read from PEM file
            if (certificate.getCertificatePem() != null) {
                return extractPublicKeyFromPem(certificate.getCertificatePem());
            } else {
                // Fallback to keystore
                char[] keystorePass = cryptoSealService.unseal(certificate.getEncKeystorePass()).toCharArray();
                return keyStoreService.readCertificate(certificate.getKeystorePath(), 
                                                     certificate.getKeystoreAlias(), 
                                                     keystorePass).getPublicKey();
            }
            
        } catch (Exception e) {
            log.error("Failed to extract public key from certificate: {}", e.getMessage());
            throw new RuntimeException("Failed to extract public key", e);
        }
    }
    
    private PublicKey extractPublicKeyFromPem(String pemContent) {
        try {
            log.debug("Attempting to parse PEM content: {}", pemContent.substring(0, Math.min(200, pemContent.length())));
            
            // Parse PEM certificate using BouncyCastle
            StringReader reader = new StringReader(pemContent);
            PEMParser pemParser = new PEMParser(reader);
            
            Object pemObject = pemParser.readObject();
            pemParser.close();
            
            log.debug("PEM object type: {}", pemObject != null ? pemObject.getClass().getSimpleName() : "null");
            
            if (pemObject instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) pemObject;
                return cert.getPublicKey();
            } else if (pemObject instanceof org.bouncycastle.cert.X509CertificateHolder) {
                // Convert BouncyCastle X509CertificateHolder to Java X509Certificate
                org.bouncycastle.cert.X509CertificateHolder certHolder = (org.bouncycastle.cert.X509CertificateHolder) pemObject;
                JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
                converter.setProvider("BC");
                X509Certificate cert = converter.getCertificate(certHolder);
                return cert.getPublicKey();
            } else {
                log.error("PEM object is not X509Certificate or X509CertificateHolder, it's: {}", pemObject != null ? pemObject.getClass().getName() : "null");
                throw new RuntimeException("PEM content is not a valid X.509 certificate");
            }
            
        } catch (Exception e) {
            log.error("Failed to parse PEM content: {}", e.getMessage());
            log.error("PEM content preview: {}", pemContent.substring(0, Math.min(500, pemContent.length())));
            throw new RuntimeException("Failed to parse PEM content", e);
        }
    }
    
    
    private PasswordEntryDto convertToDto(PasswordEntry entry) {
        return new PasswordEntryDto(
                entry.getId(),
                entry.getSiteName(),
                entry.getUsername(),
                entry.getEncryptedPassword(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.getCertificate().getId(),
                entry.getCertificate().getSubjectDn()
        );
    }
}
