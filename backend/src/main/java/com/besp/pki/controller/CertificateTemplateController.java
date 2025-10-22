package com.besp.pki.controller;

import com.besp.pki.dto.CertificateTemplateResponse;
import com.besp.pki.dto.CreateCertificateTemplateRequest;
import com.besp.pki.service.CertificateTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/ca-templates")
public class CertificateTemplateController {

    private final CertificateTemplateService service;

    public CertificateTemplateController(CertificateTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<CertificateTemplateResponse> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public CertificateTemplateResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<CertificateTemplateResponse> create(@Valid @RequestBody CreateCertificateTemplateRequest req) {
        var created = service.create(req);
        return ResponseEntity.created(URI.create("/api/ca-templates/" + created.getId())).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
