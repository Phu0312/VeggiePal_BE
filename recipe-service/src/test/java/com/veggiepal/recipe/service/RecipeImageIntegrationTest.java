package com.veggiepal.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.repository.RecipeRepository;

@SpringBootTest
class RecipeImageIntegrationTest {
    @Autowired RecipeImageService imageService;
    @Autowired RecipeRepository recipes;
    @MockitoBean FileStorageService storage;

    @Test
    void uploadCommitsNewImageBeforeDeletingOldImage() {
        Recipe recipe = recipes.saveAndFlush(Recipe.builder().userId(7711L)
                .title("Image IT " + UUID.randomUUID()).servings(1)
                .status(RecipeStatus.DRAFT).build());
        String oldUrl = "https://res.cloudinary.com/demo/image/upload/recipes/" + recipe.getId() + "/old.png";
        String newUrl = "https://res.cloudinary.com/demo/image/upload/recipes/" + recipe.getId() + "/new.png";
        recipe.setImageUrl(oldUrl);
        recipes.saveAndFlush(recipe);
        when(storage.upload(startsWith("recipes/" + recipe.getId() + "/"), any(), eq("image/png")))
                .thenReturn(newUrl);

        try {
            byte[] png = new byte[32];
            System.arraycopy(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
                    0, png, 0, 8);
            imageService.uploadImage(7711L, false, recipe.getId(),
                    new MockMultipartFile("file", "photo.png", "image/png", png));

            assertThat(recipes.findById(recipe.getId()).orElseThrow().getImageUrl()).isEqualTo(newUrl);
            verify(storage).delete(oldUrl);
        } finally {
            recipes.deleteById(recipe.getId());
        }
    }
}
