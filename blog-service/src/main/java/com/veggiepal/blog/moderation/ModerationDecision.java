package com.veggiepal.blog.moderation;

public enum ModerationDecision {
    APPROVED,
    REJECTED,
    /** The AI implementation may need to answer later; content waits instead of going public. */
    PENDING
}
