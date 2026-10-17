package com.veggiepal.ai.chat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerTest {

    private MockMvc mockMvc;
    private ChatService chatService;
    private GuestSessionService guestSessions;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        guestSessions = mock(GuestSessionService.class);
        mapper = new ObjectMapper();

        ChatController controller = new ChatController(chatService, guestSessions);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createGuestSessionReturnsUuidAndExpiresAt() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expires = LocalDateTime.now().plusHours(24);
        GuestSession session = new GuestSession(sessionId, LocalDateTime.now(), expires);

        when(guestSessions.create()).thenReturn(session);

        mockMvc.perform(post("/ai/chat/guest-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.guestSession").value(sessionId));

        verify(guestSessions).create();
    }

    @Test
    void guestChatCallsServiceAndReturnsReply() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ChatDtos.ChatReply reply = new ChatDtos.ChatReply(null, "Xin chào! VeggiePal sẵn sàng tư vấn.", 2, 10, 15);
        when(chatService.guest(eq(sessionId), eq("Tôi muốn ăn chay")))
                .thenReturn(reply);

        ChatDtos.GuestChatRequest request = new ChatDtos.GuestChatRequest(sessionId, "Tôi muốn ăn chay");

        mockMvc.perform(post("/ai/chat/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.answer").value("Xin chào! VeggiePal sẵn sàng tư vấn."))
                .andExpect(jsonPath("$.result.remainingRequests").value(2));

        verify(chatService).guest(sessionId, "Tôi muốn ăn chay");
    }

    @Test
    void guestChatRejectsInvalidSessionFormat() throws Exception {
        // Less than 16 chars violates @Pattern(regexp="^[A-Za-z0-9_-]{16,100}$")
        ChatDtos.GuestChatRequest request = new ChatDtos.GuestChatRequest("too-short", "Hello");

        mockMvc.perform(post("/ai/chat/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
