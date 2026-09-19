package com.veggiepal.nutrition.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "health_records",
        indexes = @Index(name = "idx_health_records_user_recorded", columnList = "user_id, recorded_at")
)
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "height_cm", nullable = false, precision = 4, scale = 1)
    BigDecimal heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 4, scale = 1)
    BigDecimal weightKg;

    // precision 5: the extreme 300 kg / 0.5 m² gives 1200.0
    @Column(nullable = false, precision = 5, scale = 1)
    BigDecimal bmi;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
