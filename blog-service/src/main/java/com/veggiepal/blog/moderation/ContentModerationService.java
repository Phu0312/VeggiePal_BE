package com.veggiepal.blog.moderation;

/**
 * BR-02: user content goes through moderation before it becomes public.
 *
 * <p>The AI service does not exist yet, so {@link AutoApproveContentModerationService}
 * stands in. Replacing it is the whole extension point: no caller changes.
 */
public interface ContentModerationService {

    ModerationResult moderate(String text);
}
