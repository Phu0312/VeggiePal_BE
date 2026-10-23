package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MealPlanValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MealPlanValidator validator = new MealPlanValidator(mapper);

    @Test
    void calculatesMacrosDeterministicallyForSevenDayPlan() {
        ArrayNode recipes = mapper.createArrayNode().add(recipe(false));
        ObjectNode plan = sevenDayPlan(1L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), null, true);

        assertEquals(new BigDecimal("76.00"),
                plan.path("days").get(0).path("meals").get(0).path("nutrition").path("calories").decimalValue());
        assertEquals(new BigDecimal("228.00"),
                plan.path("days").get(0).path("nutritionTotals").path("calories").decimalValue());
    }

    @Test
    void rejectsAmbiguousAllergenBeforeCallingAi() {
        ArrayNode allergies = mapper.createArrayNode();
        allergies.addObject().put("code", "SOY").put("name", "Đậu nành");
        assertThrows(AppException.class,
                () -> validator.safeCandidates(mapper.createArrayNode().add(recipe(true)), allergies, List.of()));
    }

    @Test
    void doesNotRejectSafeAllergenWhenUserHasDifferentAllergy() {
        // User is allergic ONLY to PEANUT
        ArrayNode allergies = mapper.createArrayNode();
        allergies.addObject().put("code", "PEANUT").put("name", "Đậu phộng");

        // Recipe has tofu (SOY, allergen=true)
        ObjectNode soyRecipe = recipe(true);
        soyRecipe.put("id", 101);

        ArrayNode candidates = validator.safeCandidates(mapper.createArrayNode().add(soyRecipe), allergies, List.of());
        assertEquals(1, candidates.size());
        assertEquals(101L, candidates.get(0).path("id").asLong());
    }

    @Test
    void rejectsMatchingAllergen() {
        // User is allergic to SOY
        ArrayNode allergies = mapper.createArrayNode();
        allergies.addObject().put("code", "SOY").put("name", "Đậu nành");

        // Recipe has tofu (SOY)
        ObjectNode soyRecipe = recipe(true);
        assertThrows(AppException.class,
                () -> validator.safeCandidates(mapper.createArrayNode().add(soyRecipe), allergies, List.of()));
    }

    @Test
    void rejectsHiddenAllergenEvenIfFlagIsFalse() {
        // User is allergic to TOMATO
        ArrayNode allergies = mapper.createArrayNode();
        allergies.addObject().put("code", "TOMATO").put("name", "Cà chua");

        // Recipe has tomato with allergen: false
        ObjectNode tomatoRecipe = mapper.createObjectNode();
        tomatoRecipe.put("id", 102).put("title", "Salad cà chua").put("servings", 1);
        tomatoRecipe.putArray("ingredients").addObject()
                .put("ingredientName", "Cà chua").put("normalizedName", "ca chua")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 18).put("proteinPer100g", 0.9)
                .put("carbsPer100g", 3.9).put("fatPer100g", 0.2)
                .put("vegan", true).put("allergen", false);

        assertThrows(AppException.class,
                () -> validator.safeCandidates(mapper.createArrayNode().add(tomatoRecipe), allergies, List.of()));
    }

    @Test
    void rejectsUnverifiedAllergenWhenUserHasAllergies() {
        // User has allergies
        ArrayNode allergies = mapper.createArrayNode();
        allergies.addObject().put("code", "PEANUT").put("name", "Đậu phộng");

        // Recipe has allergen=true but unidentified ingredient
        ObjectNode unverifiedRecipe = mapper.createObjectNode();
        unverifiedRecipe.put("id", 103).put("title", "Món thảo mộc").put("servings", 1);
        unverifiedRecipe.putArray("ingredients").addObject()
                .put("ingredientName", "Cây cỏ lạ").put("normalizedName", "cay co la")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 30).put("proteinPer100g", 1)
                .put("carbsPer100g", 5).put("fatPer100g", 0.1)
                .put("vegan", true).put("allergen", true);

        assertThrows(AppException.class,
                () -> validator.safeCandidates(mapper.createArrayNode().add(unverifiedRecipe), allergies, List.of()));
    }

    @Test
    void rejectsMissingAllergenInfoWhenUserHasAllergies() {
        ArrayNode allergies = mapper.createArrayNode();
        allergies.addObject().put("code", "PEANUT").put("name", "Đậu phộng");

        // Recipe has allergen=null
        ObjectNode missingAllergenRecipe = mapper.createObjectNode();
        missingAllergenRecipe.put("id", 104).put("title", "Món thiếu allergen").put("servings", 1);
        missingAllergenRecipe.putArray("ingredients").addObject()
                .put("ingredientName", "Rau bina").put("normalizedName", "rau bina")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 23).put("proteinPer100g", 2.9)
                .put("carbsPer100g", 3.6).put("fatPer100g", 0.4)
                .put("vegan", true).putNull("allergen");

        assertThrows(AppException.class,
                () -> validator.safeCandidates(mapper.createArrayNode().add(missingAllergenRecipe), allergies, List.of()));
    }

    @Test
    void calculatesMacrosWithDiverseGroundedUnits() {
        ObjectNode multiUnitRecipe = mapper.createObjectNode();
        multiUnitRecipe.put("id", 2).put("title", "Canh chay thập cẩm").put("servings", 1);
        ArrayNode ingredients = multiUnitRecipe.putArray("ingredients");
        // 1 củ cà rốt = 100g -> factor = 1.0 -> 41 kcal
        ingredients.addObject()
                .put("ingredientName", "Cà rốt").put("normalizedName", "ca rot")
                .put("quantity", 1).put("unit", "củ")
                .put("caloriesPer100g", 41).put("proteinPer100g", 1)
                .put("carbsPer100g", 10).put("fatPer100g", 0.2)
                .put("vegan", true).put("allergen", false);
        // 1 quả cà chua = 120g -> factor = 1.2 -> 18 * 1.2 = 21.6 kcal
        ingredients.addObject()
                .put("ingredientName", "Cà chua").put("normalizedName", "ca chua")
                .put("quantity", 1).put("unit", "quả")
                .put("caloriesPer100g", 18).put("proteinPer100g", 0.9)
                .put("carbsPer100g", 3.9).put("fatPer100g", 0.2)
                .put("vegan", true).put("allergen", false);

        ArrayNode candidates = validator.safeCandidates(mapper.createArrayNode().add(multiUnitRecipe), mapper.createArrayNode(), List.of());
        assertEquals(1, candidates.size());

        ArrayNode prompt = validator.promptCandidates(candidates);
        assertEquals(1, prompt.size());
        assertEquals(new BigDecimal("62.60"), prompt.get(0).path("nutritionPerServing").path("calories").decimalValue());
    }

    @Test
    void filtersOutRecipeWithInvalidUnitWithoutThrowing503() {
        ObjectNode validRecipe = recipe(false);
        validRecipe.put("id", 10);

        ObjectNode invalidRecipe = mapper.createObjectNode();
        invalidRecipe.put("id", 11).put("title", "Món đơn vị lạ").put("servings", 1);
        invalidRecipe.putArray("ingredients").addObject()
                .put("ingredientName", "Rau rừng").put("normalizedName", "rau rung")
                .put("quantity", 1).put("unit", "nam_tay_khong_xac_dinh")
                .put("caloriesPer100g", 50).put("proteinPer100g", 2)
                .put("carbsPer100g", 5).put("fatPer100g", 1)
                .put("vegan", true).put("allergen", false);

        ArrayNode input = mapper.createArrayNode().add(invalidRecipe).add(validRecipe);
        ArrayNode safe = validator.safeCandidates(input, mapper.createArrayNode(), List.of());

        assertEquals(1, safe.size());
        assertEquals(10L, safe.get(0).path("id").asLong());
    }

    @Test
    void filtersOutRecipeWithMissingOrNegativeNutrient() {
        ObjectNode validRecipe = recipe(false);
        validRecipe.put("id", 20);

        ObjectNode missingNutrientRecipe = mapper.createObjectNode();
        missingNutrientRecipe.put("id", 21).put("title", "Món thiếu calo").put("servings", 1);
        missingNutrientRecipe.putArray("ingredients").addObject()
                .put("ingredientName", "Nấm").put("normalizedName", "nam")
                .put("quantity", 100).put("unit", "GRAM")
                .putNull("caloriesPer100g").put("proteinPer100g", 2)
                .put("carbsPer100g", 5).put("fatPer100g", 1)
                .put("vegan", true).put("allergen", false);

        ArrayNode input = mapper.createArrayNode().add(missingNutrientRecipe).add(validRecipe);
        ArrayNode safe = validator.safeCandidates(input, mapper.createArrayNode(), List.of());

        assertEquals(1, safe.size());
        assertEquals(20L, safe.get(0).path("id").asLong());
    }

    @Test
    void throwsClearBusinessErrorWhenNoValidRecipesRemain() {
        ObjectNode invalidRecipe = mapper.createObjectNode();
        invalidRecipe.put("id", 30).put("title", "Món lỗi").put("servings", 1);
        invalidRecipe.putArray("ingredients").addObject()
                .put("ingredientName", "Bột bí ẩn").put("normalizedName", "bot bi an")
                .put("quantity", 1).put("unit", "muong_khong_ro")
                .put("caloriesPer100g", 100).put("proteinPer100g", 2)
                .put("carbsPer100g", 5).put("fatPer100g", 1)
                .put("vegan", true).put("allergen", false);

        AppException ex = assertThrows(AppException.class,
                () -> validator.safeCandidates(mapper.createArrayNode().add(invalidRecipe), mapper.createArrayNode(), List.of()));
        assertEquals(com.veggiepal.ai.common.ErrorCode.NO_VALID_RECIPES, ex.getErrorCode());
    }

    @Test
    void prioritizesRecipesWithAvailableIngredientsAndDoesNotFilterOutZeroMatches() {
        ObjectNode recipe1 = recipeWithIngredients(1, "Món 1", List.of("Đậu hũ", "Nấm rơm"));
        ObjectNode recipe2 = recipeWithIngredients(2, "Món 2", List.of("Đậu hũ"));
        ObjectNode recipe3 = recipeWithIngredients(3, "Món 3", List.of("Cà rốt"));

        ArrayNode input = mapper.createArrayNode().add(recipe3).add(recipe2).add(recipe1);
        List<String> available = List.of("Đậu hũ", "Nấm rơm");

        ArrayNode safe = validator.safeCandidates(input, mapper.createArrayNode(), available);

        // All 3 recipes must be retained
        assertEquals(3, safe.size());
        // Recipe 1 matches 2 items -> 1st
        assertEquals(1L, safe.get(0).path("id").asLong());
        // Recipe 2 matches 1 item -> 2nd
        assertEquals(2L, safe.get(1).path("id").asLong());
        // Recipe 3 matches 0 items -> 3rd
        assertEquals(3L, safe.get(2).path("id").asLong());
    }

    @Test
    void doesNotFailWhenZeroRecipesMatchAvailableIngredients() {
        ObjectNode recipe1 = recipeWithIngredients(1, "Món đậu", List.of("Đậu hũ"));
        ArrayNode input = mapper.createArrayNode().add(recipe1);

        // Available ingredient is not in any recipe
        List<String> available = List.of("Khoai tây");

        ArrayNode safe = validator.safeCandidates(input, mapper.createArrayNode(), available);
        assertEquals(1, safe.size());
        assertEquals(1L, safe.get(0).path("id").asLong());
    }

    @Test
    void ensuresExactly21MealsAndPreventsSameDayDuplicatesWhenRecipesAvailable() {
        ObjectNode r1 = recipeWithIngredients(1, "Món 1", List.of("Đậu hũ"));
        ObjectNode r2 = recipeWithIngredients(2, "Món 2", List.of("Cà rốt"));
        ObjectNode r3 = recipeWithIngredients(3, "Món 3", List.of("Nấm rơm"));
        ArrayNode recipes = mapper.createArrayNode().add(r1).add(r2).add(r3);

        // Input plan has duplicate recipeId=1 for all 3 meals each day
        ObjectNode plan = sevenDayPlan(1L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), null, true);

        ArrayNode days = (ArrayNode) plan.path("days");
        assertEquals(7, days.size());

        int totalMeals = 0;
        for (int d = 0; d < 7; d++) {
            ArrayNode meals = (ArrayNode) days.get(d).path("meals");
            assertEquals(3, meals.size());
            totalMeals += meals.size();

            // Distinct meals per day
            long m1 = meals.get(0).path("recipeId").asLong();
            long m2 = meals.get(1).path("recipeId").asLong();
            long m3 = meals.get(2).path("recipeId").asLong();
            assertEquals(3, java.util.Set.of(m1, m2, m3).size(), "Day " + d + " must not repeat recipes");
        }
        assertEquals(21, totalMeals);
    }

    @Test
    void handlesSingleSafeRecipeAcrossAll21Meals() {
        ObjectNode r1 = recipeWithIngredients(1, "Món duy nhất", List.of("Đậu hũ"));
        ArrayNode recipes = mapper.createArrayNode().add(r1);

        ObjectNode plan = mapper.createObjectNode();
        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), null, true);

        ArrayNode days = (ArrayNode) plan.path("days");
        assertEquals(7, days.size());
        for (int d = 0; d < 7; d++) {
            ArrayNode meals = (ArrayNode) days.get(d).path("meals");
            assertEquals(3, meals.size());
            for (int m = 0; m < 3; m++) {
                assertEquals(1L, meals.get(m).path("recipeId").asLong());
            }
        }
    }

    @Test
    void repairsHallucinatedAndInvalidRecipeIds() {
        ObjectNode r1 = recipeWithIngredients(10, "Món thật 1", List.of("Đậu hũ"));
        ObjectNode r2 = recipeWithIngredients(20, "Món thật 2", List.of("Cà rốt"));
        ObjectNode r3 = recipeWithIngredients(30, "Món thật 3", List.of("Nấm rơm"));
        ArrayNode recipes = mapper.createArrayNode().add(r1).add(r2).add(r3);

        // Plan has hallucinated ID 9999
        ObjectNode plan = sevenDayPlan(9999L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), null, true);

        ArrayNode days = (ArrayNode) plan.path("days");
        for (int d = 0; d < 7; d++) {
            ArrayNode meals = (ArrayNode) days.get(d).path("meals");
            for (int m = 0; m < 3; m++) {
                long id = meals.get(m).path("recipeId").asLong();
                assertEquals(true, java.util.Set.of(10L, 20L, 30L).contains(id),
                        "Hallucinated ID 9999 must be replaced by valid ID");
            }
        }
    }

    @Test
    void appliesPortionScalingToMeetCalorieTarget() {
        // Recipe has 76 kcal per serving. 3 meals = 228 kcal.
        ObjectNode r1 = recipe(false); // 76 kcal
        ArrayNode recipes = mapper.createArrayNode().add(r1);

        ObjectNode plan = sevenDayPlan(1L);

        // Target: min 300 kcal, max 350 kcal. (Each meal is scaled on its own toward the target: 76 kcal -> 81.32, 129.96, 114)
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("300"), new BigDecimal("350"),
                null, null, null, null, null, null
        );

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), target, true);

        BigDecimal day1Calories = plan.path("days").get(0).path("nutritionTotals").path("calories").decimalValue();
        // Per meal: breakfast 81.25/76 -> 1.07 (81.32), lunch -> 1.71 (129.96), dinner -> 1.50 (114); day 325.28, inside [300, 350]
        assertEquals(true, day1Calories.compareTo(new BigDecimal("300")) >= 0);
        assertEquals(true, day1Calories.compareTo(new BigDecimal("350")) <= 0);
        assertEquals(true, plan.path("days").get(0).path("meals").get(0).hasNonNull("servingMultiplier"));
    }

    @Test
    void flagsDaysThatMissTheTargetInsteadOfRejectingThePlan() {
        // Recipe has 76 kcal per serving. Each meal is scaled on its own, at most 2.00x -> 152 kcal, 456 kcal a day.
        // Target: 1000-1200 kcal (impossible to reach).
        ObjectNode r1 = recipe(false);
        ArrayNode recipes = mapper.createArrayNode().add(r1);

        ObjectNode plan = sevenDayPlan(1L);

        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("1000"), new BigDecimal("1200"),
                null, null, null, null, null, null
        );

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), target, true);

        assertEquals(7, plan.path("days").size());
        var day = plan.path("days").get(0);
        assertEquals(false, day.path("targetMet").asBoolean(true));
        var calories = day.path("targetDeviations").path("calories");
        assertEquals(new BigDecimal("456.00"), calories.path("actual").decimalValue());
        assertEquals(0, new BigDecimal("1000").compareTo(calories.path("min").decimalValue()));
        assertEquals(0, new BigDecimal("1200").compareTo(calories.path("max").decimalValue()));
        assertEquals(false, day.path("targetDeviations").has("protein"), "protein has no target, so it cannot deviate");
    }

    @Test
    void marksDaysThatMeetTheTarget() {
        ArrayNode recipes = mapper.createArrayNode().add(recipe(false));
        ObjectNode plan = sevenDayPlan(1L);
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("300"), new BigDecimal("350"),
                null, null, null, null, null, null
        );

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), target, true);

        var day = plan.path("days").get(0);
        assertEquals(true, day.path("targetMet").asBoolean(false));
        assertEquals(false, day.has("targetDeviations"));
    }

    @Test
    void targetMetLooksOnlyAtCaloriesButStillReportsProtein() {
        ObjectNode day = mapper.createObjectNode();
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("2522"), new BigDecimal("3082"),
                new BigDecimal("96"), new BigDecimal("144"),
                null, null, null, null);

        validator.markTarget(day, new MealPlanValidator.Macro(
                new BigDecimal("2800"), new BigDecimal("50"), new BigDecimal("300"), new BigDecimal("80")), target);

        assertEquals(true, day.path("targetMet").asBoolean(false));
        assertEquals(false, day.path("targetDeviations").has("calories"));
        assertEquals(0, new BigDecimal("50.00").compareTo(
                day.path("targetDeviations").path("protein").path("actual").decimalValue()));
    }

    @Test
    void caloriesOutsideTheRangeMissTheTargetEvenWithinTheOldTolerance() {
        ObjectNode day = mapper.createObjectNode();
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("2522"), new BigDecimal("3082"), null, null, null, null, null, null);

        // 2400 used to pass: the old rule accepted down to 2522 x 0.85 = 2143.7
        validator.markTarget(day, new MealPlanValidator.Macro(
                new BigDecimal("2400"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), target);

        assertEquals(false, day.path("targetMet").asBoolean(true));
        assertEquals(0, new BigDecimal("2400.00").compareTo(
                day.path("targetDeviations").path("calories").path("actual").decimalValue()));
    }

    @Test
    void reachesTheDailyTargetWhenTheAiPicksLightRecipes() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = sevenDayPlan(1L); // the AI picked the 120 kcal recipe for every meal

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        assertEveryDayMeetsTarget(plan, TARGET_2522_3082);
        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) {
                long id = meal.path("recipeId").asLong();
                assertEquals(false, id == 1L || id == 2L, "recipes too light for any budget must not be picked");
                if (meal.has("servingMultiplier")) {
                    BigDecimal scale = meal.path("servingMultiplier").decimalValue();
                    assertEquals(true, scale.compareTo(new BigDecimal("0.75")) >= 0 && scale.compareTo(new BigDecimal("2.00")) <= 0);
                }
            }
        }
    }

    @Test
    void aSwappedMealLosesTheNoteWrittenForTheRecipeItReplaced() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = withNotes(sevenDayPlan(1L)); // recipe 1 cannot reach any budget, so every meal is swapped

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) {
                assertEquals("Bữa ăn cân bằng dinh dưỡng", meal.path("note").asText());
            }
        }
    }

    @Test
    void aKeptMealKeepsItsNote() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = withNotes(planOf(3L, 4L, 5L));

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) {
                assertEquals("Ghi chú của AI", meal.path("note").asText());
            }
        }
    }

    @Test
    void displayedNutritionEqualsBaseTimesDisplayedMultiplier() {
        Map<Long, BigDecimal> kcal = Map.of(1L, new BigDecimal("120"), 2L, new BigDecimal("300"),
                3L, new BigDecimal("500"), 4L, new BigDecimal("700"), 5L, new BigDecimal("650"));
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = sevenDayPlan(1L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) {
                BigDecimal multiplier = meal.has("servingMultiplier") ? meal.path("servingMultiplier").decimalValue() : BigDecimal.ONE;
                BigDecimal expected = kcal.get(meal.path("recipeId").asLong()).multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
                assertEquals(0, expected.compareTo(meal.path("nutrition").path("calories").decimalValue()));
            }
        }
    }

    @Test
    void keepsAiPicksThatReachTheirBudget() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = planOf(3L, 4L, 5L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        for (var day : plan.path("days")) {
            assertEquals(3L, day.path("meals").get(0).path("recipeId").asLong());
            assertEquals(4L, day.path("meals").get(1).path("recipeId").asLong());
            assertEquals(5L, day.path("meals").get(2).path("recipeId").asLong());
        }
        assertEveryDayMeetsTarget(plan, TARGET_2522_3082);
    }

    @Test
    void laterMealsAbsorbAShortBreakfast() {
        // Breakfast is kept at 100 kcal x 2.00 = 200 (repick = false); lunch and dinner must carry the rest.
        // With fixed shares (lunch 1120.8, dinner 980.7) the day would end near 2300, below 2522.
        ArrayNode recipes = recipesWithKcal(1, 100, 4, 700, 5, 650, 6, 800, 7, 900);
        ObjectNode plan = planOf(1L, null, null);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, false);

        for (var day : plan.path("days")) {
            var breakfast = day.path("meals").get(0);
            assertEquals(1L, breakfast.path("recipeId").asLong());
            assertEquals(0, new BigDecimal("2.00").compareTo(breakfast.path("servingMultiplier").decimalValue()));
        }
        assertEveryDayMeetsTarget(plan, TARGET_2522_3082);
    }

    @Test
    void repickFalseKeepsSafeMealsThatMissTheirBudget() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 130, 3, 140, 4, 700, 5, 650, 6, 800);

        ObjectNode kept = planOf(1L, 2L, 3L);
        validator.validateAndEnrich(kept, recipes, mapper.createArrayNode(), TARGET_2522_3082, false);
        for (var day : kept.path("days")) {
            assertEquals(Set.of(1L, 2L, 3L), Set.of(
                    day.path("meals").get(0).path("recipeId").asLong(),
                    day.path("meals").get(1).path("recipeId").asLong(),
                    day.path("meals").get(2).path("recipeId").asLong()));
        }

        ObjectNode repicked = planOf(1L, 2L, 3L);
        validator.validateAndEnrich(repicked, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);
        assertEquals(false, repicked.path("days").get(0).path("meals").get(1).path("recipeId").asLong() == 2L,
                "with repick = true an unreachable lunch is replaced");
    }

    @Test
    void singleEndedTargetsAimInsideTheBound() {
        // Aiming exactly at the bound lets the 2-decimal rounding of a multiplier land the day just under it.
        ArrayNode recipes = recipesWithKcal(1, 100, 4, 700, 5, 650, 6, 800, 7, 900);
        ObjectNode plan = planOf(1L, null, null);
        MealPlanDtos.NutritionTarget minOnly = new MealPlanDtos.NutritionTarget(
                new BigDecimal("2802"), null, null, null, null, null, null, null);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), minOnly, false);

        for (var day : plan.path("days")) {
            BigDecimal calories = day.path("nutritionTotals").path("calories").decimalValue();
            assertEquals(true, calories.compareTo(minOnly.minCalories()) >= 0, day.path("day").asText() + " = " + calories);
            assertEquals(true, day.path("targetMet").asBoolean(false), day.path("day").asText() + " = " + calories);
        }
    }

    @Test
    void mealBudgetsSplitTheMidpointBetweenMeals() {
        var budgets = validator.mealBudgets(new MealPlanDtos.NutritionTarget(
                new BigDecimal("2400"), new BigDecimal("2600"), null, null, null, null, null, null));
        assertEquals(List.of("BREAKFAST", "LUNCH", "DINNER"), List.copyOf(budgets.keySet()));
        assertEquals(new BigDecimal("625.00"), budgets.get("BREAKFAST"));
        assertEquals(new BigDecimal("1000.00"), budgets.get("LUNCH"));
        assertEquals(new BigDecimal("875.00"), budgets.get("DINNER"));

        // A single-ended target aims 5% inside the bound: 2000 -> 2100, lunch 40% = 840.
        var minOnly = validator.mealBudgets(new MealPlanDtos.NutritionTarget(
                new BigDecimal("2000"), null, null, null, null, null, null, null));
        assertEquals(new BigDecimal("840.00"), minOnly.get("LUNCH"));

        assertEquals(true, validator.mealBudgets(null).isEmpty());
    }

    @Test
    void mealBudgetHintNamesEveryMealAndThePortionRange() {
        String hint = validator.mealBudgetHint(new MealPlanDtos.NutritionTarget(
                new BigDecimal("2400"), new BigDecimal("2600"), null, null, null, null, null, null));
        assertEquals(true, hint.contains("BREAKFAST khoảng 625 kcal, LUNCH khoảng 1000 kcal, DINNER khoảng 875 kcal"), hint);
        assertEquals(true, hint.contains("từ 0,75 đến 2 lần"), hint);
        assertEquals("", validator.mealBudgetHint(null));
    }

    @Test
    void reachesBudgetUsesThePortionRange() {
        assertEquals(true, validator.reachesBudget(recipeWithKcal(4, 700), new BigDecimal("1120.80")));
        assertEquals(false, validator.reachesBudget(recipeWithKcal(1, 120), new BigDecimal("1120.80")));
        assertEquals(true, validator.reachesBudget(recipeWithKcal(1, 120), null));
    }

    @Test
    void pickReplacementSkipsExcludedRecipesAndPrefersReachableOnes() {
        ArrayNode candidates = recipesWithKcal(1, 120, 3, 500, 4, 700, 5, 650, 6, 800);
        assertEquals(6L, validator.pickReplacement(candidates, Set.of(3L, 4L, 5L), new BigDecimal("1120.80")));

        AppException ex = assertThrows(AppException.class,
                () -> validator.pickReplacement(candidates, Set.of(1L, 3L, 4L, 5L, 6L), new BigDecimal("1120.80")));
        assertEquals(com.veggiepal.ai.common.ErrorCode.NO_VALID_RECIPES, ex.getErrorCode());
    }

    private static final MealPlanDtos.NutritionTarget TARGET_2522_3082 = new MealPlanDtos.NutritionTarget(
            new BigDecimal("2522"), new BigDecimal("3082"), null, null, null, null, null, null);

    // One ingredient of 100 g per serving, so the recipe's kcal per serving equals kcal.
    private ObjectNode recipeWithKcal(long id, int kcal) {
        ObjectNode recipe = mapper.createObjectNode();
        recipe.put("id", id).put("title", "Món " + id).put("servings", 1);
        recipe.putArray("ingredients").addObject()
                .put("ingredientName", "Nguyên liệu " + id).put("normalizedName", "nguyen lieu " + id)
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", kcal).put("proteinPer100g", 10)
                .put("carbsPer100g", 20).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);
        return recipe;
    }

    // idKcal = {id1, kcal1, id2, kcal2, ...}
    private ArrayNode recipesWithKcal(int... idKcal) {
        ArrayNode recipes = mapper.createArrayNode();
        for (int i = 0; i < idKcal.length; i += 2) recipes.add(recipeWithKcal(idKcal[i], idKcal[i + 1]));
        return recipes;
    }

    // null leaves that meal out of the plan
    private ObjectNode planOf(Long breakfast, Long lunch, Long dinner) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode days = root.putArray("days");
        for (String dayName : MealPlanValidator.STANDARD_DAYS) {
            ArrayNode meals = days.addObject().put("day", dayName).putArray("meals");
            if (breakfast != null) meals.addObject().put("mealType", "BREAKFAST").put("recipeId", breakfast);
            if (lunch != null) meals.addObject().put("mealType", "LUNCH").put("recipeId", lunch);
            if (dinner != null) meals.addObject().put("mealType", "DINNER").put("recipeId", dinner);
        }
        return root;
    }

    private ObjectNode withNotes(ObjectNode plan) {
        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) ((ObjectNode) meal).put("note", "Ghi chú của AI");
        }
        return plan;
    }

    private void assertEveryDayMeetsTarget(ObjectNode plan, MealPlanDtos.NutritionTarget target) {
        for (var day : plan.path("days")) {
            BigDecimal calories = day.path("nutritionTotals").path("calories").decimalValue();
            String where = day.path("day").asText() + " = " + calories;
            assertEquals(true, calories.compareTo(target.minCalories()) >= 0, where);
            assertEquals(true, calories.compareTo(target.maxCalories()) <= 0, where);
            assertEquals(true, day.path("targetMet").asBoolean(false), where);
        }
    }

    private ObjectNode recipeWithIngredients(long id, String title, List<String> ingNames) {
        ObjectNode recipe = mapper.createObjectNode();
        recipe.put("id", id).put("title", title).put("servings", 1);
        ArrayNode ingredients = recipe.putArray("ingredients");
        for (String name : ingNames) {
            ingredients.addObject()
                    .put("ingredientName", name).put("normalizedName", name.toLowerCase(java.util.Locale.ROOT))
                    .put("quantity", 100).put("unit", "GRAM")
                    .put("caloriesPer100g", 50).put("proteinPer100g", 3)
                    .put("carbsPer100g", 5).put("fatPer100g", 1)
                    .put("vegan", true).put("allergen", false);
        }
        return recipe;
    }

    private ObjectNode recipe(boolean allergen) {
        ObjectNode recipe = mapper.createObjectNode();
        recipe.put("id", 1).put("title", "Đậu hũ").put("servings", 2);
        recipe.putArray("ingredients").addObject()
                .put("ingredientName", "Đậu hũ").put("normalizedName", "dau hu")
                .put("quantity", 200).put("unit", "GRAM")
                .put("caloriesPer100g", 76).put("proteinPer100g", 8)
                .put("carbsPer100g", 2).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", allergen);
        return recipe;
    }

    private ObjectNode sevenDayPlan(long recipeId) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode days = root.putArray("days");
        for (int day = 1; day <= 7; day++) {
            ObjectNode item = days.addObject().put("day", "DAY_" + day);
            ArrayNode meals = item.putArray("meals");
            meals.addObject().put("mealType", "BREAKFAST").put("recipeId", recipeId);
            meals.addObject().put("mealType", "LUNCH").put("recipeId", recipeId);
            meals.addObject().put("mealType", "DINNER").put("recipeId", recipeId);
        }
        return root;
    }
}
