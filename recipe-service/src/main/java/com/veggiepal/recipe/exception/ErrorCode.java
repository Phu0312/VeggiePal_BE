package com.veggiepal.recipe.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    // Ingredient
    INGREDIENT_NAME_REQUIRED(4000, "Ingredient name is required", HttpStatus.BAD_REQUEST),

    INVALID_INGREDIENT_NAME(4001, "Ingredient name must not exceed 150 characters", HttpStatus.BAD_REQUEST),

    INVALID_INGREDIENT_DESCRIPTION(4002, "Ingredient description must not exceed 1000 characters", HttpStatus.BAD_REQUEST),

    INGREDIENT_NOT_EXISTED(4003, "Ingredient not existed", HttpStatus.NOT_FOUND),

    INGREDIENT_NAME_DUPLICATED(4004, "Ingredient name already exists", HttpStatus.BAD_REQUEST),

    CALORIES_REQUIRED(4005, "Calories value is required", HttpStatus.BAD_REQUEST),

    PROTEIN_REQUIRED(4006, "Protein value is required", HttpStatus.BAD_REQUEST),

    CARBS_REQUIRED(4007, "Carbohydrate value is required", HttpStatus.BAD_REQUEST),

    FAT_REQUIRED(4008, "Fat value is required", HttpStatus.BAD_REQUEST),

    INVALID_NUTRITION_VALUE(4009, "Nutrition value must be greater than or equal to 0", HttpStatus.BAD_REQUEST),

    RECIPE_NOT_EXISTED(4100, "Recipe not existed", HttpStatus.NOT_FOUND),

    RECIPE_TITLE_REQUIRED(4101, "Recipe title is required", HttpStatus.BAD_REQUEST),

    RECIPE_FORBIDDEN(4102, "You cannot modify this recipe", HttpStatus.FORBIDDEN),

    RECIPE_ALREADY_PUBLISHED(4103, "Recipe already published", HttpStatus.BAD_REQUEST),

    INGREDIENT_NOT_ACTIVE(4104, "Ingredient is not active", HttpStatus.BAD_REQUEST),

    DUPLICATE_RECIPE_INGREDIENT(4105, "A recipe cannot contain the same ingredient more than once", HttpStatus.BAD_REQUEST),

    DUPLICATE_RECIPE_STEP(4106, "Recipe step numbers must be unique", HttpStatus.BAD_REQUEST),

    INVALID_RECIPE_UNIT(4107, "Ingredient unit is not supported", HttpStatus.BAD_REQUEST),

    INVALID_RECIPE_STATUS_TRANSITION(4108, "Recipe is not in a state that allows this action", HttpStatus.BAD_REQUEST);

    ErrorCode(
            int code,
            String message,
            HttpStatusCode statusCode
    ) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    final int code;

    final String message;

    final HttpStatusCode statusCode;
}
