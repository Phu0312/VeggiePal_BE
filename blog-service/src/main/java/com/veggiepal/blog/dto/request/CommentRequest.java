package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.validation.MaxWords;

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
    // Task sheet US5: at most 500 words.
    @MaxWords(max = 500, message = "INVALID_COMMENT_CONTENT")
    // Word count alone does not bound length — one 100KB "word" is one word. This ceiling
    // keeps it from reaching the TEXT column and failing there as a 500.
    @Size(max = 5000, message = "COMMENT_TOO_LONG")
    String content;
}
