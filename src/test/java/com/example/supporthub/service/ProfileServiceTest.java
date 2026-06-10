package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.UpdateProfileRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    ProfileService profileService;

    private User carol() {
        return new User("carol", "hash", "Carol", "carol@x.io", Role.CUSTOMER, null);
    }

    @Test
    void updateMyProfile_appliesProvidedFields() {
        when(userRepository.requireByUsername("carol")).thenReturn(carol());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = profileService.updateMyProfile("carol",
                new UpdateProfileRequest("Carol New", "new@x.io", null));

        assertThat(response.fullName()).isEqualTo("Carol New");
        assertThat(response.email()).isEqualTo("new@x.io");
    }

    @Test
    void updateMyProfile_onlyTouchesFieldsThatWereSent() {
        when(userRepository.requireByUsername("carol")).thenReturn(carol());
        when(passwordEncoder.encode("newpass123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only the password is provided — name/email must be left untouched (partial update).
        UserResponse response = profileService.updateMyProfile("carol",
                new UpdateProfileRequest(null, null, "newpass123"));

        assertThat(response.fullName()).isEqualTo("Carol");      // unchanged
        assertThat(response.email()).isEqualTo("carol@x.io");    // unchanged
    }
}
