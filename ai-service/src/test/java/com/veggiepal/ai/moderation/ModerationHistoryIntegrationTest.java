package com.veggiepal.ai.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.veggiepal.ai.moderation.ModerationDtos.ContentType;
import com.veggiepal.ai.moderation.ModerationDtos.Decision;
import com.veggiepal.ai.moderation.ModerationDtos.ModerationResponse;
import com.veggiepal.ai.moderation.ModerationDtos.Reviewer;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Runs the real Specification (coalesce ordering, separate count query) against H2.
@SpringBootTest
class ModerationHistoryIntegrationTest {
    @Autowired ModerationService service;
    @Autowired ModerationRepository cases;

    @BeforeEach void clean() { cases.deleteAll(); }

    ModerationCase save(String type, String decision) {
        return cases.saveAndFlush(new ModerationCase(type, 1L, 2L, "text", decision, "", "r", BigDecimal.ONE));
    }

    @Test
    void historyListsHandledCasesNewestFirstAndSkipsTheAdminQueue() {
        ModerationCase aiApproved = save("BLOG", "APPROVED");
        save("BLOG", "REVIEW_REQUIRED");                       // still in the admin queue
        ModerationCase adminRejected = save("COMMENT", "REVIEW_REQUIRED");
        adminRejected.review(99L, "REJECTED", "spam");
        cases.saveAndFlush(adminRejected);

        assertThat(service.history(null, null, null, null, null, 0, 20).getItems())
                .extracting(ModerationResponse::caseId)
                .containsExactly(adminRejected.getId(), aiApproved.getId());
        assertThat(service.history(null, null, Reviewer.AI, null, null, 0, 20).getItems())
                .extracting(ModerationResponse::caseId).containsExactly(aiApproved.getId());
        assertThat(service.history(ContentType.COMMENT, Decision.REJECTED, Reviewer.ADMIN, null, null, 0, 20)
                .getTotalElements()).isEqualTo(1);
        assertThat(service.history(null, null, null, null, null, -3, 500).getSize()).isEqualTo(100);
    }
}
