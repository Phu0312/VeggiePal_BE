package com.veggiepal.recipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.IngredientRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

class RecipeServiceTest {

    private RecipeRepository recipeRepository;
    private IngredientRepository ingredientRepository;
    private RecipeMapper recipeMapper;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        ingredientRepository = mock(IngredientRepository.class);
        recipeMapper = mock(RecipeMapper.class);
        recipeService = new RecipeService(recipeRepository, ingredientRepository, recipeMapper);
    }

    @Test
    void deleteRecipe_softDeletesWithArchivedStatus() {
        Recipe recipe = Recipe.builder()
                .id(1L)
                .userId(100L)
                .title("Canh chua chay")
                .status(RecipeStatus.PUBLISHED)
                .build();

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recipeService.deleteRecipe(1L, 100L, false);

        assertEquals(RecipeStatus.ARCHIVED, recipe.getStatus());
        verify(recipeRepository).save(recipe);
        verify(recipeRepository, never()).delete(any(Recipe.class));
    }

    @Test
    void getPublishedRecipes_queriesOnlyPublishedStatus() {
        when(recipeRepository.searchPublished(eq(null), eq(RecipeStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<RecipeResponse> result = recipeService.getPublishedRecipes(null, 0, 50);

        assertNotNull(result);
        verify(recipeRepository).searchPublished(eq(null), eq(RecipeStatus.PUBLISHED), any(Pageable.class));
    }

    @Test
    void getMyRecipes_excludesArchivedStatus() {
        when(recipeRepository.findByUserIdAndStatusNot(eq(100L), eq(RecipeStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<RecipeResponse> result = recipeService.getMyRecipes(100L, 0, 10);

        assertNotNull(result);
        verify(recipeRepository).findByUserIdAndStatusNot(eq(100L), eq(RecipeStatus.ARCHIVED), any(Pageable.class));
    }

    @Test
    void getRecipe_returnsArchivedRecipeForSavedMealPlans() {
        Recipe archived = Recipe.builder()
                .id(2L)
                .userId(100L)
                .title("Món cũ đã lưu")
                .status(RecipeStatus.ARCHIVED)
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(2L)
                .title("Món cũ đã lưu")
                .status(RecipeStatus.ARCHIVED)
                .build();

        when(recipeRepository.findById(2L)).thenReturn(Optional.of(archived));
        when(recipeMapper.toRecipeResponse(archived)).thenReturn(response);

        RecipeResponse result = recipeService.getRecipe(2L);

        assertNotNull(result);
        assertEquals(RecipeStatus.ARCHIVED, result.getStatus());
        assertEquals("Món cũ đã lưu", result.getTitle());
    }
}
