package com.veggiepal.blog.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class JwtConfigTest {

    private static final String SECRET = "veggiepal-secret-key-must-be-at-least-32-characters";

    private final JwtDecoder decoder = new JwtConfig().jwtDecoder(SECRET);

    @Test
    void jwtDecoder_acceptsHs256TokenSignedWithSharedSecret() throws Exception {
        Jwt jwt = decoder.decode(signedToken(JWSAlgorithm.HS256));

        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
    }

    @Test
    void jwtDecoder_rejectsTokenSignedWithOtherAlgorithm() throws Exception {
        String hs384Token = signedToken(JWSAlgorithm.HS384);

        assertThatThrownBy(() -> decoder.decode(hs384Token)).isInstanceOf(JwtException.class);
    }

    private static String signedToken(JWSAlgorithm algorithm) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("an@example.com")
                .claim("userId", 7L)
                .claim("role", "USER")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(algorithm), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
