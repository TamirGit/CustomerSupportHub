package com.example.supporthub.web;

import com.example.supporthub.dto.UpdateProfileRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service profile endpoints. Any authenticated user manages only their own profile; the
 * acting user is taken from the validated token, never from the request body.
 */
@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserResponse getMyProfile(Authentication authentication) {
        return profileService.getMyProfile(authentication.getName());
    }

    @PutMapping
    public UserResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
                                        Authentication authentication) {
        return profileService.updateMyProfile(authentication.getName(), request);
    }
}
