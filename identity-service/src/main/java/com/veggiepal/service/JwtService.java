package com.veggiepal.service;

import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.entity.User;

import io.jsonwebtoken.Jwts;

@Service
public class JwtService {

    private final SecretKey signingKey;

    // task_be C11: short-lived; the 7-day session lives in the refresh token
    private final Duration accessTtl;

    @Autowired
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-ttl:PT15M}") Duration accessTtl) {
        this.signingKey = JwtConfig.signingKey(secret);
        this.accessTtl = accessTtl;
    }

    /** For tests and callers that only have the secret. */
    public JwtService(String secret) {
        this(secret, Duration.ofMinutes(15));
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + accessTtl.toMillis()
                        )
                )
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}