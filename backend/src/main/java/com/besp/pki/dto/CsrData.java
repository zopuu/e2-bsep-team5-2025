package com.besp.pki.dto;

import java.util.List;

public class CsrData {
    
    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String locality;
    private String state;
    private String country;
    private String emailAddress;
    private String publicKeyAlgorithm;
    private int publicKeySize;
    private String publicKeyPem;
    private List<String> subjectAlternativeNames;
    private String keyUsage;
    private String extendedKeyUsage;
    
    public CsrData() {}
    
    public CsrData(String commonName, String organization, String organizationalUnit,
                   String locality, String state, String country, String emailAddress,
                   String publicKeyAlgorithm, int publicKeySize, String publicKeyPem,
                   List<String> subjectAlternativeNames, String keyUsage, String extendedKeyUsage) {
        this.commonName = commonName;
        this.organization = organization;
        this.organizationalUnit = organizationalUnit;
        this.locality = locality;
        this.state = state;
        this.country = country;
        this.emailAddress = emailAddress;
        this.publicKeyAlgorithm = publicKeyAlgorithm;
        this.publicKeySize = publicKeySize;
        this.publicKeyPem = publicKeyPem;
        this.subjectAlternativeNames = subjectAlternativeNames;
        this.keyUsage = keyUsage;
        this.extendedKeyUsage = extendedKeyUsage;
    }
    
    // Getters and Setters
    public String getCommonName() {
        return commonName;
    }
    
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
    
    public String getOrganization() {
        return organization;
    }
    
    public void setOrganization(String organization) {
        this.organization = organization;
    }
    
    public String getOrganizationalUnit() {
        return organizationalUnit;
    }
    
    public void setOrganizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }
    
    public String getLocality() {
        return locality;
    }
    
    public void setLocality(String locality) {
        this.locality = locality;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getEmailAddress() {
        return emailAddress;
    }
    
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    public String getPublicKeyAlgorithm() {
        return publicKeyAlgorithm;
    }
    
    public void setPublicKeyAlgorithm(String publicKeyAlgorithm) {
        this.publicKeyAlgorithm = publicKeyAlgorithm;
    }
    
    public int getPublicKeySize() {
        return publicKeySize;
    }
    
    public void setPublicKeySize(int publicKeySize) {
        this.publicKeySize = publicKeySize;
    }
    
    public String getPublicKeyPem() {
        return publicKeyPem;
    }
    
    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }
    
    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames;
    }
    
    public void setSubjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
    }
    
    public String getKeyUsage() {
        return keyUsage;
    }
    
    public void setKeyUsage(String keyUsage) {
        this.keyUsage = keyUsage;
    }
    
    public String getExtendedKeyUsage() {
        return extendedKeyUsage;
    }
    
    public void setExtendedKeyUsage(String extendedKeyUsage) {
        this.extendedKeyUsage = extendedKeyUsage;
    }
}
