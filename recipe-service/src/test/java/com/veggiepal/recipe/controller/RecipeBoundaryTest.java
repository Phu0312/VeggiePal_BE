package com.veggiepal.recipe.controller;
import com.veggiepal.recipe.dto.request.*;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class RecipeBoundaryTest {
    @Test void exactDatabaseFieldLimitsPassValidation() {
        var request=RecipeRequest.builder().title("x".repeat(200)).description("x".repeat(2000))
                .imageUrl("x".repeat(500)).servings(1)
                .ingredients(List.of(RecipeIngredientRequest.builder().ingredientId(1L).quantity(BigDecimal.ONE)
                        .unit("x".repeat(30)).note("x".repeat(300)).build()))
                .steps(List.of(RecipeStepRequest.builder().stepNumber(1).instruction("x".repeat(2000)).build())).build();
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
    }
}
