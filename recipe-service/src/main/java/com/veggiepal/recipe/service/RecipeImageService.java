package com.veggiepal.recipe.service;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.ImageType;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.RecipeMapper;
import com.veggiepal.recipe.repository.RecipeRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/** task_be C17: same order of checks as BlogService.uploadThumbnail. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecipeImageService {

    static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    // Task sheet US2: .jpg and .png only (no WEBP, unlike blog thumbnails)
    static final Set<ImageType> ALLOWED = Set.of(ImageType.JPEG, ImageType.PNG);

    RecipeRepository recipeRepository;
    RecipeService recipeService;
    FileStorageService fileStorageService;
    RecipeMapper recipeMapper;

    @Transactional
    public RecipeResponse uploadImage(Long userId, boolean admin, Long recipeId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.RECIPE_IMAGE_REQUIRED);
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new AppException(ErrorCode.RECIPE_IMAGE_TOO_LARGE);
        }

        // Ownership before storage: a stranger must not be able to write into the bucket
        Recipe recipe = recipeService.requireEditable(recipeId, userId, admin);

        byte[] content = readContent(file);

        // The declared content type must match the real file signature
        ImageType imageType = ImageTypeDetector.detect(content)
                .filter(ALLOWED::contains)
                .filter(type -> type.getContentType().equals(file.getContentType()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RECIPE_IMAGE_TYPE));

        String previousImageUrl = recipe.getImageUrl();

        String key = "recipes/" + recipeId + "/" + UUID.randomUUID() + "." + imageType.getExtension();
        String imageUrl = fileStorageService.upload(key, content, imageType.getContentType());

        recipe.setImageUrl(imageUrl);

        try {
            recipeRepository.save(recipe);
        } catch (RuntimeException exception) {
            deleteQuietly(imageUrl);
            throw exception;
        }

        // Ignores non-Cloudinary URLs, so a seeded or pasted imageUrl is left alone
        deleteQuietly(previousImageUrl);
        return recipeMapper.toRecipeResponse(recipe);
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void deleteQuietly(String url) {
        if (url == null) {
            return;
        }
        try {
            fileStorageService.delete(url);
        } catch (RuntimeException exception) {
            log.warn("Could not delete recipe image from storage", exception);
        }
    }
}
