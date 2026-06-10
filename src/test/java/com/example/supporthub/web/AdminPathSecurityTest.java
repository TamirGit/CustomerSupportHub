package com.example.supporthub.web;

import com.example.supporthub.config.SecurityConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the URL-level admin boundary in {@link SecurityConfig} ({@code /api/admin/** -> hasRole
 * ADMIN}) independently of any controller {@code @PreAuthorize}. We hit an UNMAPPED path under
 * {@code /api/admin}: a non-admin is rejected by the filter chain (403) before routing, whereas an
 * admin passes the gate and only then hits "no handler" (404). Without the URL rule a CUSTOMER would
 * reach the dispatcher and get 404 too — so a 403 here proves the gate is doing the work.
 */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminPathSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminService adminService;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void unmappedAdminPath_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void unmappedAdminPath_asCustomer_blockedByUrlGate_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/anything"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "amy", roles = "AGENT")
    void unmappedAdminPath_asAgent_blockedByUrlGate_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/anything"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unmappedAdminPath_asAdmin_passesGateThenNoHandler_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/anything"))
                .andExpect(status().isNotFound());
    }
}
