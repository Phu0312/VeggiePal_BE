package com.veggiepal.recipe.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.recipe.dto.request.IngredientRequest;
import com.veggiepal.recipe.dto.response.ApiResponse;
import com.veggiepal.recipe.dto.response.IngredientResponse;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.service.IngredientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
@FieldDefaults(
        level = AccessLevel.PRIVATE,
        makeFinal = true
)
@Tag(
        name = "Ingredient",
        description = "Canonical ingredient catalog"
)
public class IngredientController {

    IngredientService ingredientService;

    @Operation(
            summary = "Get active ingredients"
    )
    @GetMapping
    ApiResponse<PageResponse<IngredientResponse>>
    getIngredients(
            @RequestParam(
                    required = false
            )
            String keyword,

            @RequestParam(
                    required = false
            )
            Boolean vegan,

            @RequestParam(
                    required = false
            )
            Boolean allergen,

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
                .<PageResponse<IngredientResponse>>
                        builder()
                .result(
                        ingredientService.getIngredients(
                                keyword,
                                vegan,
                                allergen,
                                page,
                                size
                        )
                )
                .build();
    }

    @Operation(
            summary = "Get ingredient detail"
    )
    @GetMapping("/{id}")
    ApiResponse<IngredientResponse>
    getIngredient(
            @PathVariable Long id
    ) {

        return ApiResponse
                .<IngredientResponse>builder()
                .result(
                        ingredientService
                                .getIngredient(id)
                )
                .build();
    }

    @Operation(
            summary = "Create ingredient - ADMIN only"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<IngredientResponse>
    createIngredient(
            @RequestBody
            @Valid
            IngredientRequest request
    ) {

        return ApiResponse
                .<IngredientResponse>builder()
                .result(
                        ingredientService
                                .createIngredient(
                                        request
                                )
                )
                .build();
    }

    @Operation(
            summary = "Update ingredient - ADMIN only"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<IngredientResponse>
    updateIngredient(
            @PathVariable Long id,

            @RequestBody
            @Valid
            IngredientRequest request
    ) {

        return ApiResponse
                .<IngredientResponse>builder()
                .result(
                        ingredientService
                                .updateIngredient(
                                        id,
                                        request
                                )
                )
                .build();
    }

    @Operation(
            summary = "Delete ingredient - ADMIN only"
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void>
    deleteIngredient(
            @PathVariable Long id
    ) {

        ingredientService
                .deleteIngredient(id);

        return ApiResponse
                .<Void>builder()
                .build();
    }
}