package com.besp.pki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PasswordEntryRequest {
    
    @NotBlank(message = "Site name is required")
    private String siteName;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String notes;
    
    @NotNull(message = "Certificate ID is required")
    private Long certificateId;
    
    // Constructors
    public PasswordEntryRequest() {}
    
    public PasswordEntryRequest(String siteName, String username, String password, String notes, Long certificateId) {
        this.siteName = siteName;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.certificateId = certificateId;
    }
    
    // Getters and Setters
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Long getCertificateId() {
        return certificateId;
    }
    
    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
    }
}
