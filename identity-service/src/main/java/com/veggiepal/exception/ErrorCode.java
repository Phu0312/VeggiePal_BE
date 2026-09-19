package com.veggiepal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),

    EMAIL_EXISTED(1002, "Email already existed", HttpStatus.BAD_REQUEST),

    INVALID_PASSWORD(1003, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),

    INVALID_EMAIL(1004, "Email is invalid", HttpStatus.BAD_REQUEST),

    EMAIL_REQUIRED(1005, "Email is required", HttpStatus.BAD_REQUEST),

    FULL_NAME_REQUIRED(1006, "Full name is required", HttpStatus.BAD_REQUEST),

    USER_NOT_EXISTED(1007, "User not existed", HttpStatus.NOT_FOUND),

    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    PASSWORD_REQUIRED(1010, "Password is required", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    INVALID_DATE_OF_BIRTH(1013, "Date of birth must be in the past", HttpStatus.BAD_REQUEST),

    WRONG_PASSWORD(1011, "Current password is incorrect", HttpStatus.BAD_REQUEST),

    PASSWORD_UNCHANGED(1012, "New password must be different from current password", HttpStatus.BAD_REQUEST),

    FILE_UPLOAD_FAILED(1017, "Could not upload file, please try again later", HttpStatus.SERVICE_UNAVAILABLE),

    AVATAR_REQUIRED(1014, "Avatar file is required", HttpStatus.BAD_REQUEST),

    INVALID_AVATAR_TYPE(1015, "Avatar must be a JPEG, PNG or WEBP image", HttpStatus.BAD_REQUEST),

    AVATAR_TOO_LARGE(1016, "Avatar must not exceed 2MB", HttpStatus.BAD_REQUEST),

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