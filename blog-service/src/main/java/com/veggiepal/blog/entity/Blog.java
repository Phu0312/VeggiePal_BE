package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.ContentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "blogs",
        indexes = {
                @Index(name = "idx_blogs_status_published", columnList = "status, published_at"),
                @Index(name = "idx_blogs_author", columnList = "author_id, created_at"),
                @Index(name = "idx_blogs_category", columnList = "category_id, status")
        }
)
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // From the JWT userId claim. No FK: users live in another service's database.
    @Column(name = "author_id", nullable = false)
    Long authorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Category category;

    @Column(nullable = false, length = 200)
    String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    String content;

    @Column(name = "thumbnail_url", length = 512)
    String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ContentStatus status;

    @Column(name = "view_count", nullable = false)
    Integer viewCount;

    @Column(name = "vote_score", nullable = false)
    Integer voteScore;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (viewCount == null) {
            viewCount = 0;
        }

        if (voteScore == null) {
            voteScore = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
