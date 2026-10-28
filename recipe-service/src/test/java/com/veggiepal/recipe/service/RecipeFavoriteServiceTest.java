package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeFavorite;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.RecipeFavoriteRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

class RecipeFavoriteServiceTest {

    RecipeRepository recipes;
    RecipeFavoriteRepository favorites;
    RecipeFavoriteService service;

    @BeforeEach
    void setUp() {
        recipes = mock(RecipeRepository.class);
        favorites = mock(RecipeFavoriteRepository.class);
        service = new RecipeFavoriteService(recipes, favorites, mock(RecipeMapper.class));
    }

    static Recipe published() {
        return Recipe.builder().id(5L).userId(1L).status(RecipeStatus.PUBLISHED).favoriteCount(3).build();
    }

    @Test
    void add_firstTime_savesAndCountsOnce() {
        when(recipes.findById(5L)).thenReturn(Optional.of(published()));
        when(favorites.existsByUserIdAndRecipeId(7L, 5L)).thenReturn(false);

        var response = service.add(7L, 5L);

        verify(favorites).save(any(RecipeFavorite.class));
        verify(recipes).addFavoriteCount(5L, 1);
        assertThat(response.getFavorited()).isTrue();
        assertThat(response.getFavoriteCount()).isEqualTo(4);
    }

    // Idempotent, so an optimistic heart can resend without double counting
    @Test
    void add_again_changesNothing() {
        when(recipes.findById(5L)).thenReturn(Optional.of(published()));
        when(favorites.existsByUserIdAndRecipeId(7L, 5L)).thenReturn(true);

        var response = service.add(7L, 5L);

        verify(favorites, never()).save(any());
        verify(recipes, never()).addFavoriteCount(any(), anyInt());
        assertThat(response.getFavoriteCount()).isEqualTo(3);
    }

    @Test
    void add_onADraft_isNotFound() {
        Recipe draft = Recipe.builder().id(5L).userId(1L).status(RecipeStatus.DRAFT).favoriteCount(0).build();
        when(recipes.findById(5L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.add(7L, 5L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECIPE_NOT_EXISTED);
    }

    @Test
    void remove_existing_deletesAndCountsDown() {
        RecipeFavorite favorite = RecipeFavorite.builder().id(1L).userId(7L).recipe(published()).build();
        when(recipes.findById(5L)).thenReturn(Optional.of(published()));
        when(favorites.findByUserIdAndRecipeId(7L, 5L)).thenReturn(Optional.of(favorite));

        var response = service.remove(7L, 5L);

        verify(favorites).delete(favorite);
        verify(recipes).addFavoriteCount(5L, -1);
        assertThat(response.getFavorited()).isFalse();
        assertThat(response.getFavoriteCount()).isEqualTo(2);
    }

    @Test
    void remove_whenNotFavorited_isANoOp() {
        when(recipes.findById(5L)).thenReturn(Optional.of(published()));
        when(favorites.findByUserIdAndRecipeId(7L, 5L)).thenReturn(Optional.empty());

        service.remove(7L, 5L);

        verify(recipes, never()).addFavoriteCount(any(), anyInt());
    }
}
