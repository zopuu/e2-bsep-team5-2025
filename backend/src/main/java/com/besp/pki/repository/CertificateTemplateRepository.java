package com.besp.pki.repository;

import com.besp.pki.entity.CertificateTemplate;
import com.besp.pki.entity.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {
    Optional<CertificateTemplate> findByName(String name);
    List<CertificateTemplate> findByIssuerCertificate(CertificateRecord issuer);
}
