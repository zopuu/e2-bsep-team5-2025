package com.besp.pki.dto;

import java.time.LocalDateTime;

public class ActiveSessionDto {
    private String jti;
    private String device;
    private String ip;
    private LocalDateTime issuedAt;
    private LocalDateTime lastActivity;
    private boolean current; // Is this the current session?
    
    public ActiveSessionDto() {}
    
    public ActiveSessionDto(String jti, String device, String ip, LocalDateTime issuedAt, LocalDateTime lastActivity, boolean current) {
        this.jti = jti;
        this.device = device;
        this.ip = ip;
        this.issuedAt = issuedAt;
        this.lastActivity = lastActivity;
        this.current = current;
    }
    
    // Getters and Setters
    public String getJti() {
        return jti;
    }
    
    public void setJti(String jti) {
        this.jti = jti;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
        this.device = device;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
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
    
    public boolean isCurrent() {
        return current;
    }
    
    public void setCurrent(boolean current) {
        this.current = current;
    }
}

