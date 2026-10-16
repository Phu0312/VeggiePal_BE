package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Bound from multipart form fields on create, and from a JSON body on edit. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoRequest {

    @NotBlank(message = "VIDEO_TITLE_REQUIRED")
    @Size(max = 150, message = "INVALID_VIDEO_TITLE")
    String title;

    @Size(max = 5000, message = "INVALID_VIDEO_DESCRIPTION")
    String description;

    // Not CATEGORY_NOT_EXISTED: that code is a 404, and a missing field is a 400
    @NotNull(message = "CATEGORY_ID_REQUIRED")
    Long categoryId;

    /** Create only: the YouTube source, instead of a file. An edit ignores it. */
    String youtubeUrl;

    /** Create only: true runs moderation right away. An edit ignores it, like BlogRequest. */
    Boolean publish;
}
