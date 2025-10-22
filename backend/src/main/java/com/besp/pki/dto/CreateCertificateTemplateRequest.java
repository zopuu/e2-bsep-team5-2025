package com.besp.pki.dto;

import com.besp.pki.entity.ExtendedKeyUsage;
import com.besp.pki.entity.KeyUsage;
import jakarta.validation.constraints.*;

import java.util.Set;

public class CreateCertificateTemplateRequest {

    @NotBlank @Size(max = 120)
    private String name;

    @NotNull
    private Long issuerCertificateId;

    @NotBlank @Size(max = 512)
    private String cnRegex;

    @NotBlank @Size(max = 512)
    private String sanRegex;

    @NotNull @Min(1) @Max(3650)
    private Integer ttlDays;

    @NotNull
    private Set<KeyUsage> keyUsages;

    @NotNull
    private Set<ExtendedKeyUsage> extendedKeyUsages;

    // getters/setters
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
}
