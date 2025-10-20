package com.besp.pki.repository;

import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import com.besp.pki.entity.CertificateEnums.CertificateStatus.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CertificateRecordRepository extends
        JpaRepository<CertificateRecord, Long>,
        JpaSpecificationExecutor<CertificateRecord> {

    Optional<CertificateRecord> findBySerialNumber(String serialNumber);
    Optional<CertificateRecord> findByFingerprintSha256(String fingerprint);
    Optional<CertificateRecord> findByKeystoreAlias(String alias);
    boolean existsBySerialNumber(String serialNumber);
    long countByStatus(CertificateStatus status);
    // Active CA issuers owned by a given user (owner_user_id)
    @Query("""
        select c from CertificateRecord c
        where c.ca = true
          and c.status = :status
          and c.notBefore <= :now
          and c.notAfter  >= :now
          and c.owner.id = :ownerUserId
        """)
    List<CertificateRecord> findActiveCaIssuersByOwnerUserId(@Param("ownerUserId") Long ownerUserId,
                                                             @Param("status") CertificateStatus status,
                                                             @Param("now") Instant now);

    @Query("""
        select c from CertificateRecord c
        where c.ca = true
          and c.status = :status
          and c.notBefore <= :now
          and c.notAfter  >= :now
          and c.owner.organization = :org
        """)
    List<CertificateRecord> findActiveCaIssuersByOwnerOrganization(@Param("org") String org,
                                                                   @Param("status") CertificateStatus status,
                                                                   @Param("now") Instant now);
}
