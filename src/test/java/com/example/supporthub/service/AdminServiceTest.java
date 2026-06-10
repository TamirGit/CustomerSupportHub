package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateAgentRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    TicketService ticketService;

    @InjectMocks
    AdminService adminService;

    @Test
    void createAgent_createsAgentWithEncodedPasswordAndNoOwningAgent() {
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = adminService.createAgent(
                new CreateAgentRequest("amy", "secret123", "Amy Agent", "amy@x.io"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.AGENT);
        assertThat(captor.getValue().getAgentId()).isNull();
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("ENCODED");
        assertThat(response.username()).isEqualTo("amy");
    }
}
