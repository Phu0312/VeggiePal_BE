package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.repository.RecipeRepository;

// Real schema (H2, MySQL mode): the new table, the new column and the JPQL update/query must all work
@SpringBootTest
@Transactional
class RecipeFavoriteIntegrationTest {

    @Autowired RecipeFavoriteService favorites;
    @Autowired RecipeService recipeService;
    @Autowired RecipeRepository recipes;

    @Test
    void aLovedRecipe_leadsThePopularListAndShowsInTheCollection() {
        Recipe loved = recipes.saveAndFlush(Recipe.builder().userId(987654L).title("Loved IT")
                .servings(1).status(RecipeStatus.PUBLISHED).build());

        favorites.add(11L, loved.getId());
        favorites.add(12L, loved.getId());
        recipes.flush();

        var popular = recipeService.getPublishedRecipes(null, "popular", 0, 5);
        assertThat(popular.getItems().get(0).getId()).isEqualTo(loved.getId());
        assertThat(popular.getItems().get(0).getFavoriteCount()).isEqualTo(2);

        assertThat(favorites.myFavorites(11L, 0, 20).getItems())
                .extracting("id").containsExactly(loved.getId());
        assertThat(favorites.myFavoriteIds(12L)).containsExactly(loved.getId());

        favorites.remove(11L, loved.getId());
        assertThat(favorites.myFavoriteIds(11L)).isEmpty();
    }

    @Test
    void favoriteIds_includeOnlyPublishedRecipes() {
        Recipe visible = recipes.saveAndFlush(Recipe.builder().userId(987654L).title("Visible IT")
                .servings(1).status(RecipeStatus.PUBLISHED).build());
        Recipe hidden = recipes.saveAndFlush(Recipe.builder().userId(987654L).title("Hidden IT")
                .servings(1).status(RecipeStatus.PUBLISHED).build());
        favorites.add(33L, visible.getId());
        favorites.add(33L, hidden.getId());
        hidden.setStatus(RecipeStatus.HIDDEN);
        recipes.saveAndFlush(hidden);

        assertThat(favorites.myFavoriteIds(33L)).containsExactly(visible.getId());
        assertThat(favorites.myFavorites(33L, 0, 20).getItems())
                .extracting("id").containsExactly(visible.getId());
    }
}
