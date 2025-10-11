package com.besp.pki.repository;

import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface CertificateRecordRepository extends JpaRepository<CertificateRecord, Long>,
                                                     JpaSpecificationExecutor<CertificateRecord> {
    Optional<CertificateRecord> findBySerialNumber(String serialNumber);
    Optional<CertificateRecord> findByFingerprintSha256(String fingerprint);
    Optional<CertificateRecord> findByKeystoreAlias(String alias);
    boolean existsBySerialNumber(String serialNumber);
    long countByStatus(CertificateStatus status);
}
