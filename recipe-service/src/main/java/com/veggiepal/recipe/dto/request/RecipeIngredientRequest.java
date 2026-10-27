package com.veggiepal.recipe.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeIngredientRequest {

    @NotNull
    Long ingredientId;

    @NotNull
    @DecimalMin("0.01")
    BigDecimal quantity;

    @NotNull
    @Size(max = 30, message = "RECIPE_UNIT_TOO_LONG")
    String unit;

    @Size(max = 300, message = "RECIPE_NOTE_TOO_LONG")
    String note;
}