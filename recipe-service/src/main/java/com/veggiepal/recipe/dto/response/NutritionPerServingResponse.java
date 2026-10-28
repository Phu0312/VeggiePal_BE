package com.veggiepal.recipe.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** task_be C16. Computed on every read from the per-100g values; never stored. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NutritionPerServingResponse {

    BigDecimal calories;

    BigDecimal protein;

    BigDecimal carbs;

    BigDecimal fat;

    /** False when a row's unit could not be turned into grams; that row is left out of the sums. */
    Boolean complete;

    List<String> missingIngredients;
}
