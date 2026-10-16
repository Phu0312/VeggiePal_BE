package com.veggiepal.ai.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.groq.GroqClient;
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
        ModerationCase item = new ModerationCase("BLOG", 42L, 7L, "REVIEW_REQUIRED", "spam",
                "review", new BigDecimal("0.7"));
        when(repository.findById(10L)).thenReturn(Optional.of(item));
        ModerationService service = new ModerationService(repository, mock(GroqClient.class),
                new ModelProperties("c", "m", "r", "p", "v", "w"), new ObjectMapper(),
                mock(UsageService.class), owner);

        var response = service.review(1L, 10L, new ReviewRequest(Decision.APPROVED, "ok"), "admin-token");

        verify(owner).apply("BLOG", 42L, 7L, Decision.APPROVED, "admin-token");
        assertEquals(Decision.APPROVED, response.decision());
        assertEquals(1L, item.getReviewedBy());
    }
}
