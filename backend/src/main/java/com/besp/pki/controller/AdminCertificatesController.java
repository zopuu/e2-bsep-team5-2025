package com.besp.pki.controller;

import com.besp.pki.dto.RootCaRequest;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.service.CertificateService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminCertificatesController {
    private final CertificateService service;
    public AdminCertificatesController(CertificateService service) {
        this.service = service;
    }

    @PostMapping("/ca/root")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoot(@Valid @RequestBody RootCaRequest req, Authentication auth) throws Exception {
        String adminEmail = auth != null ? auth.getName() : null;
        CertificateRecord rec = service.createRootCa(req,adminEmail);

        return ResponseEntity.ok(Map.of(
                "id", rec.getId(),
                "serial", rec.getSerialNumber(),
                "subject", rec.getSubjectDn(),
                "fingerprint", rec.getFingerprintSha256(),
                "validFrom", rec.getNotBefore(),
                "validTo", rec.getNotAfter()
        ));
    }

    @GetMapping("/cert/{id}/pem")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> downloadPem(@PathVariable Long id) throws Exception {
        String pem = service.exportCertificatePem(id);
        byte[] body = pem.getBytes(StandardCharsets.US_ASCII);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/x-pem-file"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "atachment; filename=\"certificate-" + id + ".pem\"")
                .body(body);
    }
    @GetMapping("/certificates")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
