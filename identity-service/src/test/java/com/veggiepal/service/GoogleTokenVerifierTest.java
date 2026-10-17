package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

class GoogleTokenVerifierTest {

    static final String CLIENT_ID = "test-client.apps.googleusercontent.com";

    final OAuth2TokenValidator<Jwt> validator = GoogleTokenVerifier.validator(CLIENT_ID);

    @Test
    void validator_acceptsTokenIssuedForThisClient() {
        Jwt jwt = token(CLIENT_ID, "https://accounts.google.com", Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void validator_acceptsIssuerWithoutScheme() {
        Jwt jwt = token(CLIENT_ID, "accounts.google.com", Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void validator_rejectsTokenForAnotherClient() {
        Jwt jwt = token("other-client.apps.googleusercontent.com", "https://accounts.google.com",
                Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validator_rejectsNonGoogleIssuer() {
        Jwt jwt = token(CLIENT_ID, "https://evil.example.com", Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validator_rejectsExpiredToken() {
        Jwt jwt = token(CLIENT_ID, "https://accounts.google.com", Instant.now().minusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void verify_malformedToken_throwsGoogleTokenInvalid() {
        // Parsing fails before the decoder ever fetches Google's JWKS, so this needs no network.
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID);

        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID));
    }

    @Test
    void validator_rejectsTokenWithoutIssuer() {
        Jwt jwt = Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .audience(List.of(CLIENT_ID))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void verify_signedTokenForThisClient_returnsJwt() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).generate();
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID,
                NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build());

        Jwt jwt = verifier.verify(signed(key, CLIENT_ID));

        assertThat(jwt.getClaimAsString("email")).isEqualTo("user@gmail.com");
    }

    @Test
    void verify_signedTokenForAnotherClient_throwsGoogleTokenInvalid() throws Exception {
        // Fails if GoogleTokenVerifier stops installing validator(): a default decoder only checks exp/nbf.
        RSAKey key = new RSAKeyGenerator(2048).generate();
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID,
                NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build());
        String idToken = signed(key, "other-client.apps.googleusercontent.com");

        assertThatThrownBy(() -> verifier.verify(idToken))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID));
    }

    static String signed(RSAKey key, String audience) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(audience)
                .claim("email", "user@gmail.com")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(),
                claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    static Jwt token(String audience, String issuer, Instant expiresAt) {
        return Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .audience(List.of(audience))
                .claim("iss", issuer)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
    }
}
