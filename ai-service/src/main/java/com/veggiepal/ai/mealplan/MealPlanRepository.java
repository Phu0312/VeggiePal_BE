package com.veggiepal.ai.mealplan;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanRepository extends JpaRepository<MealPlan,Long> {
    List<MealPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MealPlan> findByIdAndUserId(Long id,Long userId);
}
