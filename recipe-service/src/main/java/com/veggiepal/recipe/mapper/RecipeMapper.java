package com.veggiepal.recipe.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.recipe.dto.response.RecipeIngredientResponse;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.dto.response.RecipeStepResponse;
import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeIngredient;
import com.veggiepal.recipe.entity.RecipeStep;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

    @Mapping(
            target = "ingredients",
            source = "ingredients"
    )
    @Mapping(
            target = "steps",
            source = "steps"
    )
    @Mapping(
            target = "nutritionPerServing",
            expression = "java(com.veggiepal.recipe.service.RecipeNutrition.perServing(recipe))"
    )
    RecipeResponse toRecipeResponse(
            Recipe recipe
    );

    @Mapping(
            target = "ingredientId",
            source = "ingredient.id"
    )
    @Mapping(
            target = "ingredientName",
            source = "ingredient.name"
    )
    @Mapping(target = "normalizedName", source = "ingredient.normalizedName")
    @Mapping(target = "caloriesPer100g", source = "ingredient.caloriesPer100g")
    @Mapping(target = "proteinPer100g", source = "ingredient.proteinPer100g")
    @Mapping(target = "carbsPer100g", source = "ingredient.carbsPer100g")
    @Mapping(target = "fatPer100g", source = "ingredient.fatPer100g")
    @Mapping(target = "vegan", source = "ingredient.vegan")
    @Mapping(target = "allergen", source = "ingredient.allergen")
    RecipeIngredientResponse
    toRecipeIngredientResponse(
            RecipeIngredient recipeIngredient
    );

    RecipeStepResponse toRecipeStepResponse(
            RecipeStep recipeStep
    );
}
