package com.veggiepal.ai.chat;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
