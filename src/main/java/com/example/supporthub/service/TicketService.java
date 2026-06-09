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

    public TicketResponse createTicket(String actorUsername, CreateTicketRequest request) {
        User actor = loadActor(actorUsername);
        if (actor.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers can open tickets");
        }
        Ticket ticket = new Ticket(request.subject(), request.description(), actor);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(String actorUsername, TicketStatus statusFilter) {
        User actor = loadActor(actorUsername);
        List<Ticket> tickets = switch (actor.getRole()) {
            case CUSTOMER -> ticketRepository.findByOwnerId(actor.getId());
            case AGENT -> statusFilter == null
                    ? ticketRepository.findByOwner_AgentId(actor.getId())
                    : ticketRepository.findByOwner_AgentIdAndStatus(actor.getId(), statusFilter);
            case ADMIN -> statusFilter == null
                    ? ticketRepository.findAll()
                    : ticketRepository.findAll().stream().filter(t -> t.getStatus() == statusFilter).toList();
        };
        return tickets.stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(String actorUsername, Long ticketId) {
        User actor = loadActor(actorUsername);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
        if (!canAccessTicket(actor, ticket)) {
            throw new AccessDeniedException("You are not permitted to view this ticket");
        }
        return TicketResponse.from(ticket);
    }

    private boolean canAccessTicket(User actor, Ticket ticket) {
        User owner = ticket.getOwner();
        return switch (actor.getRole()) {
            case ADMIN -> true;
            case CUSTOMER -> actor.getId().equals(owner.getId());
            case AGENT -> actor.getId().equals(owner.getAgentId());
        };
    }

    private User loadActor(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '" + username + "' not found"));
    }
}
