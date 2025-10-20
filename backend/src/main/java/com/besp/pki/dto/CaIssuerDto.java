package com.besp.pki.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
public class CaIssuerDto {
    private Long id;
    private String subjectDn;
    private String issuerDn;
    private String serialNumber;
    private boolean ca;
    private Instant notBefore;
    private Instant notAfter;

    public CaIssuerDto() {}
    public CaIssuerDto(Long id, String subjectDn, String issuerDn, String serialNumber,
                       boolean ca, Instant notBefore, Instant notAfter) {
        this.id = id; this.subjectDn = subjectDn; this.issuerDn = issuerDn;
        this.serialNumber = serialNumber; this.ca = ca; this.notBefore = notBefore; this.notAfter = notAfter;
    }

}
