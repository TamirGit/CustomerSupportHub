package com.example.supporthub.repository;

import com.example.supporthub.domain.Ticket;
import com.example.supporthub.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Tickets owned by a single customer. */
    List<Ticket> findByOwnerId(Long ownerId);

    /** Tickets created by any customer registered under the given agent. */
    List<Ticket> findByOwner_AgentId(Long agentId);

    List<Ticket> findByOwner_AgentIdAndStatus(Long agentId, TicketStatus status);
}
