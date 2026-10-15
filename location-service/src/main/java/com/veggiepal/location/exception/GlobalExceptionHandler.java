package com.veggiepal.location.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.veggiepal.location.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingException(
            Exception exception
    ) {

        log.error("Exception: ", exception);

        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(
            AppException exception
    ) {

        return errorResponse(exception.getErrorCode());
    }

    // A missing lat/lng, or one that is not a number, is a client error rather than the 500
    // the catch-all above would give it.
    @ExceptionHandler(value = {
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiResponse<?>> handlingInvalidRequest(
            Exception exception
    ) {

        return errorResponse(ErrorCode.INVALID_REQUEST);
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
}
