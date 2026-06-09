package com.example.supporthub.web;

import com.example.supporthub.config.SecurityConfig;
import com.example.supporthub.service.TicketService;
import com.example.supporthub.web.error.GlobalExceptionHandler;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the global handler returns the correct status (not 500) for standard MVC errors.
 * Regression guard for the catch-all previously masking 405/415/400 as 500.
 */
@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class})
class ErrorHandlingWebTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TicketService ticketService;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void unsupportedMethod_returns405() throws Exception {
        // PUT is not mapped on /api/tickets -> HttpRequestMethodNotSupportedException
        mockMvc.perform(put("/api/tickets"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void unsupportedMediaType_returns415() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType("text/plain").content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @WithMockUser(username = "carol", roles = "CUSTOMER")
    void invalidBody_returns400WithFieldErrors() throws Exception {
        // Missing required 'subject' -> MethodArgumentNotValidException
        mockMvc.perform(post("/api/tickets").contentType("application/json")
                        .content("{\"description\":\"no subject\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
