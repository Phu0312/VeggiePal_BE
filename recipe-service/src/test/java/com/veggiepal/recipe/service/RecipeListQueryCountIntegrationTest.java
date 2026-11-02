package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManagerFactory;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.default_batch_fetch_size=25",
        "spring.jpa.show-sql=false"
})
class RecipeListQueryCountIntegrationTest {
    @Autowired RecipeService recipeService;
    @Autowired RecipeFavoriteService favoriteService;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void publishedListStatementCountDoesNotGrowWithEachRecipe() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        assertThat(recipeService.getPublishedRecipes(null, null, 0, 1).getItems()).hasSize(1);
        long oneRecipeStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        assertThat(recipeService.getPublishedRecipes(null, null, 0, 10).getItems()).hasSize(10);
        long tenRecipeStatements = statistics.getPrepareStatementCount();

        assertThat(tenRecipeStatements).isLessThanOrEqualTo(oneRecipeStatements + 5);
        System.out.printf("Published recipe list SQL: 1=%d, 10=%d%n", oneRecipeStatements, tenRecipeStatements);
    }

    @Test
    void myListStatementCountDoesNotGrowWithEachRecipe() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(recipeService.getMyRecipes(1L, 0, 1).getItems()).hasSize(1);
        long one = statistics.getPrepareStatementCount();
        statistics.clear();
        assertThat(recipeService.getMyRecipes(1L, 0, 10).getItems()).hasSize(10);
        long ten = statistics.getPrepareStatementCount();
        assertThat(ten).isLessThanOrEqualTo(one + 5);
        System.out.printf("My recipe list SQL: 1=%d, 10=%d%n", one, ten);
    }

    @Test
    void favoriteListStatementCountDoesNotGrowWithEachRecipe() {
        long userId = 998877L;
        var recipeIds = recipeService.getPublishedRecipes(null, null, 0, 10).getItems().stream()
                .map(recipe -> recipe.getId()).toList();
        recipeIds.forEach(id -> favoriteService.add(userId, id));
        try {
            Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            statistics.clear();
            assertThat(favoriteService.myFavorites(userId, 0, 1).getItems()).hasSize(1);
            long one = statistics.getPrepareStatementCount();
            statistics.clear();
            assertThat(favoriteService.myFavorites(userId, 0, 10).getItems()).hasSize(10);
            long ten = statistics.getPrepareStatementCount();
            assertThat(ten).isLessThanOrEqualTo(one + 5);
            System.out.printf("Favorite recipe list SQL: 1=%d, 10=%d%n", one, ten);
        } finally {
            recipeIds.forEach(id -> favoriteService.remove(userId, id));
        }
    }
}
