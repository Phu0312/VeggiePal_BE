package com.veggiepal.ai.chat;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestSessionService {
    private final GuestSessionRepository repository;
    private final String pepper;
    private final Duration ttl;
    private final int maxSessionsPerIpPerDay;

    GuestSessionService(GuestSessionRepository repository,
            @Value("${ai.guest.session-pepper}") String pepper,
            @Value("${ai.guest.session-ttl:24h}") Duration ttl,
            @Value("${ai.guest.max-sessions-per-ip-per-day:3}") int maxSessionsPerIpPerDay) {
        this.repository = repository;
        this.pepper = pepper;
        this.ttl = ttl;
        this.maxSessionsPerIpPerDay = maxSessionsPerIpPerDay;
    }

    @Transactional
    public GuestSession create(String remoteAddress) {
        String ipHash = hash(remoteAddress);
        if (repository.countByIpHashAndCreatedAtAfter(ipHash, LocalDate.now().atStartOfDay())
                >= maxSessionsPerIpPerDay) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        }
        LocalDateTime now = LocalDateTime.now();
        return repository.save(new GuestSession(UUID.randomUUID().toString(), ipHash, now, now.plus(ttl)));
    }

    @Transactional(readOnly = true)
    public void requireValid(String sessionId, String remoteAddress) {
        repository.findByIdAndIpHashAndExpiresAtAfter(sessionId, hash(remoteAddress), LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }

    private String hash(String remoteAddress) {
        try {
            String input = (remoteAddress == null ? "unknown" : remoteAddress) + ":" + pepper;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash guest address", exception);
        }
    }
}
