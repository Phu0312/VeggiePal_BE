package com.veggiepal.recipe.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.recipe.dto.request.RecipeIngredientRequest;
import com.veggiepal.recipe.dto.request.RecipeRequest;
import com.veggiepal.recipe.dto.request.RecipeStepRequest;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.entity.Ingredient;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeIngredient;
import com.veggiepal.recipe.entity.RecipeStep;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.IngredientRepository;
import com.veggiepal.recipe.repository.RecipeRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(
        level = AccessLevel.PRIVATE,
        makeFinal = true
)
public class RecipeService {

    RecipeRepository recipeRepository;
    IngredientRepository ingredientRepository;
    RecipeMapper recipeMapper;

    @Transactional
    public RecipeResponse createRecipe(
            Long userId,
            RecipeRequest request
    ) {

        validateDuplicateIngredients(
                request
        );

        Recipe recipe = Recipe
                .builder()
                .userId(userId)
                .title(
                        request.getTitle().trim()
                )
                .description(
                        trimToNull(
                                request.getDescription()
                        )
                )
                .imageUrl(
                        trimToNull(
                                request.getImageUrl()
                        )
                )
                .servings(
                        request.getServings()
                )
                .prepTime(
                        request.getPrepTime()
                )
                .cookTime(
                        request.getCookTime()
                )
                .status(
                        RecipeStatus.DRAFT
                )
                .build();

        for (
                RecipeIngredientRequest item
                : request.getIngredients()
        ) {

            Ingredient ingredient =
                    ingredientRepository
                            .findById(
                                    item.getIngredientId()
                            )
                            .orElseThrow(
                                    () ->
                                            new AppException(
                                                    ErrorCode
                                                            .INGREDIENT_NOT_EXISTED
                                            )
                            );

            if (!Boolean.TRUE.equals(
                    ingredient.getActive()
            )) {
                throw new AppException(
                        ErrorCode.INGREDIENT_NOT_ACTIVE
                );
            }

            RecipeIngredient recipeIngredient =
                    RecipeIngredient
                            .builder()
                            .ingredient(
                                    ingredient
                            )
                            .quantity(
                                    item.getQuantity()
                            )
                            .unit(
                                    item.getUnit()
                                            .trim()
                                            .toUpperCase()
                            )
                            .note(
                                    trimToNull(
                                            item.getNote()
                                    )
                            )
                            .build();

            recipe.addIngredient(
                    recipeIngredient
            );
        }

        for (
                RecipeStepRequest item
                : request.getSteps()
        ) {

            RecipeStep step =
                    RecipeStep
                            .builder()
                            .stepNumber(
                                    item.getStepNumber()
                            )
                            .instruction(
                                    item.getInstruction()
                                            .trim()
                            )
                            .build();

            recipe.addStep(step);
        }

        Recipe saved =
                recipeRepository.save(recipe);

        return recipeMapper
                .toRecipeResponse(saved);
    }

    @Transactional(readOnly = true)
    public RecipeResponse getRecipe(
            Long id
    ) {

        Recipe recipe =
                findById(id);

        return recipeMapper
                .toRecipeResponse(recipe);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse>
    getPublishedRecipes(
            String keyword,
            int page,
            int size
    ) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        200
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        String safeKeyword =
                normalizeKeyword(keyword);

        Page<Recipe> recipePage =
                recipeRepository
                        .searchPublished(
                                safeKeyword,
                                RecipeStatus.PUBLISHED,
                                pageable
                        );

        return toPageResponse(
                recipePage
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse>
    getMyRecipes(
            Long userId,
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.min(
                                Math.max(size, 1),
                                100
                        ),
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<Recipe> recipePage =
                recipeRepository
                        .findByUserIdAndStatusNot(
                                userId,
                                RecipeStatus.ARCHIVED,
                                pageable
                        );

        return toPageResponse(
                recipePage
        );
    }

    @Transactional
    public RecipeResponse updateRecipe(
            Long id,
            Long userId,
            boolean admin,
            RecipeRequest request
    ) {

        Recipe recipe =
                findById(id);

        checkOwner(
                recipe,
                userId,
                admin
        );

        validateDuplicateIngredients(
                request
        );

        recipe.setTitle(
                request.getTitle().trim()
        );

        recipe.setDescription(
                trimToNull(
                        request.getDescription()
                )
        );

        recipe.setImageUrl(
                trimToNull(
                        request.getImageUrl()
                )
        );

        recipe.setServings(
                request.getServings()
        );

        recipe.setPrepTime(
                request.getPrepTime()
        );

        recipe.setCookTime(
                request.getCookTime()
        );

        recipe.getIngredients().clear();

        for (
                RecipeIngredientRequest item
                : request.getIngredients()
        ) {

            Ingredient ingredient =
                    ingredientRepository
                            .findById(
                                    item.getIngredientId()
                            )
                            .orElseThrow(
                                    () ->
                                            new AppException(
                                                    ErrorCode
                                                            .INGREDIENT_NOT_EXISTED
                                            )
                            );

            RecipeIngredient recipeIngredient =
                    RecipeIngredient
                            .builder()
                            .ingredient(
                                    ingredient
                            )
                            .quantity(
                                    item.getQuantity()
                            )
                            .unit(
                                    item.getUnit()
                                            .trim()
                                            .toUpperCase()
                            )
                            .note(
                                    trimToNull(
                                            item.getNote()
                                    )
                            )
                            .build();

            recipe.addIngredient(
                    recipeIngredient
            );
        }

        recipe.getSteps().clear();

        for (
                RecipeStepRequest item
                : request.getSteps()
        ) {

            RecipeStep step =
                    RecipeStep
                            .builder()
                            .stepNumber(
                                    item.getStepNumber()
                            )
                            .instruction(
                                    item.getInstruction()
                                            .trim()
                            )
                            .build();

            recipe.addStep(step);
        }

        Recipe saved =
                recipeRepository.save(recipe);

        return recipeMapper
                .toRecipeResponse(saved);
    }

    @Transactional
    public RecipeResponse publishRecipe(
            Long id,
            Long userId,
            boolean admin
    ) {

        Recipe recipe =
                findById(id);

        checkOwner(
                recipe,
                userId,
                admin
        );

        if (
                recipe.getStatus()
                        == RecipeStatus.PUBLISHED
        ) {

            throw new AppException(
                    ErrorCode
                            .RECIPE_ALREADY_PUBLISHED
            );
        }

        recipe.setStatus(
                RecipeStatus.PUBLISHED
        );

        recipe.setPublishedAt(
                LocalDateTime.now()
        );

        Recipe saved =
                recipeRepository.save(recipe);

        return recipeMapper
                .toRecipeResponse(saved);
    }

    @Transactional
    public void deleteRecipe(
            Long id,
            Long userId,
            boolean admin
    ) {

        Recipe recipe =
                findById(id);

        checkOwner(
                recipe,
                userId,
                admin
        );

        recipe.setStatus(
                RecipeStatus.ARCHIVED
        );

        recipeRepository.save(
                recipe
        );
    }

    private Recipe findById(
            Long id
    ) {

        return recipeRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new AppException(
                                        ErrorCode
                                                .RECIPE_NOT_EXISTED
                                )
                );
    }

    private void checkOwner(
            Recipe recipe,
            Long userId,
            boolean admin
    ) {

        if (admin) {
            return;
        }

        if (
                !recipe.getUserId()
                        .equals(userId)
        ) {

            throw new AppException(
                    ErrorCode.RECIPE_FORBIDDEN
            );
        }
    }

    private void validateDuplicateIngredients(
            RecipeRequest request
    ) {

        Set<Long> ingredientIds =
                new HashSet<>();

        for (
                RecipeIngredientRequest item
                : request.getIngredients()
        ) {

            if (
                    !ingredientIds.add(
                            item.getIngredientId()
                    )
            ) {

                throw new AppException(
                        ErrorCode
                                .INGREDIENT_NAME_DUPLICATED
                );
            }
        }
    }

    private PageResponse<RecipeResponse>
    toPageResponse(
            Page<Recipe> recipePage
    ) {

        return PageResponse
                .<RecipeResponse>builder()
                .content(
                        recipePage
                                .getContent()
                                .stream()
                                .map(
                                        recipeMapper
                                                ::toRecipeResponse
                                )
                                .toList()
                )
                .page(
                        recipePage.getNumber()
                )
                .size(
                        recipePage.getSize()
                )
                .totalElements(
                        recipePage
                                .getTotalElements()
                )
                .totalPages(
                        recipePage
                                .getTotalPages()
                )
                .first(
                        recipePage.isFirst()
                )
                .last(
                        recipePage.isLast()
                )
                .build();
    }

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null) {
            return null;
        }

        String value =
                keyword.trim();

        return value.isBlank()
                ? null
                : value;
    }

    private String trimToNull(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isBlank()
                ? null
                : trimmed;
    }
}