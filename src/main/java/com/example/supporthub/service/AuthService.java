package com.example.supporthub.service;

import com.example.supporthub.domain.User;
import com.example.supporthub.dto.LoginRequest;
import com.example.supporthub.dto.TokenResponse;
import com.example.supporthub.repository.UserRepository;
import com.example.supporthub.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Authenticates username/password credentials and issues a signed JWT. This is the project's
 * answer to "authenticate via username/password" — the OAuth2 resource-owner-password grant is
 * removed in OAuth 2.1, so we verify credentials here and let the OAuth2 Resource Server validate
 * the resulting token on every subsequent request.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        // Throws BadCredentialsException (-> 401) when credentials are wrong.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String token = jwtService.generateToken(user);
        return TokenResponse.bearer(token, jwtService.ttlSeconds());
    }
}
