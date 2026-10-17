package com.veggiepal.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

// Deliberately not a JwtDecoder bean: the resource server already uses the HS256 jwtDecoder bean.
@Slf4j
@Service
public class GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decoder;

    @Autowired
    public GoogleTokenVerifier(@Value("${google.client-id}") String clientId) {
        this(clientId, NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build());
    }

    // Package-private so tests can supply a decoder that needs no network but still gets the real validator.
    GoogleTokenVerifier(String clientId, NimbusJwtDecoder decoder) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("google.client-id (GOOGLE_CLIENT_ID) is empty: every Google sign-in will be rejected");
        }
        this.decoder = decoder;
        this.decoder.setJwtValidator(validator(clientId));
    }

    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        // getClaimAsString, not getIssuer(): getIssuer() coerces to URL and throws on "accounts.google.com".
        OAuth2TokenValidator<Jwt> audienceAndIssuer = jwt -> {
            List<String> audience = jwt.getAudience();
            String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
            boolean valid = audience != null
                    && audience.contains(clientId)
                    && issuer != null && ISSUERS.contains(issuer);
            return valid
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Not a Google ID token for this client", null));
        };
        return JwtValidators.createDefaultWithValidators(audienceAndIssuer);
    }

    public Jwt verify(String idToken) {
        try {
            return decoder.decode(idToken);
        } catch (JwtException e) {
            // Spring's messages never contain the token itself.
            log.info("Google ID token rejected: {}", e.getMessage());
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }
}
