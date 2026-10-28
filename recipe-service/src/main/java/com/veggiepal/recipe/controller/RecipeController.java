package com.veggiepal.recipe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.recipe.dto.request.RecipeRequest;
import com.veggiepal.recipe.dto.response.ApiResponse;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeFavoriteResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.service.RecipeFavoriteService;
import com.veggiepal.recipe.service.RecipeImageService;
import com.veggiepal.recipe.service.RecipeService;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
@FieldDefaults(
        level = AccessLevel.PRIVATE,
        makeFinal = true
)
public class RecipeController {

    RecipeService recipeService;
    RecipeImageService recipeImageService;
    RecipeFavoriteService recipeFavoriteService;

    @GetMapping
    ApiResponse<PageResponse<RecipeResponse>>
    getPublishedRecipes(
            @RequestParam(
                    required = false
            )
            String keyword,

            @RequestParam(
                    required = false
            )
            String sort,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size
    ) {

        return ApiResponse
                .<PageResponse<RecipeResponse>>
                        builder()
                .result(
                        recipeService
                                .getPublishedRecipes(
                                        keyword,
                                        sort,
                                        page,
                                        size
                                )
                )
                .build();
    }

    // task_be C18 + D21: the caller's collection, most recently loved first
    @GetMapping("/favorites")
    ApiResponse<PageResponse<RecipeResponse>> getMyFavorites(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<RecipeResponse>>builder()
                .result(recipeFavoriteService.myFavorites(CurrentUser.id(jwt), page, size))
                .build();
    }

    // To paint the hearts on a public listing
    @GetMapping("/favorites/ids")
    ApiResponse<List<Long>> getMyFavoriteIds(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<Long>>builder()
                .result(recipeFavoriteService.myFavoriteIds(CurrentUser.id(jwt)))
                .build();
    }

    @PostMapping("/{id}/favorite")
    ApiResponse<RecipeFavoriteResponse> addFavorite(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<RecipeFavoriteResponse>builder()
                .result(recipeFavoriteService.add(CurrentUser.id(jwt), id))
                .build();
    }

    @DeleteMapping("/{id}/favorite")
    ApiResponse<RecipeFavoriteResponse> removeFavorite(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<RecipeFavoriteResponse>builder()
                .result(recipeFavoriteService.remove(CurrentUser.id(jwt), id))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<RecipeResponse> getRecipe(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return ApiResponse
                .<RecipeResponse>builder()
                .result(
                        recipeService.getRecipe(
                                id,
                                jwt == null ? null : CurrentUser.id(jwt),
                                jwt != null && CurrentUser.isAdmin(jwt)
                        )
                )
                .build();
    }

    @GetMapping("/me")
    ApiResponse<PageResponse<RecipeResponse>>
    getMyRecipes(
            @AuthenticationPrincipal
            Jwt jwt,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size
    ) {

        Long userId =
                CurrentUser.id(jwt);

        return ApiResponse
                .<PageResponse<RecipeResponse>>
                        builder()
                .result(
                        recipeService
                                .getMyRecipes(
                                        userId,
                                        page,
                                        size
                                )
                )
                .build();
    }

    @PostMapping
    ApiResponse<RecipeResponse>
    createRecipe(
            @AuthenticationPrincipal
            Jwt jwt,

            @RequestBody
            @Valid
            RecipeRequest request
    ) {

        Long userId =
                CurrentUser.id(jwt);

        return ApiResponse
                .<RecipeResponse>builder()
                .result(
                        recipeService
                                .createRecipe(
                                        userId,
                                        request
                                )
                )
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<RecipeResponse>
    updateRecipe(
            @PathVariable Long id,

            @AuthenticationPrincipal
            Jwt jwt,

            @RequestBody
            @Valid
            RecipeRequest request
    ) {

        Long userId =
                CurrentUser.id(jwt);

        boolean admin =
                CurrentUser.isAdmin(jwt);

        return ApiResponse
                .<RecipeResponse>builder()
                .result(
                        recipeService
                                .updateRecipe(
                                        id,
                                        userId,
                                        admin,
                                        request
                                )
                )
                .build();
    }

    @PostMapping("/{id}/publish")
    ApiResponse<RecipeResponse>
    publishRecipe(
            @PathVariable Long id,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        Long userId =
                CurrentUser.id(jwt);

        boolean admin =
                CurrentUser.isAdmin(jwt);

        return ApiResponse
                .<RecipeResponse>builder()
                .result(
                        recipeService
                                .publishRecipe(
                                        id,
                                        userId,
                                        admin
                                )
                )
                .build();
    }

    // task_be C17: multipart field "file", JPEG/PNG up to 5MB, owner or admin
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<RecipeResponse> uploadImage(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.<RecipeResponse>builder()
                .result(recipeImageService.uploadImage(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id, file))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void>
    deleteRecipe(
            @PathVariable Long id,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        Long userId =
                CurrentUser.id(jwt);

        boolean admin =
                CurrentUser.isAdmin(jwt);

        recipeService.deleteRecipe(
                id,
                userId,
                admin
        );

        return ApiResponse
                .<Void>builder()
                .build();
    }
}