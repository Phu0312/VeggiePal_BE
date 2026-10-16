package com.veggiepal.ai.chat;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestSessionRepository extends JpaRepository<GuestSession, String> {
    long countByIpHashAndCreatedAtAfter(String ipHash, LocalDateTime since);
    Optional<GuestSession> findByIdAndIpHashAndExpiresAtAfter(String id, String ipHash, LocalDateTime now);
}
