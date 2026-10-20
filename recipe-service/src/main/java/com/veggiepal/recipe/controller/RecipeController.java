package com.veggiepal.recipe.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.recipe.dto.request.RecipeRequest;
import com.veggiepal.recipe.dto.response.ApiResponse;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
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

    @GetMapping
    ApiResponse<PageResponse<RecipeResponse>>
    getPublishedRecipes(
            @RequestParam(
                    required = false
            )
            String keyword,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
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
                                        page,
                                        size
                                )
                )
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
                    defaultValue = "10"
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