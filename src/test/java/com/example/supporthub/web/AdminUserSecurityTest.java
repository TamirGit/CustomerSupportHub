package com.example.supporthub.web;

import com.example.supporthub.config.SecurityConfig;
import com.example.supporthub.domain.Role;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.AdminUserService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-aware tests for the ADMIN-only user-provisioning endpoint: unauthenticated → 401,
 * non-admin roles → 403, ADMIN → 201.
 */
@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminUserSecurityTest {

    private static final String AGENT_BODY = """
            {"username":"amy","password":"secret123","fullName":"Amy Agent","email":"amy@x.io","role":"AGENT"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminUserService adminUserService;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void createUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/users").contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void createUser_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/users").with(csrf())
                        .contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "amy", roles = "AGENT")
    void createUser_asAgent_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/users").with(csrf())
                        .contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_asAdmin_isAuthorized() throws Exception {
        when(adminUserService.createUser(any()))
                .thenReturn(new UserResponse(2L, "amy", "Amy Agent", "amy@x.io", Role.AGENT, null,
                        Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/admin/users").with(csrf())
                        .contentType("application/json").content(AGENT_BODY))
                .andExpect(status().isCreated());
    }
}
