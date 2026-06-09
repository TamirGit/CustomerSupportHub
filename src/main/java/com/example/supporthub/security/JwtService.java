package com.example.supporthub.security;

import com.example.supporthub.config.JwtProperties;
import com.example.supporthub.domain.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Mints signed JWT access tokens for authenticated users. The {@code roles} claim is consumed by
 * {@code SecurityConfig}'s authentication converter to rebuild authorities on each request.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public long ttlSeconds() {
        return properties.ttlSeconds();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("customer-support-hub")
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttlSeconds(), ChronoUnit.SECONDS))
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", user.getRole().name())
                .build();
        // Must declare HS256 explicitly: NimbusJwtEncoder otherwise defaults the header to RS256,
        // which has no matching key in our symmetric-secret JWK source and fails to encode.
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
