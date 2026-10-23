package com.veggiepal.ai.mealplan;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Map;

// Mifflin-St Jeor daily calorie target (spec 2026-10-21-calorie-target-design §3.2). Pure: the caller
// supplies today's date so tests can pin ages. All arithmetic stays unrounded until the final results.
public final class NutritionTargetCalculator {
    private static final Map<String, Integer> GENDER_OFFSET = Map.of("MALE", 5, "FEMALE", -161, "OTHER", -78);
    private static final Map<String, Double> ACTIVITY_FACTOR = Map.of(
            "SEDENTARY", 1.2, "LIGHT", 1.375, "MODERATE", 1.55, "ACTIVE", 1.725, "VERY_ACTIVE", 1.9);
    private static final Map<GoalType, Double> GOAL_FACTOR = Map.of(
            GoalType.LOSE, 0.85, GoalType.MAINTAIN, 1.0, GoalType.GAIN, 1.15);
    private static final double CEILING = 4000;
    private static final int ADULT_AGE = 18;

    private NutritionTargetCalculator() {}

    public static MealPlanDtos.NutritionTargetResponse calculate(BigDecimal heightCm, BigDecimal weightKg,
            LocalDate dateOfBirth, String gender, String activityLevel, GoalType goalType, LocalDate today) {
        if (heightCm == null || weightKg == null) throw new AppException(ErrorCode.HEALTH_RECORD_REQUIRED);
        Integer offset = gender == null ? null : GENDER_OFFSET.get(gender);
        Double activity = activityLevel == null ? null : ACTIVITY_FACTOR.get(activityLevel);
        if (dateOfBirth == null || offset == null || activity == null) {
            throw new AppException(ErrorCode.PROFILE_INCOMPLETE);
        }
        int age = Period.between(dateOfBirth, today).getYears();
        if (age < ADULT_AGE) throw new AppException(ErrorCode.AGE_NOT_SUPPORTED);

        double kg = weightKg.doubleValue();
        double bmr = 10 * kg + 6.25 * heightCm.doubleValue() - 5 * age + offset;
        double tdee = bmr * activity;
        double floor = "MALE".equals(gender) ? 1500 : 1200;
        double target = Math.min(Math.max(tdee * GOAL_FACTOR.get(goalType), floor), CEILING);

        MealPlanDtos.NutritionTarget range = new MealPlanDtos.NutritionTarget(
                whole(Math.max(Math.round(target * 0.9), (long) floor)),
                whole(Math.min(Math.round(target * 1.1), (long) CEILING)),
                whole(Math.round(kg * 1.2)), whole(Math.round(kg * 1.8)),
                whole(Math.round(target * 0.45 / 4)), whole(Math.round(target * 0.60 / 4)),
                whole(Math.round(target * 0.20 / 9)), whole(Math.round(target * 0.30 / 9)));
        return new MealPlanDtos.NutritionTargetResponse(goalType, (int) Math.round(bmr), (int) Math.round(tdee),
                (int) Math.round(target), range);
    }

    // Plans saved before goal_type existed only carry free text; read it the way the old code did.
    public static GoalType guessGoal(String goalText) {
        String g = goalText == null ? "" : goalText.toLowerCase(Locale.ROOT);
        if (g.contains("giảm") || g.contains("loss") || g.contains("diet")) return GoalType.LOSE;
        if (g.contains("tăng") || g.contains("gain") || g.contains("bulk")) return GoalType.GAIN;
        return GoalType.MAINTAIN;
    }

    private static BigDecimal whole(long value) {
        return BigDecimal.valueOf(value);
    }
}
