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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        when(userRepository.requireByUsername("cust")).thenReturn(customer);
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
    void createTicketFor_setsOwnerToNamedCustomer() {
        User customer = userWithId(5L, "carol", Role.CUSTOMER, null);
        when(userRepository.findByIdAndRole(5L, Role.CUSTOMER)).thenReturn(Optional.of(customer));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.createTicketFor(5L,
                new CreateTicketRequest("Cannot log in", "I get a 500 error"));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner().getId()).isEqualTo(5L);   // owned by the named customer
        assertThat(response.ownerUsername()).isEqualTo("carol");
    }

    @Test
    void createTicketFor_unknownCustomer_throwsNotFound() {
        when(userRepository.findByIdAndRole(99L, Role.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicketFor(99L, new CreateTicketRequest("s", "d")))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createTicketFor_nonCustomerId_throwsNotFound() {
        // findByIdAndRole filters on role at the query, so an id that is not a CUSTOMER returns empty.
        when(userRepository.findByIdAndRole(10L, Role.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicketFor(10L, new CreateTicketRequest("s", "d")))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void listTickets_forAgent_queriesOnlyTheirCustomersTickets() {
        User agent = userWithId(10L, "agent", Role.AGENT, null);
        User customer = userWithId(5L, "cust", Role.CUSTOMER, agent);
        Ticket ticket = new Ticket("s", "d", customer);
        when(userRepository.requireByUsername("agent")).thenReturn(agent);
        when(ticketRepository.findByOwner_Agent_Id(10L)).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.listTickets("agent", null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByOwner_Agent_Id(10L);
        verify(ticketRepository, never()).findAll(); // agent must not see all tickets
    }

    @Test
    void listTickets_forAdminWithStatus_queriesByStatusNotFindAll() {
        User admin = userWithId(1L, "admin", Role.ADMIN, null);
        User customer = userWithId(5L, "cust", Role.CUSTOMER, null);
        Ticket ticket = new Ticket("s", "d", customer);
        when(userRepository.requireByUsername("admin")).thenReturn(admin);
        when(ticketRepository.findByStatus(TicketStatus.OPEN)).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.listTickets("admin", TicketStatus.OPEN);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByStatus(TicketStatus.OPEN);
        verify(ticketRepository, never()).findAll(); // must not load all tickets and filter in memory
    }
}
