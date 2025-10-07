package com.besp.pki.dto;

import java.time.Instant;
import java.util.List;

public record CertificateResponse(
        String serialNumberHex,
        String subjectDn,
        String issuerDn,
        Instant notBefore,
        Instant notAfter,
        boolean isCa,
        Integer pathLenConstraint,
        List<String> chainSubjectDns
) {}