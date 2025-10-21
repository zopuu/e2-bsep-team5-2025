package com.besp.pki.dto;

import java.time.Instant;

public class CertificateIssueResponse {
    
    private boolean success;
    private String message;
    private Long certificateId;
    private String serialNumber;
    private String subjectDn;
    private String issuerDn;
    private Instant notBefore;
    private Instant notAfter;
    private String fingerprintSha256;
    
    // Constructors
    public CertificateIssueResponse() {}
    
    public CertificateIssueResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public CertificateIssueResponse(boolean success, String message, Long certificateId, 
                                  String serialNumber, String subjectDn, String issuerDn,
                                  Instant notBefore, Instant notAfter, String fingerprintSha256) {
        this.success = success;
        this.message = message;
        this.certificateId = certificateId;
        this.serialNumber = serialNumber;
        this.subjectDn = subjectDn;
        this.issuerDn = issuerDn;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.fingerprintSha256 = fingerprintSha256;
    }
    
    // Static factory methods
    public static CertificateIssueResponse success(String message, Long certificateId, 
                                                 String serialNumber, String subjectDn, String issuerDn,
                                                 Instant notBefore, Instant notAfter, String fingerprintSha256) {
        return new CertificateIssueResponse(true, message, certificateId, serialNumber, 
                                          subjectDn, issuerDn, notBefore, notAfter, fingerprintSha256);
    }
    
    public static CertificateIssueResponse error(String message) {
        return new CertificateIssueResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getCertificateId() {
        return certificateId;
    }
    
    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getSubjectDn() {
        return subjectDn;
    }
    
    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }
    
    public String getIssuerDn() {
        return issuerDn;
    }
    
    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }
    
    public Instant getNotBefore() {
        return notBefore;
    }
    
    public void setNotBefore(Instant notBefore) {
        this.notBefore = notBefore;
    }
    
    public Instant getNotAfter() {
        return notAfter;
    }
    
    public void setNotAfter(Instant notAfter) {
        this.notAfter = notAfter;
    }
    
    public String getFingerprintSha256() {
        return fingerprintSha256;
    }
    
    public void setFingerprintSha256(String fingerprintSha256) {
        this.fingerprintSha256 = fingerprintSha256;
    }
}
