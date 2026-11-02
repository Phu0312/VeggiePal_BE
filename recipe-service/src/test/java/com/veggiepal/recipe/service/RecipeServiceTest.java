package com.veggiepal.recipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veggiepal.recipe.dto.request.RecipeIngredientRequest;
import com.veggiepal.recipe.dto.request.RecipeRequest;
import com.veggiepal.recipe.dto.request.RecipeStepRequest;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.entity.Ingredient;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.IngredientRepository;
import com.veggiepal.recipe.repository.RecipeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class RecipeServiceTest {
    private RecipeRepository recipes;
    private IngredientRepository ingredients;
    private RecipeMapper mapper;
    private RecipeService service;

    @BeforeEach
    void setUp() {
        recipes = mock(RecipeRepository.class);
        ingredients = mock(IngredientRepository.class);
        mapper = mock(RecipeMapper.class);
        service = new RecipeService(recipes, ingredients, mapper);
    }

    @Test
    void deleteRecipeSoftDeletes() {
        Recipe recipe = Recipe.builder().id(1L).userId(100L).status(RecipeStatus.PUBLISHED).build();
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        service.deleteRecipe(1L, 100L, false);
        assertEquals(RecipeStatus.ARCHIVED, recipe.getStatus());
        verify(recipes).save(recipe);
        verify(recipes, never()).delete(any());
    }

    @Test
    void publishedAndOwnListingsApplyStatusRules() {
        when(recipes.searchPublished(eq(null), eq(RecipeStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(recipes.findByUserIdAndStatusNot(eq(100L), eq(RecipeStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        PageResponse<RecipeResponse> published = service.getPublishedRecipes(null, 0, 50);
        PageResponse<RecipeResponse> own = service.getMyRecipes(100L, 0, 10);
        assertNotNull(published);
        assertNotNull(own);
    }

    @Test
    void archivedDetailRemainsAvailableForSavedMealPlans() {
        Recipe archived = Recipe.builder().id(2L).userId(100L).status(RecipeStatus.ARCHIVED).build();
        RecipeResponse response = RecipeResponse.builder().id(2L).status(RecipeStatus.ARCHIVED).build();
        when(recipes.findById(2L)).thenReturn(Optional.of(archived));
        when(mapper.toRecipeResponse(archived)).thenReturn(response);
        assertEquals(RecipeStatus.ARCHIVED, service.getRecipe(2L).getStatus());
    }

    @Test
    void draftDetailIsNotPublic() {
        when(recipes.findById(3L)).thenReturn(Optional.of(
                Recipe.builder().id(3L).userId(100L).status(RecipeStatus.DRAFT).build()));
        AppException exception = assertThrows(AppException.class, () -> service.getRecipe(3L));
        assertEquals(ErrorCode.RECIPE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void draftDetailIsReadableByOwner() {
        Recipe draft = Recipe.builder().id(3L).userId(100L).status(RecipeStatus.DRAFT).build();
        RecipeResponse response = RecipeResponse.builder().id(3L).status(RecipeStatus.DRAFT).build();
        when(recipes.findById(3L)).thenReturn(Optional.of(draft));
        when(mapper.toRecipeResponse(draft)).thenReturn(response);
        assertEquals(RecipeStatus.DRAFT, service.getRecipe(3L, 100L, false).getStatus());
    }

    @Test
    void draftDetailIsReadableByAdmin() {
        Recipe draft = Recipe.builder().id(3L).userId(100L).status(RecipeStatus.DRAFT).build();
        RecipeResponse response = RecipeResponse.builder().id(3L).status(RecipeStatus.DRAFT).build();
        when(recipes.findById(3L)).thenReturn(Optional.of(draft));
        when(mapper.toRecipeResponse(draft)).thenReturn(response);
        assertEquals(RecipeStatus.DRAFT, service.getRecipe(3L, 999L, true).getStatus());
    }

    @Test
    void draftDetailIsHiddenFromOtherUser() {
        Recipe draft = Recipe.builder().id(3L).userId(100L).status(RecipeStatus.DRAFT).build();
        when(recipes.findById(3L)).thenReturn(Optional.of(draft));
        AppException exception = assertThrows(AppException.class, () -> service.getRecipe(3L, 999L, false));
        assertEquals(ErrorCode.RECIPE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void hiddenDetailIsReadableByOwnerAndAdmin_hiddenFromOthers() {
        Recipe hidden = Recipe.builder().id(4L).userId(100L).status(RecipeStatus.HIDDEN).build();
        RecipeResponse response = RecipeResponse.builder().id(4L).status(RecipeStatus.HIDDEN).build();
        when(recipes.findById(4L)).thenReturn(Optional.of(hidden));
        when(mapper.toRecipeResponse(hidden)).thenReturn(response);

        assertEquals(RecipeStatus.HIDDEN, service.getRecipe(4L, 100L, false).getStatus());
        assertEquals(RecipeStatus.HIDDEN, service.getRecipe(4L, 999L, true).getStatus());

        AppException exOther = assertThrows(AppException.class, () -> service.getRecipe(4L, 999L, false));
        assertEquals(ErrorCode.RECIPE_NOT_EXISTED, exOther.getErrorCode());

        AppException exGuest = assertThrows(AppException.class, () -> service.getRecipe(4L, null, false));
        assertEquals(ErrorCode.RECIPE_NOT_EXISTED, exGuest.getErrorCode());
    }

    @Test
    void updateFlushesOrphanDeletesBeforeReplacementRows() {
        Recipe recipe = Recipe.builder().id(1L).userId(100L).status(RecipeStatus.DRAFT).build();
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        when(ingredients.findById(9L)).thenReturn(Optional.of(activeIngredient()));
        when(recipes.save(recipe)).thenReturn(recipe);
        service.updateRecipe(1L, 100L, false, request("G", 1, 2));
        verify(recipes).flush();
        verify(recipes).save(recipe);
    }

    @Test
    void updateWithoutImageUrl_preservesExistingImage() {
        Recipe recipe = Recipe.builder().id(1L).userId(100L).status(RecipeStatus.DRAFT)
                .imageUrl("https://res.cloudinary.com/demo/image/upload/recipes/1/old.png").build();
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        when(ingredients.findById(9L)).thenReturn(Optional.of(activeIngredient()));
        when(recipes.save(recipe)).thenReturn(recipe);

        service.updateRecipe(1L, 100L, false, request("G", 1, 2));

        assertEquals("https://res.cloudinary.com/demo/image/upload/recipes/1/old.png", recipe.getImageUrl());
    }

    @Test
    void updateWithDifferentImageUrl_requiresImageUpload() {
        Recipe recipe = Recipe.builder().id(1L).userId(100L).status(RecipeStatus.DRAFT)
                .imageUrl("https://res.cloudinary.com/demo/image/upload/recipes/1/old.png").build();
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        var request = request("G", 1, 2);
        request.setImageUrl("https://res.cloudinary.com/demo/image/upload/recipes/1/other.png");

        assertEquals(ErrorCode.RECIPE_IMAGE_UPDATE_REQUIRES_UPLOAD,
                assertThrows(AppException.class,
                        () -> service.updateRecipe(1L, 100L, false, request)).getErrorCode());
        verify(recipes, never()).save(any());
    }

    @Test
    void duplicateStepNumbersAreRejectedBeforeSave() {
        AppException exception = assertThrows(AppException.class,
                () -> service.createRecipe(100L, request("G", 1, 1)));
        assertEquals(ErrorCode.DUPLICATE_RECIPE_STEP, exception.getErrorCode());
        verify(recipes, never()).save(any());
    }

    @Test
    void unsupportedUnitIsRejected() {
        when(ingredients.findById(9L)).thenReturn(Optional.of(activeIngredient()));
        AppException exception = assertThrows(AppException.class,
                () -> service.createRecipe(100L, request("bucket", 1, 2)));
        assertEquals(ErrorCode.INVALID_RECIPE_UNIT, exception.getErrorCode());
    }

    @Test
    void updateRejectsInactiveIngredient() {
        Recipe recipe = Recipe.builder().id(1L).userId(100L).status(RecipeStatus.DRAFT).build();
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        when(ingredients.findById(9L)).thenReturn(Optional.of(
                Ingredient.builder().id(9L).active(false).build()));
        AppException exception = assertThrows(AppException.class,
                () -> service.updateRecipe(1L, 100L, false, request("G", 1, 2)));
        assertEquals(ErrorCode.INGREDIENT_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    void archivedRecipeCannotBeUpdatedOrRepublished() {
        Recipe recipe = Recipe.builder().id(1L).userId(100L).status(RecipeStatus.ARCHIVED).build();
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        assertEquals(ErrorCode.INVALID_RECIPE_STATUS_TRANSITION,
                assertThrows(AppException.class,
                        () -> service.updateRecipe(1L, 100L, false, request("G", 1, 2))).getErrorCode());
        assertEquals(ErrorCode.INVALID_RECIPE_STATUS_TRANSITION,
                assertThrows(AppException.class,
                        () -> service.publishRecipe(1L, 100L, false)).getErrorCode());
    }

    private Ingredient activeIngredient() {
        return Ingredient.builder().id(9L).active(true).build();
    }

    private RecipeRequest request(String unit, int firstStep, int secondStep) {
        return RecipeRequest.builder().title("Recipe").servings(2)
                .ingredients(List.of(RecipeIngredientRequest.builder()
                        .ingredientId(9L).quantity(BigDecimal.ONE).unit(unit).build()))
                .steps(List.of(
                        RecipeStepRequest.builder().stepNumber(firstStep).instruction("First").build(),
                        RecipeStepRequest.builder().stepNumber(secondStep).instruction("Second").build()))
                .build();
    }

    @Test void publishedPageSizeIsCappedAt100WithStableOrdering() {
        when(recipes.searchPublished(eq(null),eq(RecipeStatus.PUBLISHED),any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.getPublishedRecipes(null,-1,200);
        var pageable=org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(recipes).searchPublished(eq(null),eq(RecipeStatus.PUBLISHED),pageable.capture());
        assertEquals(0,pageable.getValue().getPageNumber());
        assertEquals(100,pageable.getValue().getPageSize());
        assertNotNull(pageable.getValue().getSort().getOrderFor("id"));
    }

}
