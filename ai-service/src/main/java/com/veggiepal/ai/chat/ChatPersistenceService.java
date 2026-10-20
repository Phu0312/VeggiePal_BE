package com.veggiepal.ai.chat;

import com.veggiepal.ai.chat.ChatDtos.ChatReply;
import com.veggiepal.ai.groq.GroqClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatPersistenceService {
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;

    @Transactional
    public ChatReply saveExchange(ChatConversation conversation, String principal, String userText,
            GroqClient.GroqResult result, int remaining) {
        ChatConversation saved = conversations.save(conversation);
        messages.save(new ChatMessage(saved, principal, "user", userText, result.promptTokens(), 0));
        messages.save(new ChatMessage(saved, principal, "assistant", result.content(), 0, result.completionTokens()));
        saved.touch();
        conversations.save(saved);
        return new ChatReply(saved.getId(), result.content(), remaining,
                result.promptTokens(), result.completionTokens());
    }
}
