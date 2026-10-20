package com.veggiepal.ai.chat;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestSessionService {
    private final GuestSessionRepository repository;
    private final Duration ttl;
    private final String pepper;
    private final int maxSessionsPerIpPerDay;

    GuestSessionService(GuestSessionRepository repository,
            @Value("${ai.guest.session-ttl:24h}") Duration ttl,
            @Value("${ai.guest.session-pepper}") String pepper,
            @Value("${ai.guest.max-sessions-per-ip-per-day:3}") int maxSessionsPerIpPerDay) {
        this.repository = repository;
        this.ttl = ttl;
        this.pepper = pepper;
        this.maxSessionsPerIpPerDay = maxSessionsPerIpPerDay;
    }

    @Transactional
    public GuestSession create(String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        String ipHash = hash(clientIp);
        if (repository.countByIpHashAndCreatedAtGreaterThanEqual(ipHash, now.toLocalDate().atStartOfDay())
                >= maxSessionsPerIpPerDay) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        }
        return repository.save(new GuestSession(UUID.randomUUID().toString(), ipHash, now, now.plus(ttl)));
    }

    @Transactional(readOnly = true)
    public GuestSession requireValid(String sessionId, String clientIp) {
        GuestSession session = repository.findByIdAndExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!MessageDigest.isEqual(session.getIpHash().getBytes(StandardCharsets.US_ASCII),
                hash(clientIp).getBytes(StandardCharsets.US_ASCII))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return session;
    }

    private String hash(String clientIp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pepper.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            return HexFormat.of().formatHex(digest.digest(clientIp.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
