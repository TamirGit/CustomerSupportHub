package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.CreateCustomerRequest;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    CustomerService customerService;

    private User agent(long id, String username) {
        User agent = new User(username, "hash", "Agent " + id, username + "@x.io", Role.AGENT, null);
        ReflectionTestUtils.setField(agent, "id", id);
        return agent;
    }

    @Test
    void createCustomer_registersCustomerUnderCallingAgent() {
        User agent = agent(10L, "agent1");
        when(userRepository.findByUsername("agent1")).thenReturn(Optional.of(agent));
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = customerService.createCustomer("agent1",
                new CreateCustomerRequest("bob", "secret123", "Bob Jones", "bob@x.io", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getAgentId()).isEqualTo(10L);          // registered under the calling agent
        assertThat(saved.getPasswordHash()).isEqualTo("ENCODED"); // password stored hashed, never plaintext
        assertThat(response.username()).isEqualTo("bob");
    }

    @Test
    void createCustomer_rejectsDuplicateUsername() {
        when(userRepository.findByUsername("agent1")).thenReturn(Optional.of(agent(10L, "agent1")));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer("agent1",
                new CreateCustomerRequest("bob", "secret123", "Bob Jones", "bob@x.io", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void listCustomers_forAgent_returnsOnlyOwnCustomers() {
        User agent = agent(10L, "agent1");
        User c1 = new User("c1", "h", "C1", "c1@x.io", Role.CUSTOMER, agent);
        User c2 = new User("c2", "h", "C2", "c2@x.io", Role.CUSTOMER, agent);
        when(userRepository.findByUsername("agent1")).thenReturn(Optional.of(agent));
        when(userRepository.findByAgent_IdAndRole(10L, Role.CUSTOMER)).thenReturn(List.of(c1, c2));

        List<UserResponse> result = customerService.listCustomers("agent1");

        assertThat(result).hasSize(2).extracting(UserResponse::username).containsExactly("c1", "c2");
        verify(userRepository, never()).findAll(); // agents must not enumerate all customers
    }
}
