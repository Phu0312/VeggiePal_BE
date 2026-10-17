package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MealPlanValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MealPlanValidator validator = new MealPlanValidator(mapper);

    @Test
    void calculatesMacrosDeterministicallyForSevenDayPlan() {
        ArrayNode recipes = mapper.createArrayNode().add(recipe(false));
        ObjectNode plan = sevenDayPlan(1L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), null);

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
