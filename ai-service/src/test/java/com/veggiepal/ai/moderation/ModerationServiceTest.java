package com.veggiepal.ai.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.GroqClient.GroqResult;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.moderation.ModerationDtos.Decision;
import com.veggiepal.ai.moderation.ModerationDtos.ReviewRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModerationServiceTest {
    @Test
    void adminDecisionIsAppliedToOwnerBeforeCaseIsFinalized() {
        ModerationRepository repository = mock(ModerationRepository.class);
        ModerationOwnerClient owner = mock(ModerationOwnerClient.class);
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "content", "REVIEW_REQUIRED", "spam",
                "review", new BigDecimal("0.7"));
        when(repository.findForReview(10L)).thenReturn(Optional.of(item));
        ModerationService service = new ModerationService(repository, mock(GroqClient.class),
                new ModelProperties("c", "m", "r", "p", "v", "w"), new ObjectMapper(),
                mock(UsageService.class), owner);

        var response = service.review(1L, 10L, new ReviewRequest(Decision.APPROVED, "ok"), "admin-token");

        verify(owner).apply("BLOG", 42L, 7L, Decision.APPROVED, "admin-token");
        assertEquals(Decision.APPROVED, response.decision());
        assertEquals(1L, item.getReviewedBy());
        assertEquals("content", response.content());
        assertEquals(42L, response.externalContentId());
    }

    @Test
    void staleCaseCannotBeReviewedTwice() {
        ModerationRepository repository = mock(ModerationRepository.class);
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "content", "REVIEW_REQUIRED", "spam",
                "review", new BigDecimal("0.7"));
        item.review(2L, Decision.REJECTED.name(), "already handled");
        when(repository.findForReview(10L)).thenReturn(Optional.of(item));
        ModerationService service = service(repository, mock(GroqClient.class), mock(ModerationOwnerClient.class));

        AppException exception = assertThrows(AppException.class, () ->
                service.review(1L, 10L, new ReviewRequest(Decision.APPROVED, "ok"), "admin-token"));

        assertEquals(ErrorCode.MODERATION_CASE_STALE, exception.getErrorCode());
    }

    @Test
    void repeatedModerationRefreshesOpenCaseInsteadOfCreatingDuplicate() {
        ModerationRepository repository = mock(ModerationRepository.class);
        GroqClient groq = mock(GroqClient.class);
        ModerationCase existing = new ModerationCase("BLOG", 42L, 7L, "old", "REVIEW_REQUIRED", "spam",
                "old reason", new BigDecimal("0.6"));
        when(repository.findFirstByContentTypeAndExternalContentIdAndAuthorIdAndReviewedAtIsNullOrderByCreatedAtDesc(
                "BLOG", 42L, 7L)).thenReturn(Optional.of(existing));
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class))).thenReturn(
                new GroqResult("{\"decision\":\"REVIEW_REQUIRED\",\"categories\":[\"spam\"],\"reason\":\"new reason\",\"confidence\":0.8}", 10, 5));
        when(repository.save(existing)).thenReturn(existing);
        ModerationService service = service(repository, groq, mock(ModerationOwnerClient.class));

        var response = service.moderate(9L,
                new ModerationDtos.ModerationRequest(ModerationDtos.ContentType.BLOG, 42L, 7L, "new content"));

        verify(repository).save(existing);
        assertEquals("new content", response.content());
        assertEquals("new reason", response.reason());
    }

    private ModerationService service(ModerationRepository repository, GroqClient groq, ModerationOwnerClient owner) {
        return new ModerationService(repository, groq,
                new ModelProperties("c", "m", "r", "p", "v", "w"), new ObjectMapper(),
                mock(UsageService.class), owner);
    }
}
