package com.veggiepal.blog.enums;

public enum ContentStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED,

    /**
     * Taken down by an admin. Hidden from every public read, but kept so the owner still
     * sees it in their own list. Terminal: neither editing nor submitting lifts it.
     */
    BANNED
}
