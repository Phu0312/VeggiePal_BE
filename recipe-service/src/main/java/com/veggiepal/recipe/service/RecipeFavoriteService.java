package com.veggiepal.recipe.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeFavoriteResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeFavorite;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.RecipeFavoriteRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * task_be C18 + D21. Add and remove are idempotent so an optimistic heart can resend.
 * ponytail: two simultaneous first adds by the same user hit uk_recipe_favorites_user_recipe and the
 * loser answers 500 (data stays right); catch DataIntegrityViolationException if that shows up.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecipeFavoriteService {

    RecipeRepository recipeRepository;
    RecipeFavoriteRepository favoriteRepository;
    RecipeMapper recipeMapper;

    @Transactional
    public RecipeFavoriteResponse add(Long userId, Long recipeId) {

        Recipe recipe = recipeRepository.findById(recipeId)
                .filter(found -> found.getStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_EXISTED));

        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            return response(recipeId, true, recipe.getFavoriteCount());
        }

        favoriteRepository.save(RecipeFavorite.builder().userId(userId).recipe(recipe).build());
        recipeRepository.addFavoriteCount(recipeId, 1);

        // The JPQL update bypasses the persistence context, so add the delta here too
        return response(recipeId, true, recipe.getFavoriteCount() + 1);
    }

    // Works on a recipe hidden since, so a user can always empty their collection
    @Transactional
    public RecipeFavoriteResponse remove(Long userId, Long recipeId) {

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_EXISTED));

        return favoriteRepository.findByUserIdAndRecipeId(userId, recipeId)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    recipeRepository.addFavoriteCount(recipeId, -1);
                    return response(recipeId, false, recipe.getFavoriteCount() - 1);
                })
                .orElseGet(() -> response(recipeId, false, recipe.getFavoriteCount()));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> myFavorites(Long userId, int page, int size) {

        Page<Recipe> recipes = favoriteRepository.findFavoriteRecipes(
                userId, RecipeStatus.PUBLISHED,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));

        return PageResponse.<RecipeResponse>builder()
                .content(recipes.getContent().stream().map(recipeMapper::toRecipeResponse).toList())
                .page(recipes.getNumber())
                .size(recipes.getSize())
                .totalElements(recipes.getTotalElements())
                .totalPages(recipes.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Long> myFavoriteIds(Long userId) {
        return favoriteRepository.findRecipeIdsByUserId(userId);
    }

    private static RecipeFavoriteResponse response(Long recipeId, boolean favorited, Integer count) {
        return RecipeFavoriteResponse.builder()
                .recipeId(recipeId)
                .favorited(favorited)
                .favoriteCount(count == null ? 0 : count)
                .build();
    }
}
