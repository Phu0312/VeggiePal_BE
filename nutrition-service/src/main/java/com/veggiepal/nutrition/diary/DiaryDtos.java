package com.veggiepal.nutrition.diary;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class DiaryDtos {
    private DiaryDtos() {}
    public enum Source { MEAL_PLAN, MANUAL }
    public enum MealType { BREAKFAST, LUNCH, DINNER, SNACK }
    public record ConfirmMealRequest(LocalDate date,
            @DecimalMin(value="0.01", message="INVALID_DIARY_SERVINGS")
            @DecimalMax(value="100", message="INVALID_DIARY_SERVINGS")
            @Digits(integer=3, fraction=2, message="INVALID_DIARY_SERVINGS") BigDecimal servings) {}
    public record WaterRequest(LocalDate date,
            @NotNull(message="INVALID_WATER_AMOUNT") @Min(value=1, message="INVALID_WATER_AMOUNT")
            @Max(value=100000, message="INVALID_WATER_AMOUNT") Integer milliliters) {}
    public record ActivityRequest(LocalDate date,
            @NotBlank(message="INVALID_ACTIVITY") @Size(max=100, message="INVALID_ACTIVITY") String activityType,
            @NotNull(message="INVALID_ACTIVITY") @Min(value=1, message="INVALID_ACTIVITY")
            @Max(value=1440, message="INVALID_ACTIVITY") Integer minutes) {}
    public record Nutrition(BigDecimal calories, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {
        public static Nutrition zero() { return new Nutrition(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO); }
        public Nutrition plus(Nutrition other) {
            return new Nutrition(calories.add(other.calories),protein.add(other.protein),
                    carbohydrate.add(other.carbohydrate),fat.add(other.fat));
        }
    }
    public record MealSnapshot(Long recipeId, String title, BigDecimal plannedServings, Nutrition nutrition,
            LocalDate planCreatedOn) {}
    public record MealEntry(Long id, LocalDate date, Source source, Long mealPlanId, MealType mealType,
            Long recipeId, String title, BigDecimal servings, Nutrition nutrition, Instant recordedAt) {}
    public record WaterEntry(Long id, LocalDate date, int milliliters) {}
    public record ActivityEntry(Long id, LocalDate date, String activityType, int minutes) {}
    // hasMeals tells a day with nothing logged apart from logged meals that add up to 0 kcal.
    public record DailySummary(LocalDate date, String timezone, List<MealEntry> meals, Nutrition nutrition,
            boolean hasMeals, List<WaterEntry> water, long totalWaterMl, List<ActivityEntry> activities, long totalActivityMinutes) {}
}
