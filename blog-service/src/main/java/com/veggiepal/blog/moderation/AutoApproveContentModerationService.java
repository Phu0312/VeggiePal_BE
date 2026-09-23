package com.veggiepal.blog.moderation;

import org.springframework.stereotype.Service;

/** Placeholder until the AI moderation service exists. Approves everything. */
@Service
public class AutoApproveContentModerationService implements ContentModerationService {

    @Override
    public ModerationResult moderate(String text) {
        return ModerationResult.approved();
    }
}
