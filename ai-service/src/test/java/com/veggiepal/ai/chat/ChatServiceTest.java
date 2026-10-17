package com.veggiepal.ai.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.quota.QuotaProperties;
import com.veggiepal.ai.quota.QuotaService;
import org.junit.jupiter.api.Test;

class ChatServiceTest {
    @Test
    void guestChatUsesQuotaAndReturnsProviderUsage() {
        QuotaService quota=mock(QuotaService.class);GroqClient groq=mock(GroqClient.class);UsageService usage=mock(UsageService.class);
        when(quota.reserveChat("guest:abcdefghijklmnop",true)).thenReturn(2);
        when(groq.complete(eq("chat-model"),anyList(),eq(0.4),eq(700))).thenReturn(new GroqClient.GroqResult("answer",10,5));
        GuestSessionService guestSessions=mock(GuestSessionService.class);
        ChatService service=new ChatService(mock(ChatConversationRepository.class),mock(ChatMessageRepository.class),quota,
                new QuotaProperties(3,50,12,4000),groq,new ModelProperties("chat-model","m","r","p","v","w"),usage,guestSessions);
        var result=service.guest("abcdefghijklmnop","  protein?  ");
        assertEquals("answer",result.answer());assertEquals(2,result.remainingRequests());assertEquals(10,result.promptTokens());
        verify(guestSessions).requireValid("abcdefghijklmnop");
        verify(usage).success(eq(null),eq("guest:abcdefghijklmnop"),eq("CHAT"),eq("chat-model"),any(),anyLong());
    }
}
