package com.veggiepal.entity;

import java.time.LocalDateTime;
import com.veggiepal.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** One live OTP per email/purpose. Codes and reset credentials are stored only as hashes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "email_otps", indexes = @Index(name = "idx_email_otps_email_purpose", columnList = "email, purpose"))
public class EmailOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30)")
    OtpPurpose purpose;
    @Column(name = "code_hash", nullable = false, length = 100)
    String codeHash;
    @Column(nullable = false)
    int attempts;
    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
    @Column(name = "verified_at")
    LocalDateTime verifiedAt;
    @Column(name = "reset_token_hash", length = 64, unique = true)
    String resetTokenHash;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
