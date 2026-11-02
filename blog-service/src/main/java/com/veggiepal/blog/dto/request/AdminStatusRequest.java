package com.veggiepal.blog.dto.request;

import com.veggiepal.blog.enums.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminStatusRequest(@NotNull(message = "INVALID_REQUEST") ContentStatus status,
                                 @NotBlank(message = "INVALID_REQUEST")
                                 @Size(max = 1000, message = "INVALID_REQUEST") String reason) {
}
