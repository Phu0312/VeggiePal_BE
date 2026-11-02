package com.veggiepal.ai.prompt;

import static org.junit.jupiter.api.Assertions.*;

import com.veggiepal.ai.common.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PromptManagementIntegrationTest {
    @Autowired PromptManagementService management;
    @Autowired PromptRuntime runtime;

    @Test
    void draftPublishRestoreAndAudit() {
        PromptKey key = PromptKey.CHAT_SYSTEM;
        String baseline = runtime.activeContent(key);
        var draft = management.createDraft(key, 7L, null);
        assertEquals("DRAFT", draft.status());
        assertEquals(baseline, runtime.activeContent(key));

        String revised = baseline + "\nTrả lời tối đa 100 từ.";
        management.editDraft(key, draft.version(), revised);
        assertEquals(baseline, runtime.activeContent(key));

        var published = management.publish(key, draft.version(), 7L);
        assertEquals("PUBLISHED", published.status());
        assertEquals(revised, runtime.activeContent(key));
        assertEquals(draft.version(), runtime.resolve(key).version());
        assertThrows(AppException.class, () -> management.editDraft(key, draft.version(), "changed"));

        management.restore(key, 0, 8L);
        assertEquals(baseline, runtime.activeContent(key));
        assertEquals(0, runtime.resolve(key).version());
        assertEquals(2, management.activationHistory(key).size());
        assertEquals(8L, management.activationHistory(key).getFirst().getActivatedBy());
    }

    @Test
    void publishedVersionIsImmutableAndDraftCannotBeActivated() {
        PromptKey key = PromptKey.MODERATION_USER;
        var draft = management.createDraft(key, 7L, null);
        assertThrows(AppException.class, () -> management.restore(key, draft.version(), 7L));
        management.publish(key, draft.version(), 7L);
        assertThrows(AppException.class, () -> management.publish(key, draft.version(), 7L));
    }
}
