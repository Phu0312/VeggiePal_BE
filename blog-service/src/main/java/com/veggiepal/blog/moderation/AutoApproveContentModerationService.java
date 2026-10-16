package com.veggiepal.blog.moderation;

/** Test utility retained for callers that explicitly construct it. Not registered in production. */
public class AutoApproveContentModerationService implements ContentModerationService {

    @Override
    public ModerationResult moderate(String contentType, Long externalContentId, Long authorId, String text) {
        return ModerationResult.approved();
    }
}
