package com.besp.pki.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "active_sessions",
        indexes = {
                @Index(name = "ix_active_session_jti", columnList = "jti"),
                @Index(name = "ix_active_session_user", columnList = "user_id")
        })
public class ActiveSession {
    
    @Id
    @NotBlank
    @Column(name = "jti", nullable = false, unique = true, length = 255)
    private String jti; // JWT ID - unique identifier
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Column(name = "device_info", nullable = false, length = 512)
    private String deviceInfo;
    
    @NotBlank
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    @NotNull
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;
    
    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (lastActivity == null) {
            lastActivity = LocalDateTime.now();
        }
    }
    
    // Constructors
    public ActiveSession() {}
    
    public ActiveSession(String jti, User user, String deviceInfo, String ipAddress) {
        this.jti = jti;
        this.user = user;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.issuedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
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
    
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}

