package com.besp.pki.dto;

public class CsrUploadResponse {
    
    private boolean success;
    private String message;
    private CsrData csrData;
    
    public CsrUploadResponse() {}
    
    public CsrUploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public CsrUploadResponse(boolean success, String message, CsrData csrData) {
        this.success = success;
        this.message = message;
        this.csrData = csrData;
    }
    
    public static CsrUploadResponse success(String message, CsrData csrData) {
        return new CsrUploadResponse(true, message, csrData);
    }
    
    public static CsrUploadResponse error(String message) {
        return new CsrUploadResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public CsrData getCsrData() {
        return csrData;
    }
    
    public void setCsrData(CsrData csrData) {
        this.csrData = csrData;
    }
}
