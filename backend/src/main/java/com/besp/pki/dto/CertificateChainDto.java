package com.besp.pki.dto;

import java.util.List;

public record CertificateChainDto(
        long id,
        String subject,
        String issuer,
        List<String> chain // leaf → root subjects
) {}
