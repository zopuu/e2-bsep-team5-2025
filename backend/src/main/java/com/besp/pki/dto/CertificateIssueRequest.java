package com.besp.pki.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class CertificateIssueRequest {
    
    @NotNull(message = "CSR data is required")
    private CsrData csrData;
    
    @NotNull(message = "CA certificate ID is required")
    private Long caCertificateId;
    
    @NotNull(message = "Validity days is required")
    @Min(value = 1, message = "Validity must be at least 1 day")
    @Max(value = 3650, message = "Validity cannot exceed 10 years")
    private Integer validityDays;
    
    // Constructors
    public CertificateIssueRequest() {}
    
    public CertificateIssueRequest(CsrData csrData, Long caCertificateId, Integer validityDays) {
        this.csrData = csrData;
        this.caCertificateId = caCertificateId;
        this.validityDays = validityDays;
    }
    
    // Getters and Setters
    public CsrData getCsrData() {
        return csrData;
    }
    
    public void setCsrData(CsrData csrData) {
        this.csrData = csrData;
    }
    
    public Long getCaCertificateId() {
        return caCertificateId;
    }
    
    public void setCaCertificateId(Long caCertificateId) {
        this.caCertificateId = caCertificateId;
    }
    
    public Integer getValidityDays() {
        return validityDays;
    }
    
    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }
}
