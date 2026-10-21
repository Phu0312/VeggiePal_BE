package com.veggiepal.payment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "user_subscriptions", uniqueConstraints = @UniqueConstraint(name = "uk_user_subscriptions_user", columnNames = "user_id"))
public class UserSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;
}
