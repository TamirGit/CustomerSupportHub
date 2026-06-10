package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
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
     * Registers a new customer under the calling AGENT (the owning agent is always the caller).
     * Authorization is enforced at the controller ({@code @PreAuthorize("hasRole('AGENT')")}), so the
     * caller is trusted to be an AGENT here — same trust model as {@code TicketService.createTicket}.
     */
    public UserResponse createCustomer(String agentUsername, CreateUserRequest request) {
        User owningAgent = userRepository.requireByUsername(agentUsername);
        return register(owningAgent, request);
    }

    /**
     * Registers a new customer under a named agent. Used by the ADMIN API: the admin owns no
     * customers, so it supplies the agent the customer belongs to (path id). Authorization is
     * enforced at the controller ({@code @PreAuthorize("hasRole('ADMIN')")}).
     */
    public UserResponse createCustomerUnder(Long agentId, CreateUserRequest request) {
        User owningAgent = userRepository.findByIdAndRole(agentId, Role.AGENT)
                .orElseThrow(() -> new NotFoundException("Agent " + agentId + " not found"));
        return register(owningAgent, request);
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
        User customer = userRepository.findByIdAndRole(customerId, Role.CUSTOMER)
                .orElseThrow(() -> new NotFoundException("Customer " + customerId + " not found"));

        if (user.cannotAccessResourceOwnedBy(customer)) {
            throw new AccessDeniedException("You are not permitted to view this customer");
        }

        return UserResponse.from(customer);
    }

    private UserResponse register(User owningAgent, CreateUserRequest request) {
        User customer = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.email(),
                Role.CUSTOMER,
                owningAgent);

        return UserResponse.from(userRepository.save(customer));
    }
}
