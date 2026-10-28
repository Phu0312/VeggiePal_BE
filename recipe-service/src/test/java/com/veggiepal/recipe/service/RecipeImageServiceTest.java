package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.RecipeRepository;

class RecipeImageServiceTest {

    RecipeRepository recipes;
    RecipeService recipeService;
    FileStorageService storage;
    RecipeMapper mapper;
    RecipeImageService service;

    @BeforeEach
    void setUp() {
        recipes = mock(RecipeRepository.class);
        recipeService = mock(RecipeService.class);
        storage = mock(FileStorageService.class);
        mapper = mock(RecipeMapper.class);
        service = new RecipeImageService(recipes, recipeService, storage, mapper);
    }

    static byte[] png() {
        byte[] content = new byte[32];
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, content, 0, signature.length);
        return content;
    }

    @Test
    void upload_storesTheImageAndRemovesTheOldOne() {
        Recipe recipe = Recipe.builder().id(5L).userId(7L)
                .imageUrl("https://res.cloudinary.com/demo/image/upload/recipes/5/old.png").build();
        when(recipeService.requireEditable(5L, 7L, false)).thenReturn(recipe);
        when(storage.upload(startsWith("recipes/5/"), any(), eq("image/png")))
                .thenReturn("https://res.cloudinary.com/demo/image/upload/recipes/5/new.png");

        service.uploadImage(7L, false, 5L, new MockMultipartFile("file", "a.png", "image/png", png()));

        assertThat(recipe.getImageUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/recipes/5/new.png");
        verify(recipes).save(recipe);
        verify(storage).delete("https://res.cloudinary.com/demo/image/upload/recipes/5/old.png");
    }

    @Test
    void upload_missingFile_isRejected() {
        assertCode(() -> service.uploadImage(7L, false, 5L, null), ErrorCode.RECIPE_IMAGE_REQUIRED);
    }

    @Test
    void upload_over5MB_isRejectedBeforeAnythingElse() {
        MockMultipartFile big = new MockMultipartFile("file", "a.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertCode(() -> service.uploadImage(7L, false, 5L, big), ErrorCode.RECIPE_IMAGE_TOO_LARGE);
        verify(recipeService, never()).requireEditable(any(), any(), eq(false));
    }

    // Task sheet US2: .jpg/.png only, and the declared type must match the real bytes
    @Test
    void upload_pngDeclaredAsJpeg_isRejectedWithoutUploading() {
        when(recipeService.requireEditable(5L, 7L, false)).thenReturn(Recipe.builder().id(5L).userId(7L).build());

        assertCode(() -> service.uploadImage(7L, false, 5L,
                new MockMultipartFile("file", "a.jpg", "image/jpeg", png())), ErrorCode.INVALID_RECIPE_IMAGE_TYPE);
        verify(storage, never()).upload(anyString(), any(), anyString());
    }

    @Test
    void upload_byAStranger_neverReachesStorage() {
        when(recipeService.requireEditable(5L, 8L, false)).thenThrow(new AppException(ErrorCode.RECIPE_FORBIDDEN));

        assertCode(() -> service.uploadImage(8L, false, 5L,
                new MockMultipartFile("file", "a.png", "image/png", png())), ErrorCode.RECIPE_FORBIDDEN);
        verify(storage, never()).upload(anyString(), any(), anyString());
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(code);
    }
}
