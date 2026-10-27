package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Runs the planner on the real seed catalogue (recipes-seed.json, exported from recipe-service's
// GET /recipes?page=0&size=100). Re-export it whenever recipe-service's data.sql changes — see
// docs/superpowers/plans/2026-10-21-recipe-nutrition-data.md, Task 6 Step 9.
class MealPlanSeedAcceptanceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MealPlanValidator validator = new MealPlanValidator(mapper);

    @ParameterizedTest(name = "{0}-{1} kcal")
    @CsvSource({"1350,1650", "1800,2200", "2250,2750", "2700,3300", "2522,3082"})
    void everyDayMeetsTheCalorieTargetOnTheSeedCatalogue(int min, int max) throws Exception {
        JsonNode recipes;
        try (InputStream in = getClass().getResourceAsStream("/mealplan/recipes-seed.json")) {
            recipes = mapper.readTree(in);
        }
        ObjectNode plan = mapper.createObjectNode(); // what generate() validates when the AI call fails
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), null, null, null, null, null, null);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), target, true);

        assertEquals(7, plan.path("days").size());
        for (JsonNode day : plan.path("days")) {
            assertTrue(day.path("targetMet").asBoolean(false),
                    day.path("day").asText() + " totals " + day.path("nutritionTotals"));
        }
    }
}
