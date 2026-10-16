package com.veggiepal.recipe.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeIngredientResponse {

    Long ingredientId;

    String ingredientName;

    String normalizedName;

    BigDecimal caloriesPer100g;

    BigDecimal proteinPer100g;

    BigDecimal carbsPer100g;

    BigDecimal fatPer100g;

    Boolean vegan;

    Boolean allergen;

    BigDecimal quantity;

    String unit;

    String note;
}
