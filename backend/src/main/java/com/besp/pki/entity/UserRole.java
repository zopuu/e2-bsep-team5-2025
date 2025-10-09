package com.besp.pki.entity;

public enum UserRole {
    ADMIN("Administrator"),
    CA_USER("CA User"),
    REGULAR_USER("Regular User");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}







