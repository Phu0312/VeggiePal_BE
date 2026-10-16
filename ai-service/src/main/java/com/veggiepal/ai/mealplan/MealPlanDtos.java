package com.veggiepal.ai.mealplan;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public final class MealPlanDtos {
    private MealPlanDtos() {}
    public record GenerateRequest(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=500) String healthGoal,
                                  @Size(max=20) List<@Size(max=100) String> preferences,
                                  @Size(max=30) List<@Size(max=100) String> availableIngredients,
                                  @Valid NutritionTarget dailyNutritionTarget) {}
    public record NutritionTarget(
            @DecimalMin("0") BigDecimal minCalories, @DecimalMin("0") BigDecimal maxCalories,
            @DecimalMin("0") BigDecimal minProtein, @DecimalMin("0") BigDecimal maxProtein,
            @DecimalMin("0") BigDecimal minCarbohydrate, @DecimalMin("0") BigDecimal maxCarbohydrate,
            @DecimalMin("0") BigDecimal minFat, @DecimalMin("0") BigDecimal maxFat) {}
    public record ReplaceMealRequest(@NotBlank String day,@NotBlank String mealType,Long currentRecipeId) {}
    public record MealPlanResponse(Long id,String name,String goal,JsonNode plan,LocalDateTime createdAt,LocalDateTime updatedAt) {}
}
