package com.veggiepal.blog.moderation;

public record ModerationResult(ModerationDecision decision, String reason) {

    public static ModerationResult approved() {
        return new ModerationResult(ModerationDecision.APPROVED, null);
    }
}
