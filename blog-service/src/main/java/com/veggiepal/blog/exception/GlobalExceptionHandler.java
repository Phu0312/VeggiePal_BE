package com.veggiepal.blog.exception;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.veggiepal.blog.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    private static final String MAX_ATTRIBUTE = "max";

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingException(
            Exception exception
    ) {

        log.error("Exception: ", exception);

        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    // @PreAuthorize denials (AuthorizationDeniedException) are thrown by the method-security
    // AOP proxy while DispatcherServlet is still invoking the handler, so without this they
    // would be caught by handlingException above before Spring Security's
    // ExceptionTranslationFilter ever sees them, turning a 403 into a generic 500.
    // Rethrowing the exact same exception is the documented escape hatch: Spring detects
    // invocationEx == exception, treats this resolver as non-resolving, and lets the
    // exception propagate out of DispatcherServlet to the filter chain, where
    // SecurityExceptionHandler.handle() produces the correct 403/1009 response.
    @ExceptionHandler(value = AccessDeniedException.class)
    void handlingAccessDenied(
            AccessDeniedException exception
    ) throws AccessDeniedException {

        throw exception;
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<?>> handlingDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {

        // Never log the exception/message here: a unique-constraint message leaks
        // which user voted on which content (e.g. "Duplicate entry '7-BLOG-12'").
        log.warn("Data integrity violation");

        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(
            AppException exception
    ) {

        return errorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(value = {
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiResponse<?>> handlingInvalidRequest(
            Exception exception
    ) {

        return errorResponse(ErrorCode.INVALID_REQUEST);
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

    private ResponseEntity<ApiResponse<?>> errorResponse(
            ErrorCode errorCode
    ) {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    private String mapAttribute(
            String message,
            Map<String, Object> attributes
    ) {

        String mapped = message;

        for (String attribute : new String[]{MIN_ATTRIBUTE, MAX_ATTRIBUTE}) {

            Object value = attributes.get(attribute);

            if (value != null) {
                mapped = mapped.replace("{" + attribute + "}", String.valueOf(value));
            }
        }

        return mapped;
    }
}
