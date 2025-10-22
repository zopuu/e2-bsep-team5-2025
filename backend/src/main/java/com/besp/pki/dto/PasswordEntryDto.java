package com.besp.pki.dto;

import java.time.Instant;

public class PasswordEntryDto {
    
    private Long id;
    private String siteName;
    private String username;
    private String encryptedPassword;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Long certificateId;
    private String certificateSubject;
    
    // Constructors
    public PasswordEntryDto() {}
    
    public PasswordEntryDto(Long id, String siteName, String username, String encryptedPassword, 
                           String notes, Instant createdAt, Instant updatedAt, 
                           Long certificateId, String certificateSubject) {
        this.id = id;
        this.siteName = siteName;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.certificateId = certificateId;
        this.certificateSubject = certificateSubject;
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
    
    public Long getCertificateId() {
        return certificateId;
    }
    
    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
    }
    
    public String getCertificateSubject() {
        return certificateSubject;
    }
    
    public void setCertificateSubject(String certificateSubject) {
        this.certificateSubject = certificateSubject;
    }
}
