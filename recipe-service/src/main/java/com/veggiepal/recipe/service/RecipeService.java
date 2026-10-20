package com.veggiepal.recipe.service;

import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
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

    private static final Set<String> SUPPORTED_UNITS = Set.of(
            "g", "gram", "grams", "gr", "gam",
            "kg", "kilogram", "kilograms", "kilo",
            "mg", "milligram", "milligrams",
            "ml", "milliliter", "milliliters", "mililit",
            "l", "liter", "liters", "lit",
            "muong canh", "thia canh", "tbsp", "tablespoon",
            "muong ca phe", "thia ca phe", "tsp", "teaspoon",
            "qua", "trai", "cu", "bap", "tep", "cay", "mieng", "bia", "la",
            "piece", "pieces", "pcs"
    );

    RecipeRepository recipeRepository;
    IngredientRepository ingredientRepository;
    RecipeMapper recipeMapper;

    @Transactional
    public RecipeResponse createRecipe(
            Long userId,
            RecipeRequest request
    ) {

        validateRecipeItems(request);

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

            Ingredient ingredient = requireActiveIngredient(item.getIngredientId());

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
                                    normalizeAndValidateUnit(item.getUnit())
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
        return getRecipe(id, null, false);
    }

    @Transactional(readOnly = true)
    public RecipeResponse getRecipe(
            Long id,
            Long requesterId,
            boolean admin
    ) {
        Recipe recipe = findById(id);

        if (recipe.getStatus() == RecipeStatus.DRAFT || recipe.getStatus() == RecipeStatus.HIDDEN) {
            if (requesterId == null || (!admin && !recipe.getUserId().equals(requesterId))) {
                throw new AppException(ErrorCode.RECIPE_NOT_EXISTED);
            }
        }

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

        ensureMutable(recipe);
        validateRecipeItems(request);

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
        recipe.getSteps().clear();

        // Hibernate may schedule inserts before orphan deletes. Flush the removals first so
        // replacing an ingredient or step with the same unique key cannot produce HTTP 500.
        recipeRepository.flush();

        for (
                RecipeIngredientRequest item
                : request.getIngredients()
        ) {

            Ingredient ingredient = requireActiveIngredient(item.getIngredientId());

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
                                    normalizeAndValidateUnit(item.getUnit())
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

        ensureMutable(recipe);

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

        ensureMutable(recipe);

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

    private void validateRecipeItems(RecipeRequest request) {

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
                                .DUPLICATE_RECIPE_INGREDIENT
                );
            }
        }

        Set<Integer> stepNumbers = new HashSet<>();
        for (RecipeStepRequest item : request.getSteps()) {
            if (!stepNumbers.add(item.getStepNumber())) {
                throw new AppException(ErrorCode.DUPLICATE_RECIPE_STEP);
            }
        }
    }

    private Ingredient requireActiveIngredient(Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_EXISTED));
        if (!Boolean.TRUE.equals(ingredient.getActive())) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_ACTIVE);
        }
        return ingredient;
    }

    private void ensureMutable(Recipe recipe) {
        if (recipe.getStatus() == RecipeStatus.ARCHIVED || recipe.getStatus() == RecipeStatus.HIDDEN) {
            throw new AppException(ErrorCode.INVALID_RECIPE_STATUS_TRANSITION);
        }
    }

    private String normalizeAndValidateUnit(String rawUnit) {
        String normalized = Normalizer.normalize(rawUnit.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
        if (!SUPPORTED_UNITS.contains(normalized)) {
            throw new AppException(ErrorCode.INVALID_RECIPE_UNIT);
        }
        return rawUnit.trim().toUpperCase(Locale.ROOT);
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
