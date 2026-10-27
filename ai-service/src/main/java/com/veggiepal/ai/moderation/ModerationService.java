package com.veggiepal.ai.moderation;

import static com.veggiepal.ai.moderation.ModerationDtos.*;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.monitoring.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private final ModerationRepository repository;
    private final GroqClient groq;
    private final ModelProperties models;
    private final ObjectMapper mapper;
    private final UsageService usage;
    private final ModerationOwnerClient ownerClient;

    static final String SERVICE_PRINCIPAL = "service:moderation";

    public ModerationResponse moderate(Long callerId, ModerationRequest request) {
        usage.checkBudget("user:"+callerId);
        return classify(callerId, "user:"+callerId, request);
    }

    /** blog-service's background moderation (task B spec §2.8): a platform cost, so no per-user budget. */
    public ModerationResponse moderateForService(ModerationRequest request) {
        return classify(request.authorId(), SERVICE_PRINCIPAL, request);
    }

    private ModerationResponse classify(Long userId, String principal, ModerationRequest request) {
        String prompt = """
                Phân loại nội dung cộng đồng sau. Vi phạm gồm: hate, harassment, sexual, violence, self_harm,
                illegal, spam, misinformation_dangerous. Trả duy nhất JSON:
                {"decision":"APPROVED|REJECTED|REVIEW_REQUIRED","categories":["..."],"reason":"...","confidence":0.0}
                REJECTED chỉ khi vi phạm rõ và confidence >= 0.85; không chắc chắn phải REVIEW_REQUIRED.
                Nội dung: %s
                """.formatted(request.text());
        long start=System.nanoTime();
        try {
            var result=groq.complete(models.moderation(), List.of(new GroqRequest.Message("system","Bạn là bộ kiểm duyệt an toàn VeggiePal."), new GroqRequest.Message("user",prompt)),0,350);
            usage.success(userId,principal,"MODERATION",models.moderation(),result,elapsed(start));
            JsonNode json=mapper.readTree(cleanJson(result.content()));
            Decision decision=Decision.valueOf(json.path("decision").asText("REVIEW_REQUIRED"));
            BigDecimal confidence=BigDecimal.valueOf(json.path("confidence").asDouble(0));
            if (decision==Decision.REJECTED && confidence.compareTo(new BigDecimal("0.85"))<0) decision=Decision.REVIEW_REQUIRED;
            List<String> categories=mapper.convertValue(json.path("categories"), mapper.getTypeFactory().constructCollectionType(List.class,String.class));
            String reason=json.path("reason").asText(null);
            String type = request.contentType().name();
            String joinedCategories = String.join(",", categories);
            ModerationCase item = request.externalContentId() == null || request.authorId() == null
                    ? null
                    : repository.findFirstByContentTypeAndExternalContentIdAndAuthorIdAndReviewedAtIsNullOrderByCreatedAtDesc(
                            type, request.externalContentId(), request.authorId()).orElse(null);
            if (item == null) {
                item = new ModerationCase(type, request.externalContentId(), request.authorId(), request.text(),
                        decision.name(), joinedCategories, reason, confidence);
            } else {
                item.refresh(request.text(), decision.name(), joinedCategories, reason, confidence);
            }
            ModerationCase saved=repository.save(item);
            return view(saved);
        } catch (AppException exception) {
            usage.failure(userId,principal,"MODERATION",models.moderation(),elapsed(start),exception); throw exception;
        } catch (Exception exception) {
            usage.failure(userId,principal,"MODERATION",models.moderation(),elapsed(start),exception);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE,exception);
        }
    }
    public List<ModerationResponse> pending() { return repository.findByDecisionOrderByCreatedAtAsc(Decision.REVIEW_REQUIRED.name()).stream().map(this::view).toList(); }
    @Transactional
    public ModerationResponse review(Long adminId, Long id, ReviewRequest request, String bearerToken) {
        if (request.decision() == Decision.REVIEW_REQUIRED) throw new AppException(ErrorCode.INVALID_REQUEST);
        ModerationCase item = repository.findForReview(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!item.isPending()) throw new AppException(ErrorCode.MODERATION_CASE_STALE);

        if (request.decision() == Decision.DISMISSED) {
            item.review(adminId, Decision.DISMISSED.name(), request.reason() != null ? request.reason() : "Dismissed by admin");
            return view(item);
        }

        try {
            ownerClient.apply(item.getContentType(), item.getExternalContentId(), item.getAuthorId(), request.decision(), bearerToken);
            item.review(adminId, request.decision().name(), request.reason());
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.MODERATION_TARGET_NOT_FOUND) {
                // The target content was deleted in blog-service (404). Close and dismiss the orphan case
                // so it leaves the queue instead of staying stuck in REVIEW_REQUIRED.
                item.review(adminId, Decision.DISMISSED.name(),
                        request.reason() != null ? request.reason() : "Target content no longer exists (404)");
            } else {
                // Transient failure (DOWNSTREAM_UNAVAILABLE) or other error: rethrow to rollback transaction so admin can retry
                throw ex;
            }
        }
        return view(item);
    }

    @Transactional
    public ModerationResponse dismiss(Long adminId, Long id, String reason) {
        ModerationCase item = repository.findForReview(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!item.isPending()) throw new AppException(ErrorCode.MODERATION_CASE_STALE);
        item.review(adminId, Decision.DISMISSED.name(), reason != null && !reason.isBlank() ? reason : "Dismissed by admin");
        return view(item);
    }
    private ModerationResponse view(ModerationCase item) {
        List<String> categories=item.getCategories()==null||item.getCategories().isBlank()?List.of():List.of(item.getCategories().split(","));
        return new ModerationResponse(item.getId(), ContentType.valueOf(item.getContentType()),
                item.getExternalContentId(), item.getAuthorId(), item.getContentText(),
                Decision.valueOf(item.getDecision()), categories, item.getReason(), item.getConfidence(),
                item.getReviewedBy(), item.getReviewedAt(), item.getCreatedAt());
    }
    private String cleanJson(String raw) { return raw.replaceFirst("(?s)^```(?:json)?\\s*","").replaceFirst("\\s*```$","").trim(); }
    private long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
}
