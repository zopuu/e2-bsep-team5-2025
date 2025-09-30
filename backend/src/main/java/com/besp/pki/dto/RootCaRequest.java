package com.besp.pki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class RootCaRequest {
    @NotBlank public String commonName;
    public String organization;
    public String country;
    public String organizationalUnit;
    public String locality;
    public String state;

    @Positive public int yearsValid = 5;
}
