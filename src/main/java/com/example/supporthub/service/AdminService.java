package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateTicketRequest;
import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration facade for the ADMIN-only operations exposed by {@code AdminController}. It reuses
 * the existing domain services rather than duplicating their logic, keeping all "god-mode" admin
 * actions behind one cohesive entry point.
 */
@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketService ticketService;
    private final CustomerService customerService;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TicketService ticketService, CustomerService customerService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ticketService = ticketService;
        this.customerService = customerService;
    }

    /**
     * Create an AGENT — the only path to add agents. A duplicate username/email trips the DB unique
     * constraints, surfacing as a 409 via the global exception handler.
     */
    public UserResponse createAgent(CreateUserRequest request) {
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.email(),
                Role.AGENT,
                null);
        return UserResponse.from(userRepository.save(user));
    }

    public TicketResponse createTicketForCustomer(Long customerId, CreateTicketRequest request) {
        return ticketService.createTicketFor(customerId, request);
    }

    public UserResponse createCustomerForAgent(Long agentId, CreateUserRequest request) {
        return customerService.createCustomerUnder(agentId, request);
    }
}
