package com.veggiepal.recipe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.recipe.dto.response.RecipeFavoriteResponse;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeFavorite;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.repository.RecipeFavoriteRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Owns the insert transaction so a duplicate can roll back before the caller reads the winner. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class RecipeFavoriteWriter {
    RecipeRepository recipeRepository;
    RecipeFavoriteRepository favoriteRepository;

    @Transactional
    public RecipeFavoriteResponse add(Long userId, Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .filter(found -> found.getStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_EXISTED));
        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            return RecipeFavoriteService.response(recipeId, true, recipe.getFavoriteCount());
        }
        favoriteRepository.saveAndFlush(RecipeFavorite.builder().userId(userId).recipe(recipe).build());
        recipeRepository.addFavoriteCount(recipeId, 1);
        return RecipeFavoriteService.response(recipeId, true, recipe.getFavoriteCount() + 1);
    }
}
