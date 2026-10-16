package com.veggiepal.ai.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(6000, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(6001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    RESOURCE_NOT_FOUND(6002, "Resource not found", HttpStatus.NOT_FOUND),
    QUOTA_EXCEEDED(6003, "AI daily quota exceeded", HttpStatus.TOO_MANY_REQUESTS),
    GROQ_UNAVAILABLE(6004, "AI provider is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GROQ_NOT_CONFIGURED(6005, "AI provider is not configured", HttpStatus.SERVICE_UNAVAILABLE),
    DOWNSTREAM_UNAVAILABLE(6006, "Required domain service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_AI_RESPONSE(6007, "AI provider returned an invalid response", HttpStatus.BAD_GATEWAY),
    VIDEO_PROCESSING_FAILED(6008, "Video processing failed", HttpStatus.UNPROCESSABLE_ENTITY),
    FILE_TOO_LARGE(6009, "Uploaded file is too large", HttpStatus.PAYLOAD_TOO_LARGE),
    UNCATEGORIZED(6999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    public final int code;
    public final String message;
    public final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
