package com.veggiepal.ai.mealplan;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanFavoriteRepository extends JpaRepository<MealPlanFavorite, Long> {
    boolean existsByUserIdAndMealPlanId(Long userId, Long mealPlanId);
    Optional<MealPlanFavorite> findByUserIdAndMealPlanId(Long userId, Long mealPlanId);
    Page<MealPlanFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    void deleteByUserIdAndMealPlanId(Long userId, Long mealPlanId);
}
