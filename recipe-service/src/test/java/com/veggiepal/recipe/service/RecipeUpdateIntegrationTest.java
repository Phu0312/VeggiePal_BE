package com.veggiepal.recipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.veggiepal.recipe.dto.request.RecipeIngredientRequest;
import com.veggiepal.recipe.dto.request.RecipeRequest;
import com.veggiepal.recipe.dto.request.RecipeStepRequest;
import com.veggiepal.recipe.entity.Ingredient;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeIngredient;
import com.veggiepal.recipe.entity.RecipeStep;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.repository.IngredientRepository;
import com.veggiepal.recipe.repository.RecipeRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecipeUpdateIntegrationTest {
    @Autowired RecipeService service;
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;

    @Test
    void putCanReuseTheSameIngredientAndStepUniqueKeys() {
        Ingredient ingredient = ingredients.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .findFirst().orElseThrow();
        Recipe recipe = Recipe.builder().userId(987654L).title("Update regression")
                .servings(2).status(RecipeStatus.DRAFT).build();
        recipe.addIngredient(RecipeIngredient.builder().ingredient(ingredient)
                .quantity(BigDecimal.ONE).unit("G").build());
        recipe.addStep(RecipeStep.builder().stepNumber(1).instruction("Old step").build());
        Recipe saved = recipes.saveAndFlush(recipe);

        RecipeRequest replacement = RecipeRequest.builder().title("Updated regression")
                .servings(2)
                .ingredients(List.of(RecipeIngredientRequest.builder().ingredientId(ingredient.getId())
                        .quantity(BigDecimal.TEN).unit("G").build()))
                .steps(List.of(RecipeStepRequest.builder().stepNumber(1)
                        .instruction("Replacement step").build()))
                .build();

        service.updateRecipe(saved.getId(), 987654L, false, replacement);
        recipes.flush();

        Recipe updated = recipes.findById(saved.getId()).orElseThrow();
        assertEquals(1, updated.getIngredients().size());
        assertEquals(1, updated.getSteps().size());
        assertEquals("Replacement step", updated.getSteps().getFirst().getInstruction());
    }
}
