package com.veggiepal.blog.moderation;

public interface ContentModerationService {

    ModerationResult moderate(
            String contentType,
            Long externalContentId,
            Long authorId,
            String text
    );
}
