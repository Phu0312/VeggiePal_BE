package com.veggiepal.recipe.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeRequest {

    @NotBlank(message = "RECIPE_TITLE_REQUIRED")
    @Size(max = 200, message = "RECIPE_TITLE_TOO_LONG")
    String title;

    @Size(max = 2000, message = "RECIPE_DESCRIPTION_TOO_LONG")
    String description;

    @Size(max = 500, message = "RECIPE_IMAGE_URL_TOO_LONG")
    String imageUrl;

    @NotNull
    @Min(1)
    Integer servings;

    @Min(0)
    Integer prepTime;

    @Min(0)
    Integer cookTime;

    @NotEmpty
    List<@Valid RecipeIngredientRequest> ingredients;

    @NotEmpty
    List<@Valid RecipeStepRequest> steps;
}