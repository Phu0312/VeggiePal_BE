package com.veggiepal.ai.prompt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptTemplateTest {
    @Test
    void everyPromptHasAUniqueKeyAndValidDefault() {
        assertEquals(8, PromptKey.values().length);
        for (PromptKey key : PromptKey.values()) {
            assertFalse(key.defaultContent().isBlank());
            assertDoesNotThrow(() -> PromptTemplate.validate(key, key.defaultContent()));
        }
    }

    @Test
    void dynamicInputsMustRemainPresentAndUnknownInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PromptTemplate.validate(PromptKey.MODERATION_USER, "No content variable"));
        assertThrows(IllegalArgumentException.class,
                () -> PromptTemplate.validate(PromptKey.CHAT_SYSTEM, "{{unknown}}"));
    }

    @Test
    void renderingReplacesOnlyDeclaredVariables() {
        String value = PromptTemplate.render(PromptKey.MODERATION_USER,
                PromptKey.MODERATION_USER.defaultContent(), Map.of("content", "sample"));
        assertTrue(value.contains("sample"));
        assertFalse(value.contains("{{content}}"));
    }
}
