package com.besp.pki.mapper;

import com.besp.pki.dto.CertificateListItem;
import com.besp.pki.entity.CertificateRecord;

public final class CertificateMappers {
    private CertificateMappers() {}

    public static CertificateListItem toListItem(CertificateRecord r) {
        return new CertificateListItem(
                r.getId(),
                r.getSerialNumber(),
                r.getSubjectDn(),
                r.getIssuerDn(),
                r.getIssuer() != null ? r.getIssuer().getId() : null,
                r.getOwner()  != null ? r.getOwner().getId()  : null,
                r.getNotBefore(),
                r.getNotAfter(),
                r.isCa(),
                r.getPathLenConstraint(),
                r.getStatus(),
                r.getType(),
                r.getKeySize(),
                r.getFingerprintSha256()
        );
    }
}
