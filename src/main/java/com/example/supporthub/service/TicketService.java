package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.Ticket;
import com.example.supporthub.domain.TicketStatus;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateTicketRequest;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.repository.TicketRepository;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.web.error.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ticket management. CUSTOMERs create and read their own tickets; AGENTs read tickets created by
 * their own customers; ADMIN sees everything.
 */
@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public TicketResponse createTicket(String username, CreateTicketRequest request) {
        User user = userRepository.requireByUsername(username);
        if (user.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers can open tickets");
        }
        Ticket ticket = new Ticket(request.subject(), request.description(), user);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(String username, TicketStatus statusFilter) {
        User user = userRepository.requireByUsername(username);
        List<Ticket> tickets = switch (user.getRole()) {
            case CUSTOMER -> ticketRepository.findByOwnerId(user.getId());
            case AGENT -> statusFilter == null
                    ? ticketRepository.findByOwner_Agent_Id(user.getId())
                    : ticketRepository.findByOwner_Agent_IdAndStatus(user.getId(), statusFilter);
            case ADMIN -> statusFilter == null
                    ? ticketRepository.findAll()
                    : ticketRepository.findByStatus(statusFilter);
        };
        return tickets.stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(String username, Long ticketId) {
        User user = userRepository.requireByUsername(username);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
        if (!canAccessTicket(user, ticket)) {
            throw new AccessDeniedException("You are not permitted to view this ticket");
        }
        return TicketResponse.from(ticket);
    }

    private boolean canAccessTicket(User user, Ticket ticket) {
        User owner = ticket.getOwner();
        return switch (user.getRole()) {
            case ADMIN -> true;
            case CUSTOMER -> user.getId().equals(owner.getId());
            case AGENT -> user.getId().equals(owner.getAgentId());
        };
    }
}
