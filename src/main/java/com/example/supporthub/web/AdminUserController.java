package com.example.supporthub.web;

import com.example.supporthub.dto.CreateUserRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.UserProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative user management. ADMIN-only — the only way to provision AGENTs in the system.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserProvisioningService userProvisioningService;

    public AdminUserController(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userProvisioningService.createUser(request);
    }
}
