package com.veggiepal.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.entity.User;

import io.jsonwebtoken.Jwts;

@Service
public class JwtService {

    private static final long EXPIRATION =
            1000 * 60 * 60 * 24;

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = JwtConfig.signingKey(secret);
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
                                        + EXPIRATION
                        )
                )
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}