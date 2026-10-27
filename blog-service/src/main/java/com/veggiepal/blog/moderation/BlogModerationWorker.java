package com.veggiepal.blog.moderation;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.service.BlogService;

import lombok.extern.slf4j.Slf4j;

/**
 * Task sheet US12: blog moderation runs in the background. No transaction is open while
 * ai-service is called, so a slow AI can no longer hold database connections.
 */
@Component
@Slf4j
public class BlogModerationWorker {

    private final ContentModerationService contentModerationService;
    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final Duration retryInterval;

    public BlogModerationWorker(
            ContentModerationService contentModerationService,
            BlogService blogService,
            BlogRepository blogRepository,
            @Value("${moderation.retry-interval:PT2M}") Duration retryInterval
    ) {
        this.contentModerationService = contentModerationService;
        this.blogService = blogService;
        this.blogRepository = blogRepository;
        this.retryInterval = retryInterval;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModerationRequested(BlogModerationRequested event) {
        moderate(event.blogId(), event.authorId(), event.text());
    }

    // ponytail: retries every interval for as long as AI stays down; add an attempt count or backoff if the backlog grows
    @Scheduled(
            fixedDelayString = "${moderation.retry-interval:PT2M}",
            initialDelayString = "${moderation.retry-interval:PT2M}"
    )
    public void retryStalled() {
        LocalDateTime cutoff = LocalDateTime.now().minus(retryInterval);
        for (Blog blog : blogRepository.findTop20ByStatusAndModerationRequestedAtBeforeOrderByModerationRequestedAtAsc(
                ContentStatus.PENDING, cutoff)) {
            moderate(blog.getId(), blog.getAuthorId(), BlogService.moderationText(blog));
        }
    }

    private void moderate(Long blogId, Long authorId, String text) {
        try {
            ModerationResult result = contentModerationService.moderate("BLOG", blogId, authorId, text);
            blogService.applyAiModeration(blogId, text, result);
        } catch (AppException exception) {
            // Left PENDING with moderation_requested_at set, so retryStalled picks it up
            log.warn("AI moderation of blog {} failed: {}", blogId, exception.getErrorCode());
        }
    }
}
