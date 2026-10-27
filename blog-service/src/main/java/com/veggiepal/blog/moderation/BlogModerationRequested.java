package com.veggiepal.blog.moderation;

/**
 * Published inside the transaction that sends a blog to moderation; BlogModerationWorker
 * handles it after that transaction commits. {@code text} is what AI is asked to judge.
 */
public record BlogModerationRequested(Long blogId, Long authorId, String text) {
}
