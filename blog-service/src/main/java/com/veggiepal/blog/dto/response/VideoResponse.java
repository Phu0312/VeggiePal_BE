package com.veggiepal.blog.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.enums.VideoSource;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoResponse {

    Long id;

    Long authorId;

    Long categoryId;

    String categoryName;

    String title;

    String description;

    VideoSource sourceType;

    /** YOUTUBE: an embed URL for an iframe. UPLOAD: an .mp4 URL for a video tag. */
    String videoUrl;

    String thumbnailUrl;

    ContentStatus status;

    Integer viewCount;

    LocalDateTime publishedAt;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    /** Only filled when moderation rejected or deferred the video. */
    String moderationReason;
}
