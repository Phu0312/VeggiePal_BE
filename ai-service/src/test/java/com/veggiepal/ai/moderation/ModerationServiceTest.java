package com.veggiepal.ai.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void deletedTarget404ClosesOrphanCaseInsteadOfStayingReviewRequired() {
        ModerationRepository repository = mock(ModerationRepository.class);
        ModerationOwnerClient owner = mock(ModerationOwnerClient.class);
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "deleted content", "REVIEW_REQUIRED", "spam",
                "needs review", new BigDecimal("0.7"));
        when(repository.findForReview(10L)).thenReturn(Optional.of(item));

        // When blog-service returns 404, ownerClient throws MODERATION_TARGET_NOT_FOUND
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.MODERATION_TARGET_NOT_FOUND))
                .when(owner).apply("BLOG", 42L, 7L, Decision.APPROVED, "admin-token");

        ModerationService service = service(repository, mock(GroqClient.class), owner);

        var response = service.review(1L, 10L, new ReviewRequest(Decision.APPROVED, "approve attempt"), "admin-token");

        assertEquals(Decision.DISMISSED, response.decision());
        assertEquals(false, item.isPending(), "Orphan case must not remain pending");
        assertEquals(1L, item.getReviewedBy());
        org.junit.jupiter.api.Assertions.assertNotNull(item.getReviewedAt());
    }

    @Test
    void transientDownstreamFailurePreservesRetryAndDoesNotDismissCase() {
        ModerationRepository repository = mock(ModerationRepository.class);
        ModerationOwnerClient owner = mock(ModerationOwnerClient.class);
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "content", "REVIEW_REQUIRED", "spam",
                "needs review", new BigDecimal("0.7"));
        when(repository.findForReview(10L)).thenReturn(Optional.of(item));

        // When blog-service has transient error, ownerClient throws DOWNSTREAM_UNAVAILABLE
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE))
                .when(owner).apply("BLOG", 42L, 7L, Decision.APPROVED, "admin-token");

        ModerationService service = service(repository, mock(GroqClient.class), owner);

        AppException ex = assertThrows(AppException.class, () ->
                service.review(1L, 10L, new ReviewRequest(Decision.APPROVED, "approve attempt"), "admin-token"));

        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
        assertEquals(true, item.isPending(), "Transient failure must preserve pending state for retry");
        org.junit.jupiter.api.Assertions.assertNull(item.getReviewedAt());
    }

    @Test
    void adminCanExplicitlyDismissOrphanCase() {
        ModerationRepository repository = mock(ModerationRepository.class);
        ModerationOwnerClient owner = mock(ModerationOwnerClient.class);
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "orphan content", "REVIEW_REQUIRED", "spam",
                "needs review", new BigDecimal("0.7"));
        when(repository.findForReview(10L)).thenReturn(Optional.of(item));

        ModerationService service = service(repository, mock(GroqClient.class), owner);

        var response = service.dismiss(1L, 10L, "Orphan case dismissal");

        assertEquals(Decision.DISMISSED, response.decision());
        assertEquals(false, item.isPending());
        assertEquals(1L, item.getReviewedBy());
        org.mockito.Mockito.verifyNoInteractions(owner);
    }

    @Test
    void adminCanSubmitDismissDecisionInReviewRequest() {
        ModerationRepository repository = mock(ModerationRepository.class);
        ModerationOwnerClient owner = mock(ModerationOwnerClient.class);
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "orphan content", "REVIEW_REQUIRED", "spam",
                "needs review", new BigDecimal("0.7"));
        when(repository.findForReview(10L)).thenReturn(Optional.of(item));

        ModerationService service = service(repository, mock(GroqClient.class), owner);

        var response = service.review(1L, 10L, new ReviewRequest(Decision.DISMISSED, "Manual dismiss"), "admin-token");

        assertEquals(Decision.DISMISSED, response.decision());
        assertEquals(false, item.isPending());
        assertEquals(1L, item.getReviewedBy());
        org.mockito.Mockito.verifyNoInteractions(owner);
    }

    // blog-service moderates in the background: a platform cost, never the author's AI quota
    @Test
    void serviceModerationSkipsTheUserBudgetAndLogsUnderTheServicePrincipal() {
        ModerationRepository repository = mock(ModerationRepository.class);
        GroqClient groq = mock(GroqClient.class);
        UsageService usage = mock(UsageService.class);
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class))).thenReturn(
                new GroqResult("{\"decision\":\"APPROVED\",\"categories\":[],\"reason\":\"ok\",\"confidence\":0.9}", 10, 5));
        when(repository.save(any(ModerationCase.class))).thenAnswer(call -> call.getArgument(0));
        ModerationService service = service(repository, groq, mock(ModerationOwnerClient.class), usage);

        var response = service.moderateForService(
                new ModerationDtos.ModerationRequest(ModerationDtos.ContentType.BLOG, 42L, 7L, "content"));

        assertEquals(Decision.APPROVED, response.decision());
        verify(usage, never()).checkBudget(anyString());
        verify(usage).success(eq(7L), eq("service:moderation"), eq("MODERATION"), any(), any(), anyLong());
    }

    private ModerationService service(ModerationRepository repository, GroqClient groq, ModerationOwnerClient owner) {
        return service(repository, groq, owner, mock(UsageService.class));
    }

    private ModerationService service(ModerationRepository repository, GroqClient groq, ModerationOwnerClient owner,
                                      UsageService usage) {
        return new ModerationService(repository, groq,
                new ModelProperties("c", "m", "r", "p", "v", "w"), new ObjectMapper(),
                usage, owner);
    }
}
