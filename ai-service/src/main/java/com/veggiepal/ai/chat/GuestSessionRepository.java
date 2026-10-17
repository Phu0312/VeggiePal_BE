package com.veggiepal.ai.chat;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestSessionRepository extends JpaRepository<GuestSession, String> {
    Optional<GuestSession> findByIdAndExpiresAtAfter(String id, LocalDateTime now);
}
