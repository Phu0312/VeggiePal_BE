package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.veggiepal.recipe.dto.response.NutritionPerServingResponse;
import com.veggiepal.recipe.entity.Ingredient;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeIngredient;

class RecipeNutritionTest {

    static Ingredient ingredient(String name, String normalized, String kcal, String protein, String carbs, String fat) {
        return Ingredient.builder()
                .name(name).normalizedName(normalized)
                .caloriesPer100g(new BigDecimal(kcal)).proteinPer100g(new BigDecimal(protein))
                .carbsPer100g(new BigDecimal(carbs)).fatPer100g(new BigDecimal(fat))
                .build();
    }

    static Recipe recipe(Integer servings, RecipeIngredient... rows) {
        Recipe recipe = Recipe.builder().title("t").servings(servings).build();
        for (RecipeIngredient row : rows) {
            recipe.addIngredient(row);
        }
        return recipe;
    }

    static RecipeIngredient row(Ingredient ingredient, String quantity, String unit) {
        return RecipeIngredient.builder().ingredient(ingredient).quantity(new BigDecimal(quantity)).unit(unit).build();
    }

    @Test
    void grams_areScaledPer100gAndSplitAcrossServings() {
        Ingredient tofu = ingredient("Đậu hũ", "dau hu", "80", "8", "2", "4.8");

        NutritionPerServingResponse n = RecipeNutrition.perServing(recipe(2, row(tofu, "200", "GRAM")));

        assertThat(n.getCalories()).isEqualByComparingTo("80.0");
        assertThat(n.getProtein()).isEqualByComparingTo("8.0");
        assertThat(n.getCarbs()).isEqualByComparingTo("2.0");
        assertThat(n.getFat()).isEqualByComparingTo("4.8");
        assertThat(n.getComplete()).isTrue();
        assertThat(n.getMissingIngredients()).isEmpty();
    }

    // The converter's piece table gives a banana 118 g
    @Test
    void pieces_areConvertedThroughThePieceTable() {
        Ingredient banana = ingredient("Chuối", "chuoi", "89", "1.1", "22.8", "0.3");

        NutritionPerServingResponse n = RecipeNutrition.perServing(recipe(1, row(banana, "1", "quả")));

        assertThat(n.getCalories()).isEqualByComparingTo("105.0");
        assertThat(n.getComplete()).isTrue();
    }

    @Test
    void anUnconvertibleRow_isLeftOutAndNamed() {
        Ingredient tofu = ingredient("Đậu hũ", "dau hu", "80", "8", "2", "4.8");
        Ingredient jackfruit = ingredient("Mít non", "mit non", "95", "1.7", "23", "0.6");

        NutritionPerServingResponse n = RecipeNutrition.perServing(
                recipe(1, row(tofu, "100", "GRAM"), row(jackfruit, "1", "quả")));

        assertThat(n.getCalories()).isEqualByComparingTo("80.0");
        assertThat(n.getComplete()).isFalse();
        assertThat(n.getMissingIngredients()).containsExactly("Mít non");
    }

    @Test
    void missingOrZeroServings_countAsOne() {
        Ingredient tofu = ingredient("Đậu hũ", "dau hu", "80", "8", "2", "4.8");

        assertThat(RecipeNutrition.perServing(recipe(null, row(tofu, "100", "GRAM"))).getCalories())
                .isEqualByComparingTo("80.0");
        assertThat(RecipeNutrition.perServing(recipe(0, row(tofu, "100", "GRAM"))).getCalories())
                .isEqualByComparingTo("80.0");
    }
}
