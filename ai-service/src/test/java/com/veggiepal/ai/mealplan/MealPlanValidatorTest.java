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
