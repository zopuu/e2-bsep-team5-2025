package com.besp.pki.service;

import com.besp.pki.dto.*;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.x509.CsrParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsrService {
    
    private static final Logger log = LoggerFactory.getLogger(CsrService.class);
    
    private final CertificateRecordRepository certificateRecordRepository;
    
    public CsrService(CertificateRecordRepository certificateRecordRepository) {
        this.certificateRecordRepository = certificateRecordRepository;
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
}
