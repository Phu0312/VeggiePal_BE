package com.veggiepal.blog.enums;

public enum CommentStatus {
    /** Unused today; reserved so async AI moderation needs no ALTER TABLE later. */
    PENDING,

    VISIBLE,

    HIDDEN,

    DELETED
}
