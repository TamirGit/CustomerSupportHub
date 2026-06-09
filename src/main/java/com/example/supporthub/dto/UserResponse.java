package com.example.supporthub.dto;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        Long agentId,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAgentId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
