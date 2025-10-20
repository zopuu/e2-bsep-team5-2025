package com.besp.pki.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class CreateCaUserRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank @Size(min = 1, max = 64)
    private String firstName;

    @NotBlank @Size(min = 1, max = 64)
    private String lastName;

    @NotBlank @Size(min = 1, max = 128)
    private String organization;
}
