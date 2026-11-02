package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.repository.RecipeFavoriteRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

@SpringBootTest
class RecipeFavoriteConcurrencyIntegrationTest {
    @Autowired RecipeFavoriteService service;
    @Autowired RecipeFavoriteRepository favoriteRepository;
    @Autowired RecipeRepository recipeRepository;

    @Test
    void simultaneousAdds_leaveOneFavoriteAndOneCount() throws Exception {
        Recipe recipe = recipeRepository.saveAndFlush(Recipe.builder().userId(999L)
                .title("Concurrent IT " + UUID.randomUUID()).servings(1)
                .status(RecipeStatus.PUBLISHED).build());
        long userId = 99887766L;
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = executor.submit(() -> {
                start.await();
                return service.add(userId, recipe.getId());
            });
            var second = executor.submit(() -> {
                start.await();
                return service.add(userId, recipe.getId());
            });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).getFavorited()).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS).getFavorited()).isTrue();
            assertThat(favoriteRepository.findByUserIdAndRecipeId(userId, recipe.getId())).isPresent();
            assertThat(recipeRepository.findById(recipe.getId()).orElseThrow().getFavoriteCount()).isEqualTo(1);
        } finally {
            favoriteRepository.findByUserIdAndRecipeId(userId, recipe.getId()).ifPresent(favoriteRepository::delete);
            recipeRepository.deleteById(recipe.getId());
        }
    }
}
