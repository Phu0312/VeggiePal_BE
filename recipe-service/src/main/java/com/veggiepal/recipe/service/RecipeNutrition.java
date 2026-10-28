package com.veggiepal.recipe.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.veggiepal.recipe.dto.response.NutritionPerServingResponse;
import com.veggiepal.recipe.entity.Ingredient;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeIngredient;

/** task_be C16: per-serving macros, so the FE no longer converts spoons, pieces and ml itself. */
public final class RecipeNutrition {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private RecipeNutrition() {
    }

    public static NutritionPerServingResponse perServing(Recipe recipe) {

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        List<String> missing = new ArrayList<>();

        for (RecipeIngredient row : recipe.getIngredients()) {
            Ingredient ingredient = row.getIngredient();
            Optional<BigDecimal> grams = IngredientUnitConverter.toGrams(
                    row.getQuantity(), row.getUnit(), ingredient.getName(), ingredient.getNormalizedName());

            if (grams.isEmpty()) {
                missing.add(ingredient.getName());
                continue;
            }

            BigDecimal factor = grams.get().divide(HUNDRED, 6, RoundingMode.HALF_UP);
            calories = calories.add(orZero(ingredient.getCaloriesPer100g()).multiply(factor));
            protein = protein.add(orZero(ingredient.getProteinPer100g()).multiply(factor));
            carbs = carbs.add(orZero(ingredient.getCarbsPer100g()).multiply(factor));
            fat = fat.add(orZero(ingredient.getFatPer100g()).multiply(factor));
        }

        BigDecimal servings = BigDecimal.valueOf(
                recipe.getServings() == null || recipe.getServings() < 1 ? 1 : recipe.getServings());

        return NutritionPerServingResponse.builder()
                .calories(perServing(calories, servings))
                .protein(perServing(protein, servings))
                .carbs(perServing(carbs, servings))
                .fat(perServing(fat, servings))
                .complete(missing.isEmpty())
                .missingIngredients(missing)
                .build();
    }

    private static BigDecimal perServing(BigDecimal total, BigDecimal servings) {
        return total.divide(servings, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
