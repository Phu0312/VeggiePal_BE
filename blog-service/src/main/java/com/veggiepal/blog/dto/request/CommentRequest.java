package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentRequest {

    @NotNull(message = "UNSUPPORTED_TARGET_TYPE")
    TargetType targetType;

    @NotNull(message = "COMMENT_TARGET_NOT_EXISTED")
    Long targetId;

    /** Null for a top-level comment. Must point at a top-level comment otherwise. */
    Long parentCommentId;

    @NotBlank(message = "COMMENT_CONTENT_REQUIRED")
    @Size(max = 2000, message = "INVALID_COMMENT_CONTENT")
    String content;
}
