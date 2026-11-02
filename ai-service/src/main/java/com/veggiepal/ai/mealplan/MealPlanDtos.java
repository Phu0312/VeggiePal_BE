package com.veggiepal.ai.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public final class MealPlanDtos {
    private MealPlanDtos() {}

    public record GenerateRequest(
            @NotBlank @Size(max=160) String name,
            @NotBlank @Size(max=500) String healthGoal,
            @Size(max=20) List<@Size(max=100) String> preferences,
            @Size(max=30) List<@Size(max=100) String> availableIngredients,
            @Valid NutritionTarget dailyNutritionTarget,
            @NotNull GoalType goalType) {}

    public record NutritionTarget(
            @DecimalMin("0") BigDecimal minCalories, @DecimalMin("0") BigDecimal maxCalories,
            @DecimalMin("0") BigDecimal minProtein, @DecimalMin("0") BigDecimal maxProtein,
            @DecimalMin("0") BigDecimal minCarbohydrate, @DecimalMin("0") BigDecimal maxCarbohydrate,
            @DecimalMin("0") BigDecimal minFat, @DecimalMin("0") BigDecimal maxFat) {}

    public record NutritionTargetResponse(
            GoalType goalType,
            int bmr,
            int tdee,
            int targetCalories,
            NutritionTarget target) {}

    public record ReplaceMealRequest(
            @NotBlank String day,
            @NotBlank String mealType,
            Long currentRecipeId) {}

    // plan is a Map, not a JsonNode: Spring MVC writes with Jackson 3, which serializes
    // this service's Jackson 2 JsonNode as a bean ({"array":false,"nodeType":...}) instead of its JSON.
    public record MealPlanResponse(
            Long id,
            String name,
            String goal,
            Object plan,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Integer version,
            Boolean isFavorite,
            String goalType) {
        public MealPlanResponse(Long id, String name, String goal, Object plan, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this(id, name, goal, plan, createdAt, updatedAt, 1, false, null);
        }

        // The diary (nutrition-service) only accepts a plan's meals in the week it was created (task B02).
        // createdAt is written in this JVM's zone, so convert from there.
        @JsonProperty("createdOn")
        public LocalDate createdOn() {
            return createdAt == null ? null : createdAt.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        }

        public MealPlanResponse(Long id, String name, String goal, Object plan, LocalDateTime createdAt, LocalDateTime updatedAt, Integer version) {
            this(id, name, goal, plan, createdAt, updatedAt, version, false, null);
        }
    }

    public record MealPlanVersionResponse(
            Long id,
            Long mealPlanId,
            Integer version,
            String name,
            String goal,
            Object plan,
            String changeSummary,
            LocalDateTime createdAt,
            String goalType) {}

    public record UpdateFromHealthRequest(
            Boolean confirm,
            String changeSummary,
            @Valid NutritionTarget customTarget) {}

    public record UpdateFromHealthResponse(
            boolean confirmed,
            String message,
            NutritionTarget recalculatedTarget,
            Object currentPlan,
            Object proposedPlan,
            MealPlanResponse updatedMealPlan) {}

    public record ManualEditRequest(
            Boolean preview,
            String changeSummary,
            List<@Valid MealEditItem> edits) {}

    public record MealEditItem(
            @NotBlank String day,
            @NotBlank String mealType,
            String action,
            Long recipeId,
            @DecimalMin(value = "0.01", message = "SERVING_MULTIPLIER_INVALID")
            BigDecimal servingMultiplier,
            String note) {}

    public record MealPlanEditResponse(
            boolean preview,
            String message,
            Object plan,
            MealPlanResponse updatedMealPlan) {}
}
