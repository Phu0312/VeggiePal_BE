package com.veggiepal.recipe.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.veggiepal.recipe.enums.RecipeStatus;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(nullable = false, length = 200)
    String title;

    @Column(length = 2000)
    String description;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Column(nullable = false)
    Integer servings;

    @Column(name = "prep_time")
    Integer prepTime;

    @Column(name = "cook_time")
    Integer cookTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    RecipeStatus status;

    @OneToMany(
            mappedBy = "recipe",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    List<RecipeIngredient> ingredients =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "recipe",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stepNumber ASC")
    @Builder.Default
    List<RecipeStep> steps =
            new ArrayList<>();

    // task_be C18: denormalized from recipe_favorites, like blogs.vote_score. The DEFAULT lets
    // ddl-auto add the column to a table that already has rows.
    @Column(name = "favorite_count", nullable = false, columnDefinition = "int not null default 0")
    Integer favoriteCount;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @PrePersist
    void prePersist() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = RecipeStatus.DRAFT;
        }

        if (favoriteCount == null) {
            favoriteCount = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addIngredient(
            RecipeIngredient ingredient
    ) {

        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    public void addStep(
            RecipeStep step
    ) {

        steps.add(step);
        step.setRecipe(this);
    }
}