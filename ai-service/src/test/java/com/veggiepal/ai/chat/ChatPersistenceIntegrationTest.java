package com.veggiepal.ai.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.veggiepal.ai.groq.GroqClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ChatPersistenceIntegrationTest {
    @Autowired ChatService service;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatConversationRepository conversations;
    @MockitoBean GroqClient groq;

    @BeforeEach
    void clean() {
        messages.deleteAll();
        conversations.deleteAll();
    }

    @Test
    void memberConversationPersistsAndContinues() {
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult("Câu trả lời 1", 12, 5))
                .thenReturn(new GroqClient.GroqResult("Câu trả lời 2", 20, 6));

        var first=service.member(42L,new ChatDtos.ChatRequest(null,"Tôi cần thêm protein"));
        var second=service.member(42L,new ChatDtos.ChatRequest(first.conversationId(),"Gợi ý bữa sáng"));
        var detail=service.detail(42L,second.conversationId());

        assertEquals(first.conversationId(),second.conversationId());
        assertEquals(4,detail.messages().size());
        assertEquals("user",detail.messages().getFirst().role());
        assertEquals("Câu trả lời 2",detail.messages().getLast().content());
        verify(groq,times(2)).complete(anyString(),anyList(),anyDouble(),anyInt());
    }
}
