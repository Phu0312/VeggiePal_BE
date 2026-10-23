package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NutritionTargetCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    private static MealPlanDtos.NutritionTargetResponse calc(int kg, int cm, int age, String gender,
            String activity, GoalType goal) {
        return NutritionTargetCalculator.calculate(BigDecimal.valueOf(cm), BigDecimal.valueOf(kg),
                TODAY.minusYears(age), gender, activity, goal, TODAY);
    }

    private static void assertRange(MealPlanDtos.NutritionTargetResponse r, int min, int max) {
        assertEquals(0, BigDecimal.valueOf(min).compareTo(r.target().minCalories()));
        assertEquals(0, BigDecimal.valueOf(max).compareTo(r.target().maxCalories()));
    }

    @Test
    void maleLightGain() {
        var r = calc(80, 180, 25, "MALE", "LIGHT", GoalType.GAIN);
        assertEquals(1805, r.bmr());
        assertEquals(2482, r.tdee());
        assertEquals(2854, r.targetCalories());
        assertRange(r, 2569, 3140);
        assertEquals(GoalType.GAIN, r.goalType());
        assertEquals(0, BigDecimal.valueOf(96).compareTo(r.target().minProtein()));
        assertEquals(0, BigDecimal.valueOf(144).compareTo(r.target().maxProtein()));
        assertEquals(0, BigDecimal.valueOf(321).compareTo(r.target().minCarbohydrate()));
        assertEquals(0, BigDecimal.valueOf(428).compareTo(r.target().maxCarbohydrate()));
        assertEquals(0, BigDecimal.valueOf(63).compareTo(r.target().minFat()));
        assertEquals(0, BigDecimal.valueOf(95).compareTo(r.target().maxFat()));
    }

    @Test
    void femaleLightMaintain() {
        var r = calc(60, 165, 30, "FEMALE", "LIGHT", GoalType.MAINTAIN);
        assertEquals(1320, r.bmr());
        assertEquals(1815, r.tdee());
        assertEquals(1815, r.targetCalories());
        assertRange(r, 1634, 1997);
    }

    @Test
    void femaleSedentaryLoseIsRaisedToTheFloor() {
        var r = calc(45, 150, 60, "FEMALE", "SEDENTARY", GoalType.LOSE);
        assertEquals(927, r.bmr());
        assertEquals(1112, r.tdee());
        assertEquals(1200, r.targetCalories());
        assertRange(r, 1200, 1320);
    }

    @Test
    void maleVeryActiveGainIsCappedAtTheCeiling() {
        var r = calc(150, 200, 20, "MALE", "VERY_ACTIVE", GoalType.GAIN);
        assertEquals(2655, r.bmr());
        assertEquals(5045, r.tdee());
        assertEquals(4000, r.targetCalories());
        assertRange(r, 3600, 4000);
    }

    @Test
    void otherUsesTheAverageOffsetAndTheLowerFloor() {
        // 10 x 60 + 6.25 x 165 - 5 x 30 - 78 = 1403.25
        assertEquals(1403, calc(60, 165, 30, "OTHER", "SEDENTARY", GoalType.MAINTAIN).bmr());
    }

    @Test
    void missingOrUnknownProfileFieldsAreProfileIncomplete() {
        BigDecimal cm = BigDecimal.valueOf(170), kg = BigDecimal.valueOf(65);
        LocalDate dob = TODAY.minusYears(30);
        for (Runnable call : new Runnable[] {
                () -> NutritionTargetCalculator.calculate(cm, kg, null, "MALE", "LIGHT", GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, null, "LIGHT", GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, "MALE", null, GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, "ROBOT", "LIGHT", GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, "MALE", "COUCH", GoalType.MAINTAIN, TODAY)}) {
            AppException ex = assertThrows(AppException.class, call::run);
            assertEquals(ErrorCode.PROFILE_INCOMPLETE, ex.getErrorCode());
        }
    }

    @Test
    void missingHeightOrWeightIsHealthRecordRequired() {
        AppException ex = assertThrows(AppException.class, () -> NutritionTargetCalculator.calculate(
                BigDecimal.valueOf(170), null, TODAY.minusYears(30), "MALE", "LIGHT", GoalType.MAINTAIN, TODAY));
        assertEquals(ErrorCode.HEALTH_RECORD_REQUIRED, ex.getErrorCode());
    }

    @Test
    void underEighteenIsRejectedAndTheEighteenthBirthdayIsAccepted() {
        BigDecimal cm = BigDecimal.valueOf(170), kg = BigDecimal.valueOf(60);
        AppException ex = assertThrows(AppException.class, () -> NutritionTargetCalculator.calculate(
                cm, kg, TODAY.minusYears(18).plusDays(1), "FEMALE", "LIGHT", GoalType.MAINTAIN, TODAY));
        assertEquals(ErrorCode.AGE_NOT_SUPPORTED, ex.getErrorCode());

        assertEquals(GoalType.MAINTAIN, NutritionTargetCalculator.calculate(
                cm, kg, TODAY.minusYears(18), "FEMALE", "LIGHT", GoalType.MAINTAIN, TODAY).goalType());
    }

    @Test
    void guessGoalReadsTheOldFreeText() {
        assertEquals(GoalType.LOSE, NutritionTargetCalculator.guessGoal("Giảm mỡ toàn thân"));
        assertEquals(GoalType.GAIN, NutritionTargetCalculator.guessGoal("Tăng cơ"));
        assertEquals(GoalType.MAINTAIN, NutritionTargetCalculator.guessGoal("Ăn lành mạnh"));
        assertEquals(GoalType.MAINTAIN, NutritionTargetCalculator.guessGoal(null));
    }
}
