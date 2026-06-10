package com.example.supporthub.web;

import com.example.supporthub.dto.CreateTicketRequest;
import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single ADMIN-only API surface. Reads and operations that an admin already performs through
 * the regular role-aware endpoints (listing/reading tickets and customers, creating customers,
 * managing the admin's own profile) stay there; this controller adds the operations that need
 * admin-specific data shapes — creating AGENTs (the only path to add agents) and opening a ticket
 * on behalf of a customer.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/agents")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAgent(@Valid @RequestBody CreateUserRequest request) {
        return adminService.createAgent(request);
    }

    @PostMapping("/customers/{customerId}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicketForCustomer(@PathVariable Long customerId,
                                                  @Valid @RequestBody CreateTicketRequest request) {
        return adminService.createTicketForCustomer(customerId, request);
    }

    @PostMapping("/agents/{agentId}/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createCustomerForAgent(@PathVariable Long agentId,
                                               @Valid @RequestBody CreateUserRequest request) {
        return adminService.createCustomerForAgent(agentId, request);
    }
}
