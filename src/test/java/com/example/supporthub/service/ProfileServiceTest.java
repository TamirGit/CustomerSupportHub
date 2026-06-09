package com.example.supporthub.service;

import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import com.example.supporthub.dto.UpdateProfileRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.web.error.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void updateMyProfile_appliesNewUniqueEmail() {
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol()));
        when(userRepository.existsByEmail("new@x.io")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = profileService.updateMyProfile("carol",
                new UpdateProfileRequest("Carol New", "new@x.io", null));

        assertThat(response.fullName()).isEqualTo("Carol New");
        assertThat(response.email()).isEqualTo("new@x.io");
    }

    @Test
    void updateMyProfile_rejectsEmailAlreadyTaken() {
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol()));
        when(userRepository.existsByEmail("taken@x.io")).thenReturn(true);

        assertThatThrownBy(() -> profileService.updateMyProfile("carol",
                new UpdateProfileRequest(null, "taken@x.io", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_keepingOwnEmail_skipsUniquenessCheck() {
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Same email (case-insensitive) must not trip the uniqueness check against itself.
        UserResponse response = profileService.updateMyProfile("carol",
                new UpdateProfileRequest("Carol Renamed", "CAROL@x.io", null));

        assertThat(response.fullName()).isEqualTo("Carol Renamed");
        verify(userRepository, never()).existsByEmail(anyString());
    }
}
