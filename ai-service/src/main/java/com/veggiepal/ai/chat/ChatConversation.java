package com.veggiepal.ai.chat;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_conversations")
@Getter
@NoArgsConstructor
public class ChatConversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 160) private String title;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public ChatConversation(Long userId, String title) { this.userId=userId; this.title=title; this.createdAt=LocalDateTime.now(); this.updatedAt=createdAt; }
    public void touch() { updatedAt=LocalDateTime.now(); }
}
