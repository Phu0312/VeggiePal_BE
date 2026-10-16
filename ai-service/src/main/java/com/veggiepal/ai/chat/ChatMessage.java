package com.veggiepal.ai.chat;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@NoArgsConstructor
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id") private ChatConversation conversation;
    @Column(name = "principal_key", nullable = false, length = 100) private String principalKey;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "prompt_tokens", nullable = false) private int promptTokens;
    @Column(name = "completion_tokens", nullable = false) private int completionTokens;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public ChatMessage(ChatConversation conversation, String principalKey, String role, String content, int promptTokens, int completionTokens) {
        this.conversation=conversation; this.principalKey=principalKey; this.role=role; this.content=content;
        this.promptTokens=promptTokens; this.completionTokens=completionTokens; this.createdAt=LocalDateTime.now();
    }
}
