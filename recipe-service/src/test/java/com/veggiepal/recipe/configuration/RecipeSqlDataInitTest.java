package com.veggiepal.recipe.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.recipe.repository.IngredientRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

@SpringBootTest
class RecipeSqlDataInitTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @Transactional(readOnly = true)
    void dataSqlSeedsExpectedRecipesAndIngredients() {
        long recipeCount = recipeRepository.count();
        long ingredientCount = ingredientRepository.count();

        // 2 sample recipes + 100 Phase 2 recipes = 102 recipes
        assertEquals(102, recipeCount, "Recipe catalog must contain exactly 102 recipes");
        // 8 sample ingredients + 61 additional = 69 ingredients
        assertEquals(69, ingredientCount, "Ingredient catalog must contain exactly 69 ingredients");

        // Verify each recipe has ingredients and steps
        recipeRepository.findAll().forEach(r -> {
            assertTrue(!r.getIngredients().isEmpty(), "Recipe '" + r.getTitle() + "' must have ingredients");
            assertTrue(!r.getSteps().isEmpty(), "Recipe '" + r.getTitle() + "' must have steps");
        });
    }

    @Test
    void dataSqlIsIdempotentWhenExecutedMultipleTimes() throws Exception {
        long initialRecipes = recipeRepository.count();
        long initialIngredients = ingredientRepository.count();

        // Re-execute data.sql a second time to simulate application restart
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }

        long reloadedRecipes = recipeRepository.count();
        long reloadedIngredients = ingredientRepository.count();

        assertEquals(initialRecipes, reloadedRecipes, "Recipe count must remain unchanged after re-running data.sql");
        assertEquals(initialIngredients, reloadedIngredients, "Ingredient count must remain unchanged after re-running data.sql");
    }
}
