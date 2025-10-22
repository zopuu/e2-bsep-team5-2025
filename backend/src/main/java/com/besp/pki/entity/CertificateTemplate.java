package com.besp.pki.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, unique = true)
    private String name;

    // CA issuer – koji CA sertifikat potpisuje sertifikate iz ovog šablona
    @ManyToOne(optional = false)
    @JoinColumn(name = "issuer_certificate_id", nullable = false)
    private CertificateRecord issuerCertificate;

    @NotBlank
    @Size(max = 512)
    @Column(name = "cn_regex", nullable = false, length = 512)
    private String cnRegex;

    @NotBlank
    @Size(max = 512)
    @Column(name = "san_regex", nullable = false, length = 512)
    private String sanRegex;

    @Min(1) @Max(3650)
    @Column(name = "ttl_days", nullable = false)
    private Integer ttlDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "template_key_usages", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "key_usage", nullable = false, length = 40)
    private Set<KeyUsage> keyUsages;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "template_extended_key_usages", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "extended_key_usage", nullable = false, length = 40)
    private Set<ExtendedKeyUsage> extendedKeyUsages;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CertificateRecord getIssuerCertificate() { return issuerCertificate; }
    public void setIssuerCertificate(CertificateRecord issuerCertificate) { this.issuerCertificate = issuerCertificate; }
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
}
