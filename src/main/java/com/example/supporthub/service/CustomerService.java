package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateCustomerRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.web.error.DuplicateResourceException;
import com.example.supporthub.web.error.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer management. An AGENT operates only on the customers registered under it; an ADMIN may
 * operate across all agents.
 */
@Service
@Transactional
public class CustomerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new customer under an agent. For an AGENT user the owning agent is always the
     * user itself; for an ADMIN user the target agent must be supplied via {@code agentId}.
     */
    public UserResponse createCustomer(String username, CreateCustomerRequest request) {
        User user = userRepository.requireByUsername(username);
        User owningAgent = resolveOwningAgent(user, request.agentId());

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }

        User customer = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.email(),
                Role.CUSTOMER,
                owningAgent);
        return UserResponse.from(userRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listCustomers(String username) {
        User user = userRepository.requireByUsername(username);
        List<User> customers = user.getRole() == Role.ADMIN
                ? userRepository.findByRole(Role.CUSTOMER)
                : userRepository.findByAgent_IdAndRole(user.getId(), Role.CUSTOMER);
        return customers.stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getCustomer(String username, Long customerId) {
        User user = userRepository.requireByUsername(username);
        User customer = userRepository.findById(customerId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new NotFoundException("Customer " + customerId + " not found"));

        if (!canAccessCustomer(user, customer)) {
            throw new AccessDeniedException("You are not permitted to view this customer");
        }
        return UserResponse.from(customer);
    }

    private User resolveOwningAgent(User user, Long requestedAgentId) {
        if (user.getRole() == Role.AGENT) {
            return user;
        }
        // ADMIN must name the agent the customer belongs to.
        if (requestedAgentId == null) {
            throw new IllegalArgumentException("agentId is required when an admin creates a customer");
        }
        User agent = userRepository.findById(requestedAgentId)
                .orElseThrow(() -> new NotFoundException("Agent " + requestedAgentId + " not found"));
        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException("User " + requestedAgentId + " is not an agent");
        }
        return agent;
    }

    private boolean canAccessCustomer(User user, User customer) {
        return switch (user.getRole()) {
            case ADMIN -> true;
            case AGENT -> user.getId().equals(customer.getAgentId());
            case CUSTOMER -> user.getId().equals(customer.getId());
        };
    }
}
