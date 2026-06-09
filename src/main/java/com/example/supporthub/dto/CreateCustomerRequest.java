package com.example.supporthub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to register a new customer. {@code agentId} is optional and only honoured for ADMIN
 * callers; an AGENT always registers customers under itself.
 */
public record CreateCustomerRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3-50 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be 6-100 characters")
        String password,

        @NotBlank(message = "fullName is required")
        @Size(max = 100)
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        Long agentId) {
}
