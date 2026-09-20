package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogRequest {

    @NotBlank(message = "BLOG_TITLE_REQUIRED")
    @Size(max = 200, message = "INVALID_BLOG_TITLE")
    String title;

    @NotBlank(message = "BLOG_CONTENT_REQUIRED")
    @Size(min = 20, message = "INVALID_BLOG_CONTENT")
    String content;

    // Not CATEGORY_NOT_EXISTED: that code carries HTTP 404, and a missing field
    // in the request body is a 400. The handler takes the status from the code.
    @NotNull(message = "CATEGORY_ID_REQUIRED")
    Long categoryId;

    /**
     * false (or absent) saves a draft. true runs moderation right away.
     * The client never sends a status: that would be a way around BR-02.
     */
    Boolean publish;
}
