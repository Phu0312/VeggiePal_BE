package com.veggiepal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_badges", indexes = @Index(name = "idx_badge_user_type", columnList = "user_id,badge_type"))
@Getter @Setter @NoArgsConstructor
public class UserBadge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "badge_type", nullable = false, length = 20)
    private String badgeType;
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
    @Column(name = "granted_by")
    private Long grantedBy;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(name = "revoked_by")
    private Long revokedBy;
    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    public UserBadge(Long userId, String badgeType, LocalDateTime grantedAt, Long grantedBy) {
        this.userId = userId;
        this.badgeType = badgeType;
        this.grantedAt = grantedAt;
        this.grantedBy = grantedBy;
    }
}
