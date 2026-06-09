package com.example.supporthub.dto;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;

public record UserResponse(Long id, String username, String fullName, String email, Role role, Long agentId) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAgentId());
    }
}
