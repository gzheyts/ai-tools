package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePersonRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotNull String email
) {
    public CreatePersonRequest {
        if (firstName != null) {
            firstName = firstName.strip();
        }
        if (lastName != null) {
            lastName = lastName.strip();
        }
        if (email != null) {
            email = email.strip().toLowerCase();
        }
    }
}
