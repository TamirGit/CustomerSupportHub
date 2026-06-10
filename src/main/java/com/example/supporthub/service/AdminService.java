package com.example.supporthub.service;

import com.example.supporthub.dto.CreateTicketRequest;
import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.dto.UserResponse;
import org.springframework.stereotype.Service;

/**
 * Orchestration facade for the ADMIN-only operations exposed by {@code AdminController}. It reuses
 * the existing domain services rather than duplicating their logic, keeping all "god-mode" admin
 * actions behind one cohesive entry point.
 */
@Service
public class AdminService {

    private final UserProvisioningService userProvisioningService;
    private final TicketService ticketService;

    public AdminService(UserProvisioningService userProvisioningService, TicketService ticketService) {
        this.userProvisioningService = userProvisioningService;
        this.ticketService = ticketService;
    }

    /** Provision a user of any role (the only path to create AGENTs). */
    public UserResponse createUser(CreateUserRequest request) {
        return userProvisioningService.createUser(request);
    }

    /** Open a ticket on behalf of the given customer. */
    public TicketResponse createTicketForCustomer(Long customerId, CreateTicketRequest request) {
        return ticketService.createTicketFor(customerId, request);
    }
}
