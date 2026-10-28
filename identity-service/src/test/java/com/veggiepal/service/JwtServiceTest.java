package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;

class JwtServiceTest {

    private static final String SECRET = "veggiepal-secret-key-must-be-at-least-32-characters";

    @Test
    void generateToken_isAcceptedByJwtDecoder() {
        User user = User.builder()
                .id(7L)
                .email("an@example.com")
                .role(Role.USER)
                .build();

        String token = new JwtService(SECRET).generateToken(user);
        JwtDecoder decoder = new JwtConfig().jwtDecoder(SECRET);
        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo("an@example.com");
        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(java.time.Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(900L);
    }

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> JwtConfig.signingKey("too-short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }

    @Test
    void generateToken_usesConfiguredLifetime() {
        JwtService service = new JwtService(SECRET, java.time.Duration.ofMinutes(3));
        User user = User.builder().id(7L).email("an@example.com").role(Role.USER).build();

        Jwt jwt = new JwtConfig().jwtDecoder(SECRET).decode(service.generateToken(user));

        assertThat(java.time.Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(180L);
        assertThat(service.accessTtlSeconds()).isEqualTo(180L);
    }
}
