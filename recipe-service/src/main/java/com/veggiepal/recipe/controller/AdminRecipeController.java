package com.veggiepal.recipe.controller;

import com.veggiepal.recipe.dto.response.ApiResponse;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/recipes")
@RequiredArgsConstructor
public class AdminRecipeController {
    private final RecipeService service;

    @GetMapping
    ApiResponse<PageResponse<RecipeResponse>> list(
            @RequestParam(required = false) RecipeStatus status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RecipeResponse>>builder()
                .result(service.getAdminRecipes(status, authorId, keyword, page, size)).build();
    }
}
