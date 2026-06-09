package com.example.supporthub.service;

import com.example.supporthub.domain.User;
import com.example.supporthub.dto.UpdateProfileRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.web.error.DuplicateResourceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Self-service profile read/update. Works for any role: a user only ever touches their own record,
 * satisfying "AGENT update their own profile" and "CUSTOMER query and update his/her own profile".
 */
@Service
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String username) {
        return UserResponse.from(userRepository.requireByUsername(username));
    }

    public UserResponse updateMyProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.requireByUsername(username);
        if (StringUtils.hasText(request.fullName())) {
            user.setFullName(request.fullName());
        }
        if (StringUtils.hasText(request.email()) && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
            }
            user.setEmail(request.email());
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(userRepository.save(user));
    }
}
