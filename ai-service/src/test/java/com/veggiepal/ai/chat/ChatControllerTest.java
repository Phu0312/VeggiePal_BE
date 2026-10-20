package com.veggiepal.ai.chat;

import static org.mockito.ArgumentMatchers.any;
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
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.10");
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ChatController(chatService, guestSessions, clientIpResolver)).build();
    }

    @Test
    void createGuestSessionUsesResolvedClientIp() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expires = LocalDateTime.now().plusHours(24);
        when(guestSessions.create("203.0.113.10"))
                .thenReturn(new GuestSession(sessionId, "hash", LocalDateTime.now(), expires));

        mockMvc.perform(post("/ai/chat/guest-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.guestSession").value(sessionId));

        verify(guestSessions).create("203.0.113.10");
    }

    @Test
    void guestChatPassesResolvedClientIp() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ChatDtos.ChatReply reply = new ChatDtos.ChatReply(null, "answer", 2, 10, 15);
        when(chatService.guest(sessionId, "vegan protein", "203.0.113.10")).thenReturn(reply);

        ChatDtos.GuestChatRequest request = new ChatDtos.GuestChatRequest(sessionId, "vegan protein");
        mockMvc.perform(post("/ai/chat/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.answer").value("answer"))
                .andExpect(jsonPath("$.result.remainingRequests").value(2));

        verify(chatService).guest(sessionId, "vegan protein", "203.0.113.10");
    }

    @Test
    void guestChatRejectsInvalidSessionFormat() throws Exception {
        ChatDtos.GuestChatRequest request = new ChatDtos.GuestChatRequest("too-short", "Hello");
        mockMvc.perform(post("/ai/chat/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
