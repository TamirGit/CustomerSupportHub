package com.example.supporthub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Partial profile update. All fields are optional; only non-null fields are applied. Present
 * values are still validated.
 */
public record UpdateProfileRequest(
        @Size(max = 100) String fullName,

        @Email(message = "email must be a valid address") String email,

        @Size(min = 6, max = 100, message = "password must be 6-100 characters") String password) {
}
