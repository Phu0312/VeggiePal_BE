package com.veggiepal.blog.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.service.BlogService;

@ExtendWith(MockitoExtension.class)
class BlogModerationWorkerTest {

    @Mock
    ContentModerationService contentModerationService;

    @Mock
    BlogService blogService;

    @Mock
    BlogRepository blogRepository;

    BlogModerationWorker worker;

    @BeforeEach
    void setUp() {
        worker = new BlogModerationWorker(contentModerationService, blogService, blogRepository, Duration.ofMinutes(2));
    }

    @Test
    void onModerationRequested_appliesTheVerdict() {
        when(contentModerationService.moderate("BLOG", 10L, 7L, "t\nc")).thenReturn(ModerationResult.approved());

        worker.onModerationRequested(new BlogModerationRequested(10L, 7L, "t\nc"));

        verify(blogService).applyAiModeration(10L, "t\nc", ModerationResult.approved());
    }

    // The request already answered 200; an outage just leaves the post PENDING for retryStalled
    @Test
    void onModerationRequested_aiUnavailable_leavesTheBlogForTheRetryJob() {
        when(contentModerationService.moderate("BLOG", 10L, 7L, "t\nc"))
                .thenThrow(new AppException(ErrorCode.MODERATION_UNAVAILABLE));

        assertThatCode(() -> worker.onModerationRequested(new BlogModerationRequested(10L, 7L, "t\nc")))
                .doesNotThrowAnyException();

        verify(blogService, never()).applyAiModeration(any(), any(), any());
    }

    @Test
    void retryStalled_remoderatesPostsWaitingLongerThanTheInterval() {
        Blog stalled = Blog.builder()
                .id(10L).authorId(7L).title("t").content("c")
                .status(ContentStatus.PENDING)
                .moderationRequestedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(blogRepository.findTop20ByStatusAndModerationRequestedAtBeforeOrderByModerationRequestedAtAsc(
                eq(ContentStatus.PENDING), any(LocalDateTime.class))).thenReturn(List.of(stalled));
        when(contentModerationService.moderate("BLOG", 10L, 7L, "t\nc")).thenReturn(ModerationResult.approved());

        LocalDateTime before = LocalDateTime.now();
        worker.retryStalled();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(blogRepository).findTop20ByStatusAndModerationRequestedAtBeforeOrderByModerationRequestedAtAsc(
                eq(ContentStatus.PENDING), cutoff.capture());
        assertThat(cutoff.getValue()).isBetween(before.minusMinutes(2), after.minusMinutes(2));
        verify(blogService).applyAiModeration(10L, "t\nc", ModerationResult.approved());
    }
}
