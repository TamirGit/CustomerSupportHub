package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.web.error.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AdminUserService adminUserService;

    @Test
    void createUser_createsAgentWithNoOwningAgent() {
        when(userRepository.existsByUsername("amy")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = adminUserService.createUser(
                new CreateUserRequest("amy", "secret123", "Amy Agent", "amy@x.io", Role.AGENT, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.AGENT);
        assertThat(captor.getValue().getAgentId()).isNull();
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("ENCODED");
        assertThat(response.username()).isEqualTo("amy");
    }

    @Test
    void createUser_customerWithoutAgentId_isRejected() {
        when(userRepository.existsByUsername("carol")).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.createUser(
                new CreateUserRequest("carol", "secret123", "Carol", "carol@x.io", Role.CUSTOMER, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_customerIsAttachedToNamedAgent() {
        User agent = new User("amy", "h", "Amy", "amy@x.io", Role.AGENT, null);
        ReflectionTestUtils.setField(agent, "id", 10L);
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(passwordEncoder.encode(any())).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminUserService.createUser(
                new CreateUserRequest("carol", "secret123", "Carol", "carol@x.io", Role.CUSTOMER, 10L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(captor.getValue().getAgentId()).isEqualTo(10L);
    }

    @Test
    void createUser_duplicateUsername_isRejected() {
        when(userRepository.existsByUsername("amy")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(
                new CreateUserRequest("amy", "secret123", "Amy", "amy@x.io", Role.AGENT, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_agentIdForNonCustomer_isRejected() {
        when(userRepository.existsByUsername("amy")).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.createUser(
                new CreateUserRequest("amy", "secret123", "Amy", "amy@x.io", Role.AGENT, 10L)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }
}
