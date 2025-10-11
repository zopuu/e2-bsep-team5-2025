package com.besp.pki.dto;

import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.CertificateType;
import com.besp.pki.entity.CertificateEnums.RevocationReason;

import java.time.Instant;

public record CertificateListItem(
        long id,
        String serialNumber,
        String subjectDn,
        String issuerDn,
        Long issuerId,
        Long ownerUserId,
        Instant notBefore,
        Instant notAfter,
        boolean ca,
        Integer pathLenConstraint,
        CertificateStatus status,
        CertificateType type,
        Integer keySize,
        String fingerprintSha256,
        RevocationReason revocationReason,
        Instant revocationDate
) {}
