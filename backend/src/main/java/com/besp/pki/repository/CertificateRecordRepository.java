package com.besp.pki.repository;

import com.besp.pki.entity.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface CertificateRecordRepository extends JpaRepository<CertificateRecord, Long> {
    Optional<CertificateRecord> findBySerialNumber(String serialNumber);
    Optional<CertificateRecord> findByFingerprintSha256(String fingerprint);
}
