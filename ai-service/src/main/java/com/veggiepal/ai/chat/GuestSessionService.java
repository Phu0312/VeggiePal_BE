package com.veggiepal.ai.chat;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestSessionService {
    private final GuestSessionRepository repository;
    private final Duration ttl;

    GuestSessionService(GuestSessionRepository repository,
            @Value("${ai.guest.session-ttl:24h}") Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    @Transactional
    public GuestSession create() {
        LocalDateTime now = LocalDateTime.now();
        return repository.save(new GuestSession(UUID.randomUUID().toString(), "guest", now, now.plus(ttl)));
    }

    @Transactional(readOnly = true)
    public void requireValid(String sessionId) {
        repository.findByIdAndExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }
}
