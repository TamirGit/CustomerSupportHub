package com.example.supporthub.repository;

import com.example.supporthub.domain.Ticket;
import com.example.supporthub.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Tickets owned by a single customer. */
    List<Ticket> findByOwnerId(Long ownerId);

    /** Tickets created by any customer registered under the given agent (owner -> agent -> id). */
    List<Ticket> findByOwner_Agent_Id(Long agentId);

    List<Ticket> findByOwner_Agent_IdAndStatus(Long agentId, TicketStatus status);

    /** All tickets with a given status (e.g. an admin filtering across all customers). */
    List<Ticket> findByStatus(TicketStatus status);
}
