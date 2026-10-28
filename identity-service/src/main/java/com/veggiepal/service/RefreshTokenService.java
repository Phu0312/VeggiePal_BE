package com.veggiepal.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.veggiepal.entity.RefreshToken;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.RefreshTokenRepository;
import com.veggiepal.repository.UserRepository;

/**
 * task_be C11 + A06: 7-day sessions behind 15-minute access tokens. Deleting a user's rows
 * (revokeAll) signs them out everywhere within one access-token lifetime, with no change
 * to the services that validate access tokens.
 * ponytail: no reuse detection of rotated tokens and no purge job for expired rows; add both
 * if sessions ever get stolen or the table grows.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repository, UserRepository userRepository,
                               @Value("${jwt.refresh-ttl:P7D}") Duration ttl) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.ttl = ttl;
    }

    @Transactional
    public String issue(Long userId) {
        lockUser(userId);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256(raw))
                .expiresAt(LocalDateTime.now().plus(ttl))
                .build());
        return raw;
    }

    /** Consumes the token (one use) and returns whose it was. */
    @Transactional
    public Long rotate(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        String hash = sha256(raw);
        Long userId = repository.findUserIdByTokenHash(hash)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        // Every session writer takes the user lock first. The locking read sees a token
        // consumed/revoked while we waited, even under MySQL's REPEATABLE READ isolation.
        lockUser(userId);
        RefreshToken token = repository.findForRotation(hash)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        repository.delete(token);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return token.getUserId();
    }

    @Transactional
    public void revoke(String raw) {
        if (StringUtils.hasText(raw)) {
            String hash = sha256(raw);
            repository.findUserIdByTokenHash(hash).ifPresent(userId -> {
                userRepository.lockById(userId);
                repository.deleteByTokenHash(hash);
            });
        }
    }

    @Transactional
    public void revokeAll(Long userId) {
        userRepository.lockById(userId);
        repository.deleteByUserId(userId);
    }

    private void lockUser(Long userId) {
        userRepository.lockById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    static String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
