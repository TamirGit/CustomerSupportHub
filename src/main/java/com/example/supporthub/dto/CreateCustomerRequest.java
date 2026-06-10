package com.example.supporthub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to register a new customer. The owning agent is always the calling AGENT (for
 * {@code POST /api/customers}) or the agent named in the path (for the ADMIN endpoint
 * {@code POST /api/admin/agents/{agentId}/customers}), so no {@code agentId} appears in the body.
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
        String email) {
}
