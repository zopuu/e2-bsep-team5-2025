package com.besp.pki.dto;

import com.besp.pki.entity.ExtendedKeyUsage;
import com.besp.pki.entity.KeyUsage;

import java.time.LocalDateTime;
import java.util.Set;

public class CertificateTemplateResponse {
    private Long id;
    private String name;
    private Long issuerCertificateId;
    private String cnRegex;
    private String sanRegex;
    private Integer ttlDays;
    private Set<KeyUsage> keyUsages;
    private Set<ExtendedKeyUsage> extendedKeyUsages;
    private LocalDateTime createdAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getIssuerCertificateId() { return issuerCertificateId; }
    public void setIssuerCertificateId(Long issuerCertificateId) { this.issuerCertificateId = issuerCertificateId; }
    public String getCnRegex() { return cnRegex; }
    public void setCnRegex(String cnRegex) { this.cnRegex = cnRegex; }
    public String getSanRegex() { return sanRegex; }
    public void setSanRegex(String sanRegex) { this.sanRegex = sanRegex; }
    public Integer getTtlDays() { return ttlDays; }
    public void setTtlDays(Integer ttlDays) { this.ttlDays = ttlDays; }
    public Set<KeyUsage> getKeyUsages() { return keyUsages; }
    public void setKeyUsages(Set<KeyUsage> keyUsages) { this.keyUsages = keyUsages; }
    public Set<ExtendedKeyUsage> getExtendedKeyUsages() { return extendedKeyUsages; }
    public void setExtendedKeyUsages(Set<ExtendedKeyUsage> extendedKeyUsages) { this.extendedKeyUsages = extendedKeyUsages; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
