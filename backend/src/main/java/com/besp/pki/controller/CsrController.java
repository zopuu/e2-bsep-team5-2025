package com.besp.pki.controller;

import com.besp.pki.dto.*;
import com.besp.pki.service.CsrService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/csr")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CsrController {
    
    private static final Logger log = LoggerFactory.getLogger(CsrController.class);
    
    private final CsrService csrService;
    
    public CsrController(CsrService csrService) {
        this.csrService = csrService;
    }
    
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<CsrUploadResponse> uploadCsr(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received CSR upload request for file: {}", file.getOriginalFilename());
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(CsrUploadResponse.error("File is empty"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".pem") && 
                !file.getOriginalFilename().toLowerCase().endsWith(".csr")) {
                return ResponseEntity.badRequest()
                    .body(CsrUploadResponse.error("File must be a .pem or .csr file"));
            }
            
            // Read file content
            String pemContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            // Parse CSR
            CsrUploadResponse response = csrService.parseCsr(pemContent);
            
            if (response.isSuccess()) {
                log.info("CSR uploaded and parsed successfully");
                return ResponseEntity.ok(response);
            } else {
                log.error("CSR parsing failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(CsrUploadResponse.error("Failed to read uploaded file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during CSR upload: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(CsrUploadResponse.error("Unexpected error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/parse")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<CsrUploadResponse> parseCsr(@Valid @RequestBody CsrUploadRequest request) {
        try {
            log.info("Received CSR parse request");
            
            // Parse CSR content
            CsrUploadResponse response = csrService.parseCsr(request.getCsrContent());
            
            if (response.isSuccess()) {
                log.info("CSR parsed successfully");
                return ResponseEntity.ok(response);
            } else {
                log.error("CSR parsing failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during CSR parsing: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(CsrUploadResponse.error("Unexpected error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/ca-certificates")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<List<CaIssuerDto>> getCaCertificates() {
        try {
            log.info("Getting available CA certificates");
            
            List<CaIssuerDto> caCertificates = csrService.getAvailableCaCertificates();
            
            log.info("Found {} available CA certificates", caCertificates.size());
            return ResponseEntity.ok(caCertificates);
            
        } catch (Exception e) {
            log.error("Failed to get CA certificates: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<CertificateIssueResponse> issueCertificate(@Valid @RequestBody CertificateIssueRequest request) {
        try {
            log.info("Received certificate issue request for CN: {}", request.getCsrData().getCommonName());
            
            CertificateIssueResponse response = csrService.issueCertificate(request);
            
            if (response.isSuccess()) {
                log.info("Certificate issued successfully with ID: {}", response.getCertificateId());
                return ResponseEntity.ok(response);
            } else {
                log.error("Certificate issuance failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during certificate issuance: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(CertificateIssueResponse.error("Unexpected error: " + e.getMessage()));
        }
    }
}
