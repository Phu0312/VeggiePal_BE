package com.veggiepal.ai.common;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> app(AppException exception) {
        ErrorCode error = exception.getErrorCode();
        return ResponseEntity.status(error.status).body(new ApiResponse<>(error.code, error.message, null));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String message = Objects.requireNonNull(exception.getFieldError()).getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiResponse<>(ErrorCode.INVALID_REQUEST.code, message, null));
    }
    @ExceptionHandler(AccessDeniedException.class)
    void accessDenied(AccessDeniedException exception) { throw exception; }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.internalServerError().body(new ApiResponse<>(6999, "Uncategorized error", null));
    }
}
