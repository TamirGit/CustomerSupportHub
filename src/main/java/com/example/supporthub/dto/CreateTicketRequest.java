package com.example.supporthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "subject is required")
        @Size(max = 200)
        String subject,

        @NotBlank(message = "description is required")
        @Size(max = 4000)
        String description) {
}
