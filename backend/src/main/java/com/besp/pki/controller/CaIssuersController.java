package com.besp.pki.controller;

import com.besp.pki.dto.CaIssuerDto;
import com.besp.pki.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ca")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CaIssuersController {

    private final CertificateService certificateService;

    public CaIssuersController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/issuers")
    @PreAuthorize("hasAnyRole('CA_USER','ADMIN')")
    public ResponseEntity<List<CaIssuerDto>> listMyIssuers(Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        var dto = certificateService.findActiveIssuersForCaUserEmail(email);
        return ResponseEntity.ok(dto);
    }
}
