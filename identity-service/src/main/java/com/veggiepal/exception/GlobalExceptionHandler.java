package com.veggiepal.exception;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.veggiepal.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingException(
            Exception exception
    ) {

        log.error("Exception: ", exception);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(
            AppException exception
    ) {

        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidation(
            MethodArgumentNotValidException exception
    ) {

        String enumKey =
                exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode =
                ErrorCode.INVALID_KEY;

        Map<String, Object> attributes = null;

        try {

            errorCode =
                    ErrorCode.valueOf(enumKey);

            ConstraintViolation<?> constraintViolation =
                    exception
                            .getBindingResult()
                            .getAllErrors()
                            .getFirst()
                            .unwrap(ConstraintViolation.class);

            attributes =
                    constraintViolation
                            .getConstraintDescriptor()
                            .getAttributes();

        } catch (IllegalArgumentException ignored) {

        }

        ApiResponse<?> apiResponse =
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(
                                Objects.nonNull(attributes)
                                        ? mapAttribute(
                                        errorCode.getMessage(),
                                        attributes
                                )
                                        : errorCode.getMessage()
                        )
                        .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    private String mapAttribute(
            String message,
            Map<String, Object> attributes
    ) {

        String minValue =
                String.valueOf(
                        attributes.get(MIN_ATTRIBUTE)
                );

        return message.replace(
                "{" + MIN_ATTRIBUTE + "}",
                minValue
        );
    }
}