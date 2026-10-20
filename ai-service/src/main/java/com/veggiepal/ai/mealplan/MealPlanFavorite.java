package com.veggiepal.ai.mealplan;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_meal_plan_favorites", uniqueConstraints = {
    @UniqueConstraint(name = "uk_meal_plan_user_favorite", columnNames = {"user_id", "meal_plan_id"})
})
@Getter
@NoArgsConstructor
public class MealPlanFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MealPlanFavorite(Long userId, MealPlan mealPlan) {
        this.userId = userId;
        this.mealPlan = mealPlan;
        this.createdAt = LocalDateTime.now();
    }
}
