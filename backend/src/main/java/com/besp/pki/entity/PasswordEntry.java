package com.besp.pki.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "password_entries",
        indexes = {
                @Index(name = "ix_password_user", columnList = "owner_id"),
                @Index(name = "ix_password_site", columnList = "site_name")
        })
public class PasswordEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "site_name", nullable = false, length = 255)
    private String siteName;
    
    @NotBlank
    @Column(name = "username", nullable = false, length = 255)
    private String username;
    
    @NotBlank
    @Column(name = "encrypted_password", nullable = false, length = 2048)
    private String encryptedPassword;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    private CertificateRecord certificate; // Certificate used for encryption
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Constructors
    public PasswordEntry() {}
    
    public PasswordEntry(String siteName, String username, String encryptedPassword, User owner, CertificateRecord certificate) {
        this.siteName = siteName;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.owner = owner;
        this.certificate = certificate;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSiteName() {
        return siteName;
    }
    
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEncryptedPassword() {
        return encryptedPassword;
    }
    
    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public User getOwner() {
        return owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public CertificateRecord getCertificate() {
        return certificate;
    }
    
    public void setCertificate(CertificateRecord certificate) {
        this.certificate = certificate;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
