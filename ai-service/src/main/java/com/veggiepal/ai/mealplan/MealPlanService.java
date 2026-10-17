package com.veggiepal.ai.mealplan;

import static com.veggiepal.ai.mealplan.MealPlanDtos.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.monitoring.UsageService;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MealPlanService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MealPlanService.class);
    private final MealPlanRepository repository;
    private final RestClient http;
    private final String recipeUrl;
    private final String nutritionUrl;
    private final GroqClient groq;
    private final ModelProperties models;
    private final ObjectMapper mapper;
    private final UsageService usage;
    private final MealPlanValidator validator;

    public MealPlanService(MealPlanRepository repository,
            @Qualifier("serviceRestClient") RestClient http,
            @Value("${services.recipe.base-url}") String recipeUrl,
            @Value("${services.nutrition.base-url}") String nutritionUrl,
            GroqClient groq, ModelProperties models, ObjectMapper mapper, UsageService usage,
            MealPlanValidator validator) {
        this.repository = repository;
        this.http = http;
        this.recipeUrl = recipeUrl;
        this.nutritionUrl = nutritionUrl;
        this.groq = groq;
        this.models = models;
        this.mapper = mapper;
        this.usage = usage;
        this.validator = validator;
    }

    @Transactional
    public MealPlanResponse generate(Long userId, String jwt, GenerateRequest request) {
        validator.validateTarget(request.dailyNutritionTarget());
        JsonNode allRecipes = result(get(recipeUrl + "/recipes?size=200", null)).path("content");
        JsonNode allergies = result(get(nutritionUrl + "/nutrition/me/allergies", jwt));
        JsonNode health = result(get(nutritionUrl + "/nutrition/me/health-records/latest", jwt));
        ArrayNode candidates = validator.safeCandidates(allRecipes, allergies, request.availableIngredients());
        ArrayNode promptRecipes = validator.promptCandidates(candidates);

        String prompt = """
                Tạo thực đơn chay đúng 7 ngày từ danh sách recipe thật. Không tự tạo recipe hoặc ID.
                Mỗi ngày phải có duy nhất BREAKFAST, LUNCH và DINNER. Ưu tiên sở thích và mục tiêu được cung cấp.
                Trả duy nhất JSON {"days":[{"day":"MONDAY","meals":[{"mealType":"BREAKFAST","recipeId":1,"note":"..."}]}]}.
                Mục tiêu: %s; sở thích: %s; nguyên liệu sẵn có: %s; sức khỏe: %s; dị ứng đã lọc: %s;
                mục tiêu dinh dưỡng/ngày: %s; recipes an toàn: %s
                """.formatted(request.healthGoal(), request.preferences(), request.availableIngredients(), health,
                allergies, request.dailyNutritionTarget(), json(promptRecipes));

        ObjectNode plan;
        try {
            JsonNode aiNode = call(userId, prompt);
            plan = (aiNode instanceof ObjectNode on) ? on : mapper.createObjectNode();
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.QUOTA_EXCEEDED
                    || ex.getErrorCode() == ErrorCode.UNAUTHENTICATED
                    || ex.getErrorCode() == ErrorCode.GROQ_NOT_CONFIGURED) {
                throw ex;
            }
            log.warn("Groq plan generation failed with error {}, falling back to deterministic planner.", ex.getErrorCode(), ex);
            plan = mapper.createObjectNode();
        } catch (Exception ex) {
            log.warn("Groq plan generation failed with unexpected error, falling back to deterministic planner.", ex);
            plan = mapper.createObjectNode();
        }

        validator.validateAndEnrich(plan, candidates, allergies, request.dailyNutritionTarget());
        ObjectNode constraints = plan.putObject("constraints");
        constraints.set("preferences", mapper.valueToTree(nullToEmpty(request.preferences())));
        constraints.set("availableIngredients", mapper.valueToTree(nullToEmpty(request.availableIngredients())));
        constraints.set("allergies", allergies.deepCopy());
        constraints.set("healthRecord", health.deepCopy());
        constraints.set("dailyNutritionTarget", mapper.valueToTree(request.dailyNutritionTarget()));

        MealPlan saved = repository.save(new MealPlan(
                userId, request.name().trim(), request.healthGoal().trim(), plan.toString()));
        return view(saved);
    }

    public List<MealPlanResponse> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::view).toList();
    }

    public MealPlanResponse get(Long userId, Long id) { return view(owned(userId, id)); }

    @Transactional
    public MealPlanResponse replace(Long userId, Long id, String jwt, ReplaceMealRequest request) {
        MealPlan entity = owned(userId, id);
        ObjectNode root = object(read(entity.getPlanJson()));
        JsonNode constraints = root.path("constraints");
        List<String> available = constraints.path("availableIngredients").isArray()
                ? mapper.convertValue(constraints.path("availableIngredients"), new TypeReference<>() {}) : List.of();
        NutritionTarget target = constraints.path("dailyNutritionTarget").isNull()
                || constraints.path("dailyNutritionTarget").isMissingNode()
                ? null : mapper.convertValue(constraints.path("dailyNutritionTarget"), NutritionTarget.class);
        validator.validateTarget(target);

        JsonNode allRecipes = result(get(recipeUrl + "/recipes?size=200", null)).path("content");
        JsonNode allergies = result(get(nutritionUrl + "/nutrition/me/allergies", jwt));
        ArrayNode safeRecipes = validator.safeCandidates(allRecipes, allergies, List.of());
        ArrayNode candidates = validator.safeCandidates(allRecipes, allergies, available);
        String prompt = "Chọn một recipe thay thế từ danh sách thật, khác ID " + request.currentRecipeId()
                + ", phù hợp mục tiêu " + entity.getGoal()
                + ". Trả duy nhất JSON {\"recipeId\":1,\"note\":\"...\"}. Recipes: "
                + json(validator.promptCandidates(candidates));

        long replacementId = -1;
        String note = "Món ăn thay thế dinh dưỡng";
        try {
            JsonNode replacement = call(userId, prompt);
            replacementId = replacement.path("recipeId").asLong(-1);
            if (replacement.hasNonNull("note") && !replacement.path("note").asText().isBlank()) {
                note = replacement.path("note").asText();
            }
        } catch (Exception ex) {
            log.warn("AI recipe replacement failed, selecting best deterministic alternative.", ex);
        }

        Long currentId = request.currentRecipeId();
        boolean validReplacement = replacementId > 0
                && (currentId == null || replacementId != currentId)
                && isCandidateSafe(candidates, allergies, replacementId);

        if (!validReplacement) {
            replacementId = selectAlternativeReplacement(candidates, allergies, currentId);
        }

        boolean replaced = false;
        for (JsonNode day : root.path("days")) {
            if (!day.path("day").asText().equalsIgnoreCase(request.day())) continue;
            for (JsonNode meal : day.path("meals")) {
                if (!meal.path("mealType").asText().equalsIgnoreCase(request.mealType())) continue;
                ObjectNode object = (ObjectNode) meal;
                object.put("recipeId", replacementId);
                object.put("note", note);
                replaced = true;
                break;
            }
            if (replaced) break;
        }
        if (!replaced) throw new AppException(ErrorCode.INVALID_REQUEST);
        validator.validateAndEnrich(root, safeRecipes, allergies, target);
        root.withObject("constraints").set("allergies", allergies.deepCopy());
        entity.replaceJson(root.toString());
        return view(entity);
    }

    private JsonNode call(Long userId, String prompt) {
        usage.checkBudget("user:" + userId);
        long start = System.nanoTime();
        try {
            var result = groq.complete(models.mealPlan(), List.of(
                    new GroqRequest.Message("system", "Lập thực đơn chay an toàn chỉ từ dữ liệu được cung cấp. Không tự tính dinh dưỡng."),
                    new GroqRequest.Message("user", prompt)), 0.2, 1800);
            usage.success(userId, "user:" + userId, "MEAL_PLAN", models.mealPlan(), result, elapsed(start));
            return read(cleanJson(result.content()));
        } catch (AppException exception) {
            usage.failure(userId, "user:" + userId, "MEAL_PLAN", models.mealPlan(), elapsed(start), exception);
            throw exception;
        } catch (Exception exception) {
            usage.failure(userId, "user:" + userId, "MEAL_PLAN", models.mealPlan(), elapsed(start), exception);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE, exception);
        }
    }

    private JsonNode get(String url, String jwt) {
        try {
            var spec = http.get().uri(url);
            if (jwt != null) spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
            return spec.retrieve().body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404 && url.contains("/health-records/latest")) {
                throw new AppException(ErrorCode.HEALTH_RECORD_REQUIRED, exception);
            }
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE, exception);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE, exception);
        }
    }

    private JsonNode result(JsonNode envelope) {
        if (envelope == null || envelope.path("result").isMissingNode()) {
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        }
        return envelope.path("result");
    }

    private ObjectNode object(JsonNode node) {
        if (node instanceof ObjectNode object) return object;
        throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
    }

    private JsonNode read(String value) {
        try { return mapper.readTree(value); }
        catch (Exception exception) { throw new AppException(ErrorCode.INVALID_AI_RESPONSE, exception); }
    }

    private String json(JsonNode value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new AppException(ErrorCode.INVALID_REQUEST, exception); }
    }

    private JsonNode findRecipe(JsonNode recipes, long id) {
        for (JsonNode recipe : recipes) if (recipe.path("id").asLong() == id) return recipe;
        throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
    }

    private boolean isCandidateSafe(JsonNode candidates, JsonNode allergies, long id) {
        if (!candidates.isArray()) return false;
        for (JsonNode c : candidates) {
            if (c.path("id").asLong() == id) {
                return validator.isAllergySafe(c, allergies);
            }
        }
        return false;
    }

    private long selectAlternativeReplacement(JsonNode candidates, JsonNode allergies, Long currentId) {
        if (candidates.isArray()) {
            for (JsonNode c : candidates) {
                long id = c.path("id").asLong();
                if ((currentId == null || id != currentId) && validator.isAllergySafe(c, allergies)) {
                    return id;
                }
            }
        }
        throw new AppException(ErrorCode.NO_VALID_RECIPES);
    }

    private MealPlan owned(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private MealPlanResponse view(MealPlan plan) {
        return new MealPlanResponse(plan.getId(), plan.getName(), plan.getGoal(), read(plan.getPlanJson()),
                plan.getCreatedAt(), plan.getUpdatedAt());
    }

    private List<String> nullToEmpty(List<String> values) { return values == null ? List.of() : values; }
    private String cleanJson(String raw) { return raw.replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim(); }
    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
}
