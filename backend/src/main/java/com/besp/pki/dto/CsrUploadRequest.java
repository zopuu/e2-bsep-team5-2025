package com.besp.pki.dto;

import jakarta.validation.constraints.NotBlank;

public class CsrUploadRequest {
    
    @NotBlank(message = "CSR content is required")
    private String csrContent;
    
    public CsrUploadRequest() {}
    
    public CsrUploadRequest(String csrContent) {
        this.csrContent = csrContent;
    }
    
    public String getCsrContent() {
        return csrContent;
    }
    
    public void setCsrContent(String csrContent) {
        this.csrContent = csrContent;
    }
}
