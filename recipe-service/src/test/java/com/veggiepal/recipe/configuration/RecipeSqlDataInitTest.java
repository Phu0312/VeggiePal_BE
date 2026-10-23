package com.veggiepal.recipe.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
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

        // 2 sample recipes + 100 Phase 2 recipes + 13 recipes from the 2026-10 nutrition audit = 115 recipes
        assertEquals(115, recipeCount, "Recipe catalog must contain exactly 115 recipes");
        // 8 sample ingredients + 61 additional + 18 from the 2026-10 nutrition audit = 87 ingredients
        assertEquals(87, ingredientCount, "Ingredient catalog must contain exactly 87 ingredients");

        // A normalized_name typo makes an INSERT ... SELECT skip its row without an error; these counts catch it
        assertEquals(508, countRows("recipe_ingredients"), "recipe_ingredients must contain exactly 508 rows");
        assertEquals(363, countRows("recipe_steps"), "recipe_steps must contain exactly 363 rows");

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
        long initialRecipeIngredients = countRows("recipe_ingredients");
        long initialRecipeSteps = countRows("recipe_steps");

        // Re-execute data.sql a second time to simulate application restart
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }

        long reloadedRecipes = recipeRepository.count();
        long reloadedIngredients = ingredientRepository.count();

        assertEquals(initialRecipes, reloadedRecipes, "Recipe count must remain unchanged after re-running data.sql");
        assertEquals(initialIngredients, reloadedIngredients, "Ingredient count must remain unchanged after re-running data.sql");
        assertEquals(508, countRows("recipe_ingredients"), "recipe_ingredients must still contain exactly 508 rows after re-running data.sql");
        assertEquals(363, countRows("recipe_steps"), "recipe_steps must still contain exactly 363 rows after re-running data.sql");
        assertEquals(initialRecipeIngredients, countRows("recipe_ingredients"), "recipe_ingredients count must remain unchanged after re-running data.sql");
        assertEquals(initialRecipeSteps, countRows("recipe_steps"), "recipe_steps count must remain unchanged after re-running data.sql");
    }

    private long countRows(String table) {
        return new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
