package com.veggiepal.ai.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_guest_sessions")
@Getter
@NoArgsConstructor
public class GuestSession {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public GuestSession(String id, String ipHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.ipHash = ipHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public GuestSession(String id, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this(id, "guest", createdAt, expiresAt);
    }
}
