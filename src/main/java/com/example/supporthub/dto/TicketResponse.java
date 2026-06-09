package com.example.supporthub.dto;

import com.example.supporthub.domain.Ticket;
import com.example.supporthub.domain.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String subject,
        String description,
        TicketStatus status,
        Long ownerId,
        String ownerUsername,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getOwner().getId(),
                ticket.getOwner().getUsername(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
