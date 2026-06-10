package com.example.supporthub.web;

import com.example.supporthub.config.SecurityConfig;
import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.TicketStatus;
import com.example.supporthub.dto.TicketResponse;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.AdminService;
import com.example.supporthub.web.error.RestAccessDeniedHandler;
import com.example.supporthub.web.error.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-aware tests for the ADMIN-only API surface: unauthenticated → 401, non-admin → 403,
 * ADMIN → 201, for both creating an agent and opening a ticket on behalf of a customer.
 */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminControllerSecurityTest {

    private static final String AGENT_BODY = """
            {"username":"amy","password":"secret123","fullName":"Amy Agent","email":"amy@x.io"}
            """;
    private static final String TICKET_BODY = """
            {"subject":"Cannot log in","description":"500 on login"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminService adminService;

    @MockBean
    JwtDecoder jwtDecoder;

    // --- POST /api/admin/agents ---

    @Test
    void createAgent_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/agents").contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void createAgent_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/agents").with(csrf()).contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "amy", roles = "AGENT")
    void createAgent_asAgent_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/agents").with(csrf()).contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createAgent_asAdmin_isAuthorized() throws Exception {
        when(adminService.createAgent(any()))
                .thenReturn(new UserResponse(2L, "amy", "Amy Agent", "amy@x.io", Role.AGENT, null,
                        Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/admin/agents").with(csrf()).contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isCreated());
    }

    // --- POST /api/admin/customers/{customerId}/tickets ---

    @Test
    void createTicketForCustomer_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/customers/5/tickets").contentType("application/json").content(TICKET_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void createTicketForCustomer_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/customers/5/tickets").with(csrf())
                        .contentType("application/json").content(TICKET_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createTicketForCustomer_asAdmin_isAuthorized() throws Exception {
        when(adminService.createTicketForCustomer(eq(5L), any()))
                .thenReturn(new TicketResponse(1L, "Cannot log in", "500 on login", TicketStatus.OPEN,
                        5L, "carol", Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/admin/customers/5/tickets").with(csrf())
                        .contentType("application/json").content(TICKET_BODY))
                .andExpect(status().isCreated());
    }
}
