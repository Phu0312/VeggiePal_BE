package com.veggiepal.payment.exception;

import com.veggiepal.payment.dto.response.ApiResponse;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        return response(exception.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        ErrorCode error;
        try { error = ErrorCode.valueOf(Objects.requireNonNull(exception.getFieldError()).getDefaultMessage()); }
        catch (Exception ignored) { error = ErrorCode.INVALID_KEY; }
        return response(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return response(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        return response(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    void handleAccessDenied(AccessDeniedException exception) { throw exception; }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception exception) {
        log.error("Unhandled request exception", exception);
        return response(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode error) {
        return ResponseEntity.status(error.getStatusCode()).body(ApiResponse.<Void>builder()
                .code(error.getCode()).message(error.getMessage()).build());
    }
}
