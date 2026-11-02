package com.veggiepal.ai.chat;

import static com.veggiepal.ai.chat.ChatDtos.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.prompt.PromptKey;
import com.veggiepal.ai.prompt.PromptRuntime;
import com.veggiepal.ai.quota.QuotaProperties;
import com.veggiepal.ai.quota.QuotaService;
import com.veggiepal.ai.quota.PremiumQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String SYSTEM_PROMPT = PromptKey.CHAT_SYSTEM.defaultContent();
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final QuotaService quota;
    private final PremiumQuotaService premiumQuota;
    private final QuotaProperties quotaProperties;
    private final GroqClient groq;
    private final ModelProperties models;
    private final UsageService usage;
    private final GuestSessionService guestSessions;
    private final ChatPersistenceService persistence;
    @Autowired(required = false)
    private PromptRuntime promptRuntime;

    private String systemPrompt() {
        return promptRuntime == null ? SYSTEM_PROMPT : promptRuntime.activeContent(PromptKey.CHAT_SYSTEM);
    }

    public ChatReply guest(String guestSession, String text, String clientIp) {
        GuestSession session = guestSessions.requireValid(guestSession, clientIp);
        String principal = "guest-ip:" + session.getIpHash();
        int remaining = quota.reserveChat(principal, true);
        var prompts = List.of(new GroqRequest.Message("system", systemPrompt()), new GroqRequest.Message("user", text.trim()));
        var result = call(null, principal, prompts);
        return new ChatReply(null, result.content(), remaining, result.promptTokens(), result.completionTokens());
    }

    public ChatReply member(Long userId, ChatRequest request) {
        String principal = "user:" + userId;
        int remaining = premiumQuota.reserveChat(userId);
        ChatConversation conversation = request.conversationId() == null
                ? conversations.save(new ChatConversation(userId, title(request.message())))
                : conversations.findByIdAndUserId(request.conversationId(), userId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        List<ChatMessage> history = messages.findByConversationIdOrderByCreatedAtDesc(conversation.getId(),
                PageRequest.of(0, quotaProperties.maxHistoryMessages()));
        Collections.reverse(history);
        List<GroqRequest.Message> prompts = new ArrayList<>();
        prompts.add(new GroqRequest.Message("system", systemPrompt()));
        history.forEach(m -> prompts.add(new GroqRequest.Message(m.getRole(), m.getContent())));
        prompts.add(new GroqRequest.Message("user", request.message().trim()));
        var result = call(userId, principal, prompts);
        return persistence.saveExchange(conversation, principal, request.message().trim(), result, remaining);
    }

    private GroqClient.GroqResult call(Long userId, String principal, List<GroqRequest.Message> prompts) {
        long start = System.nanoTime();
        try {
            var result = groq.complete(models.chat(), prompts, 0.4, 700);
            usage.success(userId, principal, "CHAT", models.chat(), result, elapsed(start));
            return result;
        } catch (RuntimeException exception) {
            usage.failure(userId, principal, "CHAT", models.chat(), elapsed(start), exception);
            throw exception;
        }
    }
    public List<ConversationSummary> list(Long userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::summary).toList();
    }
    @Transactional(readOnly = true)
    public ConversationDetail detail(Long userId, Long id) {
        ChatConversation c = conversations.findByIdAndUserId(id, userId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        var items = messages.findByConversationIdOrderByCreatedAtAsc(id).stream()
                .map(m -> new MessageView(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt())).toList();
        return new ConversationDetail(summary(c), items);
    }
    @Transactional
    public void delete(Long userId, Long id) {
        ChatConversation conversation = conversations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        conversations.delete(conversation);
    }
    private ConversationSummary summary(ChatConversation c) { return new ConversationSummary(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt()); }
    private String title(String text) { String clean=text.trim(); return clean.substring(0, Math.min(80, clean.length())); }
    private long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
}
