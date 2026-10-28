package com.veggiepal.location.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Shared codes: keep the same numbers as the other services
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST),

    // Restaurant search
    INVALID_COORDINATES(5001, "Latitude must be within [-90, 90] and longitude within [-180, 180]", HttpStatus.BAD_REQUEST),

    INVALID_SEARCH_RADIUS(5002, "Search radius must be greater than 0 and at most 10 km", HttpStatus.BAD_REQUEST),

    INVALID_SEARCH_KEYWORD(5003, "Search keyword must be 2 to 100 characters", HttpStatus.BAD_REQUEST);

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
