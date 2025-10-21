package com.besp.pki.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_tokens",
        indexes = {
                @Index(name = "ix_revoked_jti", columnList = "jti"),
                @Index(name = "ix_revoked_user", columnList = "user_id")
        })
public class RevokedToken {
    
    @Id
    @NotBlank
    @Column(name = "jti", nullable = false, unique = true, length = 255)
    private String jti; // JWT ID - unique identifier
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;
    
    @Column(name = "device_info", length = 512)
    private String deviceInfo;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "reason", length = 255)
    private String reason; // Optional reason for revocation
    
    @PrePersist
    protected void onCreate() {
        if (revokedAt == null) {
            revokedAt = LocalDateTime.now();
        }
    }
    
    // Constructors
    public RevokedToken() {}
    
    public RevokedToken(String jti, User user, String deviceInfo, String ipAddress) {
        this.jti = jti;
        this.user = user;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.revokedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getJti() {
        return jti;
    }
    
    public void setJti(String jti) {
        this.jti = jti;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public String getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}

