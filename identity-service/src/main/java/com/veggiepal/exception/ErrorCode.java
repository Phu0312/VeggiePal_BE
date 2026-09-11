package com.veggiepal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(
            9999,
            "Uncategorized error",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    INVALID_KEY(
            1001,
            "Invalid validation key",
            HttpStatus.BAD_REQUEST
    ),

    EMAIL_EXISTED(
            1002,
            "Email already existed",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_PASSWORD(
            1003,
            "Password must be at least {min} characters",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_EMAIL(
            1004,
            "Email is invalid",
            HttpStatus.BAD_REQUEST
    ),

    EMAIL_REQUIRED(
            1005,
            "Email is required",
            HttpStatus.BAD_REQUEST
    ),

    FULL_NAME_REQUIRED(
            1006,
            "Full name is required",
            HttpStatus.BAD_REQUEST
    ),

    USER_NOT_EXISTED(
            1007,
            "User not existed",
            HttpStatus.NOT_FOUND
    ),

    UNAUTHENTICATED(
            1008,
            "Unauthenticated",
            HttpStatus.UNAUTHORIZED
    ),

    UNAUTHORIZED(
            1009,
            "You do not have permission",
            HttpStatus.FORBIDDEN
    );

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