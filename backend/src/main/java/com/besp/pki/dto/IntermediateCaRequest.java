package com.besp.pki.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record IntermediateCaRequest(
        @NotNull Long issuerRecordId,
        @NotNull NameDto subject,
        @Positive @Min(1) int yearsValid,
        Integer pathLenConstraint,  // null = no constraint
        List<String> crlDistribuitionPoints,
        List<String> authorityInfoAccessOcsp,
        String crlDistributionPoint,
        String ocspUrl,
        Long ownerUserId,
        String createdBy
) {}