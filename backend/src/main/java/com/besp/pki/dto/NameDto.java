package com.besp.pki.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NameDto (
    @Size(min=1,max=128) String commonName,
    String organization,
    String organizationalUnit,
    String locality,
    String state,
    @Pattern(regexp = "^[A-Z]{2}$") String country
) {}
