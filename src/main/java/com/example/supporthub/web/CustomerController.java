package com.example.supporthub.web;

import com.example.supporthub.dto.CreateCustomerRequest;
import com.example.supporthub.dto.UserResponse;
import com.example.supporthub.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer management. Coarse role gating is applied here with {@code @PreAuthorize}; per-record
 * ownership is enforced in {@link CustomerService}.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public UserResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request,
                                       Authentication authentication) {
        return customerService.createCustomer(authentication.getName(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<UserResponse> listCustomers(Authentication authentication) {
        return customerService.listCustomers(authentication.getName());
    }

    @GetMapping("/{id}")
    public UserResponse getCustomer(@PathVariable Long id, Authentication authentication) {
        return customerService.getCustomer(authentication.getName(), id);
    }
}
