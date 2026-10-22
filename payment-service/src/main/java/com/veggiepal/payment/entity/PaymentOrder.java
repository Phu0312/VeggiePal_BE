package com.veggiepal.payment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_orders_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_payment_orders_status_paid", columnList = "status,paid_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_payment_orders_order_code", columnNames = "order_code"))
public class PaymentOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_code", nullable = false)
    private Long orderCode;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;
    @Column(nullable = false)
    private long amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentStatus status;
    @Column(name = "payment_link_id", length = 100)
    private String paymentLinkId;
    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;
    @Column(name = "qr_code", length = 4096)
    private String qrCode;
    @Column(name = "account_name", length = 255)
    private String accountName;
    @Column(name = "account_number", length = 100)
    private String accountNumber;
    @Column(length = 20)
    private String bin;
    @Column(length = 255)
    private String description;
    @Column(name = "expired_at")
    private Instant expiredAt;
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Version
    private long version;
}
