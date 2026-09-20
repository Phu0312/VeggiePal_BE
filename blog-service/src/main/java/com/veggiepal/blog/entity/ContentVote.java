package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "content_votes",
        uniqueConstraints = @UniqueConstraint(
                // FR-04-03: the database refuses a duplicate vote, not just the code
                name = "uk_content_votes_user_target",
                columnNames = {"user_id", "target_type", "target_id"}
        )
)
public class ContentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    TargetType targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    /** -1 or 1. A number, not an enum, so vote_score can be summed arithmetically. */
    @Column(nullable = false)
    Integer value;

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
