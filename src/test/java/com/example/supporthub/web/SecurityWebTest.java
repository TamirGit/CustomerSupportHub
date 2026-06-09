package com.example.supporthub.web;

import com.example.supporthub.config.SecurityConfig;
import com.example.supporthub.domain.Role;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.CustomerService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-aware tests covering authentication (401) and role-based authorization (403/allowed)
 * on the customer-creation endpoint, loaded as a web slice with no database.
 */
@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityWebTest {

    private static final String VALID_BODY = """
            {"username":"bob","password":"secret123","fullName":"Bob Jones","email":"bob@x.io"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CustomerService customerService;

    // Required by the OAuth2 resource-server filter chain; never exercised in these tests.
    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void createCustomer_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/customers").contentType("application/json").content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "cust", roles = "CUSTOMER")
    void createCustomer_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/customers").with(csrf())
                        .contentType("application/json").content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void createCustomer_asAgent_isAuthorized() throws Exception {
        when(customerService.createCustomer(eq("agent1"), any()))
                .thenReturn(new UserResponse(1L, "bob", "Bob Jones", "bob@x.io", Role.CUSTOMER, 10L));

        mockMvc.perform(post("/api/customers").with(csrf())
                        .contentType("application/json").content(VALID_BODY))
                .andExpect(status().isCreated());
    }
}
