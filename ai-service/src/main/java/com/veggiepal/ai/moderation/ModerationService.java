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

    public ModerationResponse moderate(Long callerId, ModerationRequest request) {
        usage.checkBudget("user:"+callerId);
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
            usage.success(callerId,"user:"+callerId,"MODERATION",models.moderation(),result,elapsed(start));
            JsonNode json=mapper.readTree(cleanJson(result.content()));
            Decision decision=Decision.valueOf(json.path("decision").asText("REVIEW_REQUIRED"));
            BigDecimal confidence=BigDecimal.valueOf(json.path("confidence").asDouble(0));
            if (decision==Decision.REJECTED && confidence.compareTo(new BigDecimal("0.85"))<0) decision=Decision.REVIEW_REQUIRED;
            List<String> categories=mapper.convertValue(json.path("categories"), mapper.getTypeFactory().constructCollectionType(List.class,String.class));
            String reason=json.path("reason").asText(null);
            ModerationCase saved=repository.save(new ModerationCase(request.contentType().name(),request.externalContentId(),request.authorId(),decision.name(),String.join(",",categories),reason,confidence));
            return view(saved);
        } catch (AppException exception) {
            usage.failure(callerId,"user:"+callerId,"MODERATION",models.moderation(),elapsed(start),exception); throw exception;
        } catch (Exception exception) {
            usage.failure(callerId,"user:"+callerId,"MODERATION",models.moderation(),elapsed(start),exception);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE,exception);
        }
    }
    public List<ModerationResponse> pending() { return repository.findByDecisionOrderByCreatedAtAsc(Decision.REVIEW_REQUIRED.name()).stream().map(this::view).toList(); }
    @Transactional
    public ModerationResponse review(Long adminId, Long id, ReviewRequest request, String bearerToken) {
        if (request.decision()==Decision.REVIEW_REQUIRED) throw new AppException(ErrorCode.INVALID_REQUEST);
        ModerationCase item=repository.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ownerClient.apply(item.getContentType(), item.getExternalContentId(), item.getAuthorId(), request.decision(), bearerToken);
        item.review(adminId,request.decision().name(),request.reason()); return view(item);
    }
    private ModerationResponse view(ModerationCase item) {
        List<String> categories=item.getCategories()==null||item.getCategories().isBlank()?List.of():List.of(item.getCategories().split(","));
        return new ModerationResponse(item.getId(),Decision.valueOf(item.getDecision()),categories,item.getReason(),item.getConfidence(),item.getCreatedAt());
    }
    private String cleanJson(String raw) { return raw.replaceFirst("(?s)^```(?:json)?\\s*","").replaceFirst("\\s*```$","").trim(); }
    private long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
}
