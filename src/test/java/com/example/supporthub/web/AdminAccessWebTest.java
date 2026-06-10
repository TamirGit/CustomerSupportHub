package com.example.supporthub.web;

import com.example.supporthub.config.SecurityConfig;
import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.TicketStatus;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.AdminService;
import com.example.supporthub.service.CustomerService;
import com.example.supporthub.service.ProfileService;
import com.example.supporthub.service.TicketService;
import com.example.supporthub.web.error.RestAccessDeniedHandler;
import com.example.supporthub.web.error.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "ADMIN can do anything" guard proof: with an ADMIN principal, every endpoint admits the request
 * (services are mocked — this asserts authorization, not business logic). The one intentional
 * exception is the customer-only {@code POST /api/tickets}: an admin opens tickets via the
 * dedicated {@code POST /api/admin/customers/{id}/tickets} instead, since a ticket is owned by a
 * customer, not the admin.
 */
@WebMvcTest({TicketController.class, CustomerController.class, ProfileController.class, AdminController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminAccessWebTest {

    private static final String CUSTOMER_BODY = """
            {"username":"newc","password":"passw0rd","fullName":"New Customer","email":"newc@x.io"}
            """;
    private static final String USER_BODY = """
            {"username":"amy","password":"secret123","fullName":"Amy Agent","email":"amy@x.io","role":"AGENT"}
            """;
    private static final String TICKET_BODY = """
            {"subject":"Cannot log in","description":"500 on login"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockBean TicketService ticketService;
    @MockBean CustomerService customerService;
    @MockBean ProfileService profileService;
    @MockBean AdminService adminService;
    @MockBean JwtDecoder jwtDecoder;

    private static UserResponse user() {
        return new UserResponse(1L, "u", "Full Name", "u@x.io", Role.CUSTOMER, null, Instant.now(), Instant.now());
    }

    private static TicketResponse ticket() {
        return new TicketResponse(1L, "s", "d", TicketStatus.OPEN, 1L, "u", Instant.now(), Instant.now());
    }

    @BeforeEach
    void stubServices() {
        when(ticketService.listTickets(any(), any())).thenReturn(List.of());
        when(ticketService.getTicket(any(), any())).thenReturn(ticket());
        when(customerService.listCustomers(any())).thenReturn(List.of());
        when(customerService.getCustomer(any(), any())).thenReturn(user());
        when(customerService.createCustomer(any(), any())).thenReturn(user());
        when(profileService.getMyProfile(any())).thenReturn(user());
        when(profileService.updateMyProfile(any(), any())).thenReturn(user());
        when(adminService.createUser(any())).thenReturn(user());
        when(adminService.createTicketForCustomer(any(), any())).thenReturn(ticket());
    }

    @Test
    void adminListsTickets() throws Exception {
        mockMvc.perform(get("/api/tickets")).andExpect(status().isOk());
    }

    @Test
    void adminReadsTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/1")).andExpect(status().isOk());
    }

    @Test
    void adminListsCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isOk());
    }

    @Test
    void adminReadsCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/1")).andExpect(status().isOk());
    }

    @Test
    void adminCreatesCustomer() throws Exception {
        mockMvc.perform(post("/api/customers").contentType("application/json").content(CUSTOMER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void adminReadsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isOk());
    }

    @Test
    void adminUpdatesOwnProfile() throws Exception {
        mockMvc.perform(put("/api/users/me").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminProvisionsUser() throws Exception {
        mockMvc.perform(post("/api/admin/users").contentType("application/json").content(USER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCreatesTicketForCustomer() throws Exception {
        mockMvc.perform(post("/api/admin/customers/1/tickets").contentType("application/json").content(TICKET_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCannotUseCustomerOnlyTicketEndpoint() throws Exception {
        // By design: a ticket is owned by a customer; admin uses /api/admin/customers/{id}/tickets.
        mockMvc.perform(post("/api/tickets").contentType("application/json").content(TICKET_BODY))
                .andExpect(status().isForbidden());
    }
}
