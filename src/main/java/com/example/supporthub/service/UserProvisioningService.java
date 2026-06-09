package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.web.error.DuplicateResourceException;
import com.example.supporthub.web.error.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the <strong>ADMIN-only</strong> user-provisioning API ({@code POST /api/admin/users},
 * guarded by {@code AdminUserController}). Lets an administrator create users of any role — most
 * importantly AGENTs, which have no other creation path. A CUSTOMER created here must name its
 * owning agent.
 */
@Service
@Transactional
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProvisioningService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }
        User owningAgent = resolveOwningAgent(request);

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.email(),
                request.role(),
                owningAgent);
        return UserResponse.from(userRepository.save(user));
    }

    private User resolveOwningAgent(CreateUserRequest request) {
        if (request.role() == Role.CUSTOMER) {
            if (request.agentId() == null) {
                throw new IllegalArgumentException("agentId is required when creating a CUSTOMER");
            }
            User agent = userRepository.findById(request.agentId())
                    .orElseThrow(() -> new NotFoundException("Agent " + request.agentId() + " not found"));
            if (agent.getRole() != Role.AGENT) {
                throw new IllegalArgumentException("User " + request.agentId() + " is not an agent");
            }
            return agent;
        }
        // ADMIN / AGENT users are not owned by an agent.
        if (request.agentId() != null) {
            throw new IllegalArgumentException("agentId is only valid when creating a CUSTOMER");
        }
        return null;
    }
}
