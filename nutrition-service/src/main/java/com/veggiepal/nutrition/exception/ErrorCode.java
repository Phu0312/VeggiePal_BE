package com.veggiepal.nutrition.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Shared codes: keep the same numbers as identity-service
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    HEIGHT_REQUIRED(2001, "Height is required", HttpStatus.BAD_REQUEST),

    INVALID_HEIGHT(2002, "Height must be between 50 and 250 cm with at most 1 decimal", HttpStatus.BAD_REQUEST),

    WEIGHT_REQUIRED(2003, "Weight is required", HttpStatus.BAD_REQUEST),

    INVALID_WEIGHT(2004, "Weight must be between 20 and 300 kg with at most 1 decimal", HttpStatus.BAD_REQUEST),

    HEALTH_RECORD_NOT_EXISTED(2005, "Health record not existed", HttpStatus.NOT_FOUND),

    ALLERGEN_IDS_REQUIRED(2006, "Allergen list is required", HttpStatus.BAD_REQUEST),

    ALLERGEN_NOT_EXISTED(2007, "Allergen not existed", HttpStatus.BAD_REQUEST),

    INVALID_DIARY_SERVINGS(2010, "Servings must be between 0.01 and 100, with at most 2 decimals", HttpStatus.BAD_REQUEST),
    INVALID_WATER_AMOUNT(2011, "Water amount must be a positive integer in ml, at most 100000", HttpStatus.BAD_REQUEST),
    INVALID_ACTIVITY(2012, "Activity type is required (max 100 characters); minutes must be between 1 and 1440", HttpStatus.BAD_REQUEST),
    DIARY_ENTRY_NOT_FOUND(2013, "Diary entry not found", HttpStatus.NOT_FOUND),
    MEAL_PLAN_MEAL_NOT_FOUND(2014, "Meal plan or meal not found for the current user and date", HttpStatus.NOT_FOUND),
    DIARY_SOURCE_UNAVAILABLE(2015, "Meal plan snapshot is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST);

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
