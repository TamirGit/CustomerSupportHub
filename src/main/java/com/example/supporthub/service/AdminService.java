package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateAgentRequest;
import com.example.supporthub.dto.CreateTicketRequest;
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

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder, TicketService ticketService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ticketService = ticketService;
    }

    /**
     * Create an AGENT — the only path to add agents. A duplicate username/email trips the DB unique
     * constraints, surfacing as a 409 via the global exception handler.
     */
    public UserResponse createAgent(CreateAgentRequest request) {
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.email(),
                Role.AGENT,
                null);
        return UserResponse.from(userRepository.save(user));
    }

    /** Open a ticket on behalf of the given customer. */
    public TicketResponse createTicketForCustomer(Long customerId, CreateTicketRequest request) {
        return ticketService.createTicketFor(customerId, request);
    }
}
