package com.veggiepal.ai.chat;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ChatDtos {
    private ChatDtos() {}
    public record ChatRequest(Long conversationId, @NotBlank(message="Message is required") @Size(max=4000, message="Message is too long") String message) {}
    public record GuestChatRequest(@NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{16,100}$", message="Invalid guest session") String guestSession,
                                   @NotBlank(message="Message is required") @Size(max=4000, message="Message is too long") String message) {}
    public record GuestSessionResponse(String guestSession, LocalDateTime expiresAt) {}
    public record ChatReply(Long conversationId, String answer, int remainingRequests, int promptTokens, int completionTokens) {}
    public record ConversationSummary(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record MessageView(Long id, String role, String content, LocalDateTime createdAt) {}
    public record ConversationDetail(ConversationSummary conversation, List<MessageView> messages) {}
}
