package com.veggiepal.ai.prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.veggiepal.ai.groq.GroqClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PromptPreviewIntegrationTest {
    @Autowired PromptManagementService management;
    @Autowired PromptPreviewService preview;
    @Autowired PromptRuntime runtime;
    @MockitoBean GroqClient groq;

    @Test
    void previewUsesDraftWithoutPublishingIt() {
        PromptKey key = PromptKey.RECOMMENDATION;
        String active = runtime.activeContent(key);
        var draft = management.createDraft(key, 9L, null);
        management.editDraft(key, draft.version(), draft.content() + " Chỉ chọn món chay.");
        when(groq.complete(eq("test-recommendation"), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult("[{\"type\":\"RECIPE\",\"id\":1}]", 20, 10));

        var result = preview.preview(key, draft.version(), null, Map.of(
                "limit", "1", "interest", "protein", "candidates", "[]"));

        assertTrue(result.jsonShapeValid());
        assertEquals(active, runtime.activeContent(key));
        verify(groq, times(1)).complete(eq("test-recommendation"), anyList(), anyDouble(), anyInt());
    }
}
