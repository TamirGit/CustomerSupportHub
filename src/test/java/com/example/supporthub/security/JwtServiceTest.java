package com.example.supporthub.security;

import com.example.supporthub.config.JwtConfig;
import com.example.supporthub.config.JwtProperties;
import com.example.supporthub.domain.Role;
import com.example.supporthub.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real HS256 encoder/decoder (no mocks, no database). Guards against regressions in
 * token signing — e.g. the encoder defaulting to RS256 and failing to sign with a symmetric key.
 */
class JwtServiceTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("unit-test-secret-key-that-is-at-least-32-bytes-long", 3600);

    private final JwtConfig jwtConfig = new JwtConfig(PROPERTIES);
    private final JwtService jwtService = new JwtService(jwtConfig.jwtEncoder(), PROPERTIES);
    private final JwtDecoder jwtDecoder = jwtConfig.jwtDecoder();

    @Test
    void generatesHs256TokenThatDecodesWithExpectedClaims() {
        User user = new User("amy", "hash", "Amy Agent", "amy@x.io", Role.AGENT, null);
        ReflectionTestUtils.setField(user, "id", 5L);

        String token = jwtService.generateToken(user);
        Jwt decoded = jwtDecoder.decode(token); // would throw if signature/alg were wrong

        assertThat(decoded.getSubject()).isEqualTo("amy");
        assertThat(decoded.getClaimAsString("roles")).isEqualTo("AGENT");
        assertThat(decoded.getClaim("uid").toString()).isEqualTo("5");
        assertThat(decoded.getHeaders().get("alg")).hasToString("HS256");
        assertThat(decoded.getExpiresAt()).isNotNull();
    }
}
