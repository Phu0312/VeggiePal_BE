package com.veggiepal.blog.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.blog.enums.ContentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {

    Long id;

    Long authorId;

    Long categoryId;

    String categoryName;

    String title;

    String content;

    String thumbnailUrl;

    ContentStatus status;

    Integer viewCount;

    Integer voteScore;

    LocalDateTime publishedAt;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    /** Only filled when moderation rejected or deferred the content. */
    String moderationReason;
}
