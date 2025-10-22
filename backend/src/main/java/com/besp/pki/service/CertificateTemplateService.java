package com.besp.pki.service;

import com.besp.pki.dto.CertificateTemplateResponse;
import com.besp.pki.dto.CreateCertificateTemplateRequest;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.entity.CertificateTemplate;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.repository.CertificateTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class CertificateTemplateService {

    private final CertificateTemplateRepository repo;
    private final CertificateRecordRepository certRepo;

    public CertificateTemplateService(CertificateTemplateRepository repo,
                                      CertificateRecordRepository certRepo) {
        this.repo = repo;
        this.certRepo = certRepo;
    }

    public CertificateTemplateResponse create(CreateCertificateTemplateRequest req) {
        // regex sintaksna validacija (bez testiranja match-a)
        try { Pattern.compile(req.getCnRegex()); } catch (Exception e) { throw new IllegalArgumentException("Invalid CN regex"); }
        try { Pattern.compile(req.getSanRegex()); } catch (Exception e) { throw new IllegalArgumentException("Invalid SAN regex"); }

        CertificateRecord issuer = certRepo.findById(req.getIssuerCertificateId())
                .orElseThrow(() -> new IllegalArgumentException("Issuer certificate not found"));

        // TODO: opciono: validiraj da issuer NIJE povučen, da je CA cert, i da policy dozvoljava ekstenzije
        // checkPolicyNotExceeded(issuer, req.getKeyUsages(), req.getExtendedKeyUsages(), req.getTtlDays());

        if (repo.findByName(req.getName()).isPresent()) {
            throw new IllegalArgumentException("Template name already exists");
        }

        CertificateTemplate t = new CertificateTemplate();
        t.setName(req.getName());
        t.setIssuerCertificate(issuer);
        t.setCnRegex(req.getCnRegex());
        t.setSanRegex(req.getSanRegex());
        t.setTtlDays(req.getTtlDays());
        t.setKeyUsages(req.getKeyUsages());
        t.setExtendedKeyUsages(req.getExtendedKeyUsages());

        t = repo.save(t);
        return map(t);
    }

    public List<CertificateTemplateResponse> listAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    public CertificateTemplateResponse get(Long id) {
        return repo.findById(id).map(this::map)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private CertificateTemplateResponse map(CertificateTemplate t) {
        CertificateTemplateResponse r = new CertificateTemplateResponse();
        r.setId(t.getId());
        r.setName(t.getName());
        r.setIssuerCertificateId(t.getIssuerCertificate().getId());
        r.setCnRegex(t.getCnRegex());
        r.setSanRegex(t.getSanRegex());
        r.setTtlDays(t.getTtlDays());
        r.setKeyUsages(t.getKeyUsages());
        r.setExtendedKeyUsages(t.getExtendedKeyUsages());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}
