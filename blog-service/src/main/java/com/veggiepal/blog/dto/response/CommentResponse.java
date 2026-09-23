package com.veggiepal.blog.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {

    Long id;

    Long authorId;

    TargetType targetType;

    Long targetId;

    Long parentCommentId;

    /** Null when the comment was deleted; the row survives so replies keep their thread. */
    String content;

    boolean deleted;

    Long replyCount;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
