package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.CategoryType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_parent", columnList = "parent_id, display_order"),
                @Index(name = "idx_categories_type", columnList = "type, is_active")
        }
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Self-reference: excluded from toString/equals or a parent-child pair recurses forever
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Category parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CategoryType type;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "display_order", nullable = false)
    Short displayOrder;

    @Column(name = "is_active", nullable = false)
    Boolean active;

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
