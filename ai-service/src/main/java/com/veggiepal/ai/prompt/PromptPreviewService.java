package com.veggiepal.ai.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.video.VideoProperties;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromptPreviewService {
    private final PromptManagementService management;
    private final PromptRuntime runtime;
    private final GroqClient groq;
    private final ModelProperties models;
    private final ObjectMapper mapper;
    private final VideoProperties videoProperties;

    public PromptPreviewService(PromptManagementService management, PromptRuntime runtime,
                                GroqClient groq, ModelProperties models, ObjectMapper mapper,
                                VideoProperties videoProperties) {
        this.management = management; this.runtime = runtime; this.groq = groq;
        this.models = models; this.mapper = mapper; this.videoProperties = videoProperties;
    }

    public PreviewResult preview(PromptKey key, int version, String sampleInput, Map<String, String> variables) {
        var entry = management.getVersion(key, version);
        if (!"DRAFT".equals(entry.status())) throw new AppException(ErrorCode.INVALID_REQUEST, "Only drafts can be previewed");
        Map<String, String> values = variables == null ? Map.of() : variables;
        String candidate;
        try { candidate = PromptTemplate.render(key, entry.content(), values); }
        catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.INVALID_REQUEST, exception.getMessage()); }
        String input = sampleInput == null ? "" : sampleInput.trim();
        List<GroqRequest.Message> messages;
        String model;
        int maxTokens;
        double temperature;
        switch (key) {
            case CHAT_SYSTEM -> {
                if (input.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST, "sampleInput is required");
                messages = List.of(new GroqRequest.Message("system", candidate), new GroqRequest.Message("user", input));
                model = models.chat(); maxTokens = 700; temperature = 0.4;
            }
            case VIDEO_EXTRACTION -> {
                if (input.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST, "sampleInput is required");
                messages = List.of(new GroqRequest.Message("system", candidate),
                        new GroqRequest.Message("user", json(Map.of("transcript", input))));
                model = models.videoExtraction(); maxTokens = videoProperties.videoExtractionMaxTokens();
                temperature = 0.1;
            }
            case MEAL_PLAN_SYSTEM, MEAL_PLAN_CREATE, MEAL_PLAN_REPLACE -> {
                String system = key == PromptKey.MEAL_PLAN_SYSTEM ? candidate
                        : runtime.activeContent(PromptKey.MEAL_PLAN_SYSTEM);
                String user = key == PromptKey.MEAL_PLAN_SYSTEM ? input : candidate;
                if (user.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST, "sampleInput is required");
                messages = List.of(new GroqRequest.Message("system", system), new GroqRequest.Message("user", user));
                model = models.mealPlan(); maxTokens = 1800; temperature = 0.2;
            }
            case RECOMMENDATION -> {
                messages = List.of(new GroqRequest.Message("user", candidate));
                model = models.recommendation(); maxTokens = 1500; temperature = 0.2;
            }
            case MODERATION_SYSTEM, MODERATION_USER -> {
                if (key == PromptKey.MODERATION_SYSTEM && input.isBlank())
                    throw new AppException(ErrorCode.INVALID_REQUEST, "sampleInput is required");
                String system = key == PromptKey.MODERATION_SYSTEM ? candidate
                        : runtime.activeContent(PromptKey.MODERATION_SYSTEM);
                String user = key == PromptKey.MODERATION_SYSTEM
                        ? runtime.render(PromptKey.MODERATION_USER, Map.of("content", input)) : candidate;
                if (user.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST, "sampleInput is required");
                messages = List.of(new GroqRequest.Message("system", system), new GroqRequest.Message("user", user));
                model = models.moderation(); maxTokens = 350; temperature = 0;
            }
            default -> throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        var result = groq.complete(model, messages, temperature, maxTokens);
        boolean jsonValid = key == PromptKey.CHAT_SYSTEM || isJsonShapeValid(key, result.content());
        return new PreviewResult(result.content(), jsonValid, result.promptTokens(), result.completionTokens());
    }

    private boolean isJsonShapeValid(PromptKey key, String content) {
        try {
            JsonNode node = mapper.readTree(content.trim().replaceAll("^```(?:json)?\\s*|\\s*```$", ""));
            return switch (key) {
                case VIDEO_EXTRACTION -> node.isObject() && (node.has("error")
                        || (node.has("title") && node.path("ingredients").isArray()
                        && node.path("steps").isArray() && node.has("nutritionEstimate")));
                case MEAL_PLAN_CREATE, MEAL_PLAN_SYSTEM -> node.isObject() && node.path("days").isArray();
                case MEAL_PLAN_REPLACE -> node.isObject() && node.path("recipeId").isNumber();
                case RECOMMENDATION -> node.isArray();
                case MODERATION_SYSTEM, MODERATION_USER -> node.isObject()
                        && node.has("decision") && node.path("categories").isArray();
                default -> true;
            };
        } catch (Exception exception) { return false; }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new AppException(ErrorCode.INVALID_REQUEST, exception); }
    }

    public record PreviewResult(String output, boolean jsonShapeValid, int promptTokens, int completionTokens) { }
}
