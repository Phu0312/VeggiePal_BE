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
    HEALTH_RECORD_REQUIRED(6010, "Vui lòng cập nhật chiều cao và cân nặng trong hồ sơ sức khỏe trước khi lập thực đơn.", HttpStatus.BAD_REQUEST),
    NO_VALID_RECIPES(6011, "Không tìm thấy công thức món ăn phù hợp với yêu cầu dinh dưỡng và dị ứng.", HttpStatus.BAD_REQUEST),
    MODERATION_TARGET_NOT_FOUND(6012, "Moderated content no longer exists", HttpStatus.NOT_FOUND),
    MODERATION_CASE_STALE(6013, "Moderation case is no longer pending", HttpStatus.CONFLICT),
    FAVORITE_ALREADY_EXISTS(6014, "Thực đơn này đã có trong danh sách yêu thích.", HttpStatus.CONFLICT),
    FAVORITE_NOT_FOUND(6015, "Thực đơn yêu thích không tồn tại.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(6016, "Bạn không có quyền truy cập thực đơn này.", HttpStatus.FORBIDDEN),
    VERSION_NOT_FOUND(6017, "Phiên bản thực đơn không tồn tại.", HttpStatus.NOT_FOUND),
    RECIPE_NOT_VEGAN(6018, "Món ăn được chọn không phải món thuần chay.", HttpStatus.BAD_REQUEST),
    RECIPE_CONTAINS_ALLERGEN(6019, "Món ăn chứa thành phần gây dị ứng của bạn.", HttpStatus.BAD_REQUEST),
    VIDEO_QUEUE_FULL(6020, "Video processing queue is full", HttpStatus.SERVICE_UNAVAILABLE),
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
