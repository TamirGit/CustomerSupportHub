package com.example.supporthub.web;

import com.example.supporthub.domain.TicketStatus;
import com.example.supporthub.dto.CreateTicketRequest;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ticket endpoints. Only CUSTOMERs may create; listing/reading is scoped by role inside
 * {@link TicketService}.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request,
                                       Authentication authentication) {
        return ticketService.createTicket(authentication.getName(), request);
    }

    @GetMapping
    public List<TicketResponse> listTickets(@RequestParam(required = false) TicketStatus status,
                                            Authentication authentication) {
        return ticketService.listTickets(authentication.getName(), status);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(@PathVariable Long id, Authentication authentication) {
        return ticketService.getTicket(authentication.getName(), id);
    }
}
