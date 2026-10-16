package com.veggiepal.ai.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T result) {
    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(1000, null, result);
    }
}
