package com.veggiepal.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "subscription_plans", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_plan_code", columnNames = "code"))
public class SubscriptionPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PlanCode code;
    @Column(nullable = false)
    private long price;
    @Column(nullable = false)
    private int durationDays;
    @Column(nullable = false)
    private int chatPerDay;
    @Column(nullable = false)
    private int mealPlansPerMonth;
    @Column(nullable = false)
    private int videosPerMonth;
    @Column(nullable = false)
    private boolean active;
}
