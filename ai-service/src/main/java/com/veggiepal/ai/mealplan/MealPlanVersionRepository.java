package com.veggiepal.ai.mealplan;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanVersionRepository extends JpaRepository<MealPlanVersion, Long> {
    List<MealPlanVersion> findByMealPlanIdOrderByVersionDesc(Long mealPlanId);
    Optional<MealPlanVersion> findByMealPlanIdAndVersion(Long mealPlanId, Integer version);
}
