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
import com.veggiepal.ai.quota.QuotaProperties;
import com.veggiepal.ai.quota.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý dinh dưỡng chay của VeggiePal. Trả lời bằng ngôn ngữ người dùng, thực tế và ngắn gọn.
            Không chẩn đoán hoặc thay thế bác sĩ. Với triệu chứng nghiêm trọng, thai kỳ, trẻ nhỏ, bệnh nền hoặc nguy cơ thiếu chất,
            hãy khuyên người dùng gặp bác sĩ/chuyên gia dinh dưỡng. Không khẳng định chắc chắn dữ liệu dinh dưỡng khi thiếu khẩu phần.
            Chỉ tư vấn ăn chay, dinh dưỡng, nguyên liệu và nấu ăn; từ chối yêu cầu ngoài phạm vi.
            """;
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final QuotaService quota;
    private final QuotaProperties quotaProperties;
    private final GroqClient groq;
    private final ModelProperties models;
    private final UsageService usage;
    private final GuestSessionService guestSessions;

    public ChatReply guest(String guestSession, String remoteAddress, String text) {
        guestSessions.requireValid(guestSession, remoteAddress);
        String principal = "guest:" + guestSession;
        int remaining = quota.reserveChat(principal, true);
        var prompts = List.of(new GroqRequest.Message("system", SYSTEM_PROMPT), new GroqRequest.Message("user", text.trim()));
        var result = call(null, principal, prompts);
        return new ChatReply(null, result.content(), remaining, result.promptTokens(), result.completionTokens());
    }

    @Transactional
    public ChatReply member(Long userId, ChatRequest request) {
        String principal = "user:" + userId;
        int remaining = quota.reserveChat(principal, false);
        ChatConversation conversation = request.conversationId() == null
                ? conversations.save(new ChatConversation(userId, title(request.message())))
                : conversations.findByIdAndUserId(request.conversationId(), userId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        List<ChatMessage> history = messages.findByConversationIdOrderByCreatedAtDesc(conversation.getId(),
                PageRequest.of(0, quotaProperties.maxHistoryMessages()));
        Collections.reverse(history);
        List<GroqRequest.Message> prompts = new ArrayList<>();
        prompts.add(new GroqRequest.Message("system", SYSTEM_PROMPT));
        history.forEach(m -> prompts.add(new GroqRequest.Message(m.getRole(), m.getContent())));
        prompts.add(new GroqRequest.Message("user", request.message().trim()));
        var result = call(userId, principal, prompts);
        messages.save(new ChatMessage(conversation, principal, "user", request.message().trim(), result.promptTokens(), 0));
        messages.save(new ChatMessage(conversation, principal, "assistant", result.content(), 0, result.completionTokens()));
        conversation.touch();
        return new ChatReply(conversation.getId(), result.content(), remaining, result.promptTokens(), result.completionTokens());
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
