package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.Ticket;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateTicketRequest;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.repository.TicketRepository;
import com.example.supporthub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    TicketRepository ticketRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    TicketService ticketService;

    private User userWithId(long id, String username, Role role, User agent) {
        User user = new User(username, "hash", username, username + "@x.io", role, agent);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void createTicket_setsOwnerToCallingCustomer() {
        User customer = userWithId(5L, "cust", Role.CUSTOMER, null);
        when(userRepository.findByUsername("cust")).thenReturn(Optional.of(customer));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.createTicket("cust",
                new CreateTicketRequest("Cannot log in", "I get a 500 error"));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner().getId()).isEqualTo(5L);
        assertThat(response.subject()).isEqualTo("Cannot log in");
        assertThat(response.ownerUsername()).isEqualTo("cust");
    }

    @Test
    void createTicket_deniedForNonCustomer() {
        User agent = userWithId(10L, "agent", Role.AGENT, null);
        when(userRepository.findByUsername("agent")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> ticketService.createTicket("agent",
                new CreateTicketRequest("subject", "desc")))
                .isInstanceOf(AccessDeniedException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void listTickets_forAgent_queriesOnlyTheirCustomersTickets() {
        User agent = userWithId(10L, "agent", Role.AGENT, null);
        User customer = userWithId(5L, "cust", Role.CUSTOMER, agent);
        Ticket ticket = new Ticket("s", "d", customer);
        when(userRepository.findByUsername("agent")).thenReturn(Optional.of(agent));
        when(ticketRepository.findByOwner_Agent_Id(10L)).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.listTickets("agent", null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByOwner_Agent_Id(10L);
        verify(ticketRepository, never()).findAll(); // agent must not see all tickets
    }
}
