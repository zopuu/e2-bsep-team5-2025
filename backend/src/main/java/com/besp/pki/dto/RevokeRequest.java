package com.besp.pki.dto;

import com.besp.pki.entity.CertificateEnums.RevocationReason;
import jakarta.validation.constraints.NotNull;

public record RevokeRequest(
        @NotNull RevocationReason reason
) {}
