package com.veggiepal.recipe.exception;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.veggiepal.recipe.dto.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handleAppException(
            AppException exception
    ) {

        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> response = ApiResponse
                .<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        String enumKey = Objects.requireNonNull(
                exception
                        .getFieldError()
        ).getDefaultMessage();

        ErrorCode errorCode;

        try {

            errorCode = ErrorCode.valueOf(enumKey);

        } catch (Exception e) {

            errorCode = ErrorCode.INVALID_REQUEST;
        }

        ApiResponse<Void> response = ApiResponse
                .<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    void handleAccessDenied(
            AccessDeniedException exception
    ) {

        throw exception;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception
    ) {

        ErrorCode errorCode =
                ErrorCode.UNCATEGORIZED_EXCEPTION;

        ApiResponse<Void> response = ApiResponse
                .<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(response);
    }
}