package com.veggiepal.recipe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeStepRequest {

    @NotNull
    @Min(1)
    Integer stepNumber;

    @NotBlank
    @Size(max = 2000, message = "RECIPE_INSTRUCTION_TOO_LONG")
    String instruction;
}