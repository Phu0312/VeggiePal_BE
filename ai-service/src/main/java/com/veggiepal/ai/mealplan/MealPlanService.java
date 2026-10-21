package com.veggiepal.ai.mealplan;

import static com.veggiepal.ai.mealplan.MealPlanDtos.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.common.PageResponse;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.quota.PremiumQuotaService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final MealPlanFavoriteRepository favoriteRepository;
    private final MealPlanVersionRepository versionRepository;
    private final PremiumQuotaService premiumQuota;

    @Autowired
    public MealPlanService(MealPlanRepository repository,
            @Qualifier("serviceRestClient") RestClient http,
            @Value("${services.recipe.base-url}") String recipeUrl,
            @Value("${services.nutrition.base-url}") String nutritionUrl,
            GroqClient groq, ModelProperties models, ObjectMapper mapper, UsageService usage,
            MealPlanValidator validator,
            MealPlanFavoriteRepository favoriteRepository,
            MealPlanVersionRepository versionRepository,
            PremiumQuotaService premiumQuota) {
        this.repository = repository;
        this.http = http;
        this.recipeUrl = recipeUrl;
        this.nutritionUrl = nutritionUrl;
        this.groq = groq;
        this.models = models;
        this.mapper = mapper;
        this.usage = usage;
        this.validator = validator;
        this.favoriteRepository = favoriteRepository;
        this.versionRepository = versionRepository;
        this.premiumQuota = premiumQuota;
    }

    public MealPlanService(MealPlanRepository repository,
            RestClient http,
            String recipeUrl,
            String nutritionUrl,
            GroqClient groq, ModelProperties models, ObjectMapper mapper, UsageService usage,
            MealPlanValidator validator, PremiumQuotaService premiumQuota) {
        this(repository, http, recipeUrl, nutritionUrl, groq, models, mapper, usage, validator, null, null, premiumQuota);
    }

    public MealPlanResponse generate(Long userId, String jwt, GenerateRequest request) {
        premiumQuota.reserveMealPlan(userId);
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
                Mỗi note viết bằng tiếng Việt, một câu ngắn.
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
                + ". Trả duy nhất JSON {\"recipeId\":1,\"note\":\"...\"}, note viết bằng tiếng Việt. Recipes: "
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
        repository.save(entity);
        return view(entity);
    }

    // ==========================================
    // FEATURE 1: FAVORITES
    // ==========================================
    @Transactional
    public MealPlanResponse favorite(Long userId, Long mealPlanId) {
        MealPlan plan = repository.findById(mealPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!plan.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (favoriteRepository != null && favoriteRepository.existsByUserIdAndMealPlanId(userId, mealPlanId)) {
            throw new AppException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
        if (favoriteRepository != null) {
            favoriteRepository.save(new MealPlanFavorite(userId, plan));
        }
        return view(plan, true);
    }

    @Transactional
    public void unfavorite(Long userId, Long mealPlanId) {
        repository.findById(mealPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (favoriteRepository == null) return;
        MealPlanFavorite favorite = favoriteRepository.findByUserIdAndMealPlanId(userId, mealPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    public PageResponse<MealPlanResponse> listFavorites(Long userId, int page, int size) {
        if (favoriteRepository == null) {
            return PageResponse.<MealPlanResponse>builder()
                    .content(List.of()).page(page).size(size).totalElements(0).totalPages(0).first(true).last(true).build();
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        Page<MealPlanFavorite> favPage = favoriteRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(safePage, safeSize));
        List<MealPlanResponse> items = favPage.getContent().stream()
                .map(fav -> view(fav.getMealPlan(), true))
                .toList();

        return PageResponse.<MealPlanResponse>builder()
                .content(items)
                .page(favPage.getNumber())
                .size(favPage.getSize())
                .totalElements(favPage.getTotalElements())
                .totalPages(favPage.getTotalPages())
                .first(favPage.isFirst())
                .last(favPage.isLast())
                .build();
    }

    public MealPlanResponse getFavoriteDetail(Long userId, Long mealPlanId) {
        if (favoriteRepository == null) throw new AppException(ErrorCode.FAVORITE_NOT_FOUND);
        MealPlanFavorite favorite = favoriteRepository.findByUserIdAndMealPlanId(userId, mealPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_NOT_FOUND));
        if (!favorite.getMealPlan().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return view(favorite.getMealPlan(), true);
    }

    // ==========================================
    // FEATURE 2: HEALTH METRICS UPDATE & VERSIONING
    // ==========================================
    @Transactional
    public UpdateFromHealthResponse updateFromHealth(Long userId, Long mealPlanId, String jwt, UpdateFromHealthRequest request) {
        MealPlan entity = owned(userId, mealPlanId);
        JsonNode health = result(get(nutritionUrl + "/nutrition/me/health-records/latest", jwt));

        NutritionTarget recalculatedTarget = (request != null && request.customTarget() != null)
                ? request.customTarget()
                : calculateTargetFromHealth(health, entity.getGoal());
        validator.validateTarget(recalculatedTarget);

        ObjectNode root = object(read(entity.getPlanJson())).deepCopy();
        ObjectNode constraints = root.withObject("constraints");
        constraints.set("dailyNutritionTarget", mapper.valueToTree(recalculatedTarget));
        constraints.set("healthRecord", health.deepCopy());

        JsonNode allergies = result(get(nutritionUrl + "/nutrition/me/allergies", jwt));
        constraints.set("allergies", allergies.deepCopy());

        JsonNode allRecipes = result(get(recipeUrl + "/recipes?size=200", null)).path("content");
        validator.validateAndEnrich(root, allRecipes, allergies, recalculatedTarget);

        boolean confirmed = request != null && Boolean.TRUE.equals(request.confirm());
        if (!confirmed) {
            return new UpdateFromHealthResponse(
                    false,
                    "Vui lòng xác nhận để lưu thực đơn cập nhật theo chỉ số sức khỏe.",
                    recalculatedTarget,
                    mapper.convertValue(read(entity.getPlanJson()), Object.class),
                    mapper.convertValue(root, Object.class),
                    null
            );
        }

        // Confirmation given: archive old version and persist updated plan
        String summary = (request.changeSummary() != null && !request.changeSummary().isBlank())
                ? request.changeSummary().trim()
                : "Cập nhật từ chỉ số sức khỏe";

        if (versionRepository != null) {
            versionRepository.save(new MealPlanVersion(
                    entity,
                    entity.getVersion() != null ? entity.getVersion() : 1,
                    entity.getName(),
                    entity.getGoal(),
                    entity.getPlanJson(),
                    summary
            ));
        }

        entity.bumpVersion(root.toString());
        repository.save(entity);

        return new UpdateFromHealthResponse(
                true,
                "Đã cập nhật thực đơn thành công từ chỉ số sức khỏe mới.",
                recalculatedTarget,
                mapper.convertValue(read(entity.getPlanJson()), Object.class),
                mapper.convertValue(root, Object.class),
                view(entity)
        );
    }

    public NutritionTarget calculateTargetFromHealth(JsonNode healthRecord, String goal) {
        double heightCm = healthRecord.path("heightCm").asDouble(170.0);
        double weightKg = healthRecord.path("weightKg").asDouble(65.0);

        // Harris-Benedict baseline TDEE
        double bmr = (10.0 * weightKg) + (6.25 * heightCm) - 120.0;
        double tdee = bmr * 1.35;

        String g = (goal != null) ? goal.toLowerCase(Locale.ROOT) : "";
        double targetCalories;
        if (g.contains("giảm") || g.contains("loss") || g.contains("diet")) {
            targetCalories = tdee * 0.85;
        } else if (g.contains("tăng") || g.contains("gain") || g.contains("bulk")) {
            targetCalories = tdee * 1.15;
        } else {
            targetCalories = tdee;
        }

        BigDecimal minCal = BigDecimal.valueOf(Math.round(targetCalories * 0.90));
        BigDecimal maxCal = BigDecimal.valueOf(Math.round(targetCalories * 1.10));

        BigDecimal minProtein = BigDecimal.valueOf(Math.round(weightKg * 1.2));
        BigDecimal maxProtein = BigDecimal.valueOf(Math.round(weightKg * 1.8));

        BigDecimal minCarbs = BigDecimal.valueOf(Math.round((targetCalories * 0.45) / 4.0));
        BigDecimal maxCarbs = BigDecimal.valueOf(Math.round((targetCalories * 0.60) / 4.0));

        BigDecimal minFat = BigDecimal.valueOf(Math.round((targetCalories * 0.20) / 9.0));
        BigDecimal maxFat = BigDecimal.valueOf(Math.round((targetCalories * 0.30) / 9.0));

        return new NutritionTarget(minCal, maxCal, minProtein, maxProtein, minCarbs, maxCarbs, minFat, maxFat);
    }

    // ==========================================
    // FEATURE 3: MANUAL EDITING & VERSION RETRIEVAL
    // ==========================================
    @Transactional
    public MealPlanEditResponse manualEdit(Long userId, Long mealPlanId, String jwt, ManualEditRequest request) {
        if (request == null || request.edits() == null || request.edits().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách chỉnh sửa không được rỗng");
        }
        MealPlan entity = owned(userId, mealPlanId);
        ObjectNode root = object(read(entity.getPlanJson())).deepCopy();
        JsonNode allergies = result(get(nutritionUrl + "/nutrition/me/allergies", jwt));
        JsonNode allRecipes = result(get(recipeUrl + "/recipes?size=200", null)).path("content");

        JsonNode targetNode = root.path("constraints").path("dailyNutritionTarget");
        NutritionTarget target = (targetNode.isMissingNode() || targetNode.isNull())
                ? null : mapper.convertValue(targetNode, NutritionTarget.class);

        Set<String> affectedDays = new HashSet<>();

        for (MealEditItem edit : request.edits()) {
            String dayName = edit.day().trim().toUpperCase(Locale.ROOT);
            if (!MealPlanValidator.STANDARD_DAYS.contains(dayName)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Ngày không hợp lệ: " + edit.day());
            }
            affectedDays.add(dayName);

            String mealType = edit.mealType().trim().toUpperCase(Locale.ROOT);
            ObjectNode dayNode = findDayNode(root, dayName);
            ArrayNode mealsArray = (ArrayNode) dayNode.path("meals");

            String action = (edit.action() != null)
                    ? edit.action().trim().toUpperCase(Locale.ROOT)
                    : inferAction(edit, mealsArray, mealType);

            switch (action) {
                case "REMOVE", "DELETE" -> {
                    removeMeal(mealsArray, mealType);
                }
                case "ADD" -> {
                    JsonNode recipe = fetchAndValidateRecipe(edit.recipeId(), allergies, allRecipes);
                    MealPlanValidator.Macro base = validator.macroFor(recipe);
                    BigDecimal mult = edit.servingMultiplier() != null ? edit.servingMultiplier() : BigDecimal.ONE;
                    if (mult.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new AppException(ErrorCode.INVALID_REQUEST, "Hệ số khẩu phần phải lớn hơn 0");
                    }
                    MealPlanValidator.Macro scaled = (mult.compareTo(BigDecimal.ONE) == 0) ? base : base.multiply(mult);

                    ObjectNode mealNode = mapper.createObjectNode();
                    mealNode.put("mealType", mealType);
                    mealNode.put("recipeId", edit.recipeId());
                    mealNode.put("note", edit.note() != null && !edit.note().isBlank() ? edit.note().trim() : "Bữa ăn bổ sung");
                    mealNode.set("nutrition", scaled.toJson(mapper));
                    if (mult.compareTo(BigDecimal.ONE) != 0) {
                        mealNode.put("servingMultiplier", mult.setScale(2, RoundingMode.HALF_UP));
                    }
                    mealsArray.add(mealNode);
                }
                case "REPLACE" -> {
                    JsonNode recipe = fetchAndValidateRecipe(edit.recipeId(), allergies, allRecipes);
                    MealPlanValidator.Macro base = validator.macroFor(recipe);
                    ObjectNode existing = findMealNode(mealsArray, mealType);
                    BigDecimal mult = edit.servingMultiplier() != null
                            ? edit.servingMultiplier()
                            : (existing != null && existing.hasNonNull("servingMultiplier") ? existing.path("servingMultiplier").decimalValue() : BigDecimal.ONE);
                    if (mult.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new AppException(ErrorCode.INVALID_REQUEST, "Hệ số khẩu phần phải lớn hơn 0");
                    }
                    MealPlanValidator.Macro scaled = (mult.compareTo(BigDecimal.ONE) == 0) ? base : base.multiply(mult);

                    if (existing == null) {
                        existing = mealsArray.addObject();
                        existing.put("mealType", mealType);
                    }
                    existing.put("recipeId", edit.recipeId());
                    if (edit.note() != null && !edit.note().isBlank()) {
                        existing.put("note", edit.note().trim());
                    }
                    existing.set("nutrition", scaled.toJson(mapper));
                    if (mult.compareTo(BigDecimal.ONE) != 0) {
                        existing.put("servingMultiplier", mult.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        existing.remove("servingMultiplier");
                    }
                }
                case "UPDATE_SERVING" -> {
                    ObjectNode existing = findMealNode(mealsArray, mealType);
                    if (existing == null) {
                        throw new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy bữa ăn để cập nhật khẩu phần");
                    }
                    BigDecimal mult = edit.servingMultiplier();
                    if (mult == null || mult.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new AppException(ErrorCode.INVALID_REQUEST, "Hệ số khẩu phần phải lớn hơn 0");
                    }
                    long recipeId = existing.path("recipeId").asLong();
                    JsonNode recipe = fetchAndValidateRecipe(recipeId, allergies, allRecipes);
                    MealPlanValidator.Macro base = validator.macroFor(recipe);
                    MealPlanValidator.Macro scaled = (mult.compareTo(BigDecimal.ONE) == 0) ? base : base.multiply(mult);

                    existing.set("nutrition", scaled.toJson(mapper));
                    if (mult.compareTo(BigDecimal.ONE) != 0) {
                        existing.put("servingMultiplier", mult.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        existing.remove("servingMultiplier");
                    }
                }
                default -> throw new AppException(ErrorCode.INVALID_REQUEST, "Hành động chỉnh sửa không hợp lệ: " + action);
            }
        }

        // Recalculate daily totals and targets for all days
        for (JsonNode dayNode : root.path("days")) {
            if (dayNode instanceof ObjectNode d) {
                MealPlanValidator.Macro dayTotal = new MealPlanValidator.Macro();
                for (JsonNode m : d.path("meals")) {
                    JsonNode nut = m.path("nutrition");
                    if (nut != null && !nut.isMissingNode()) {
                        BigDecimal cal = safeDecimal(nut.path("calories"));
                        BigDecimal pro = safeDecimal(nut.path("protein"));
                        BigDecimal carb = safeDecimal(nut.path("carbohydrate"));
                        BigDecimal fat = safeDecimal(nut.path("fat"));
                        dayTotal.add(new MealPlanValidator.Macro(cal, pro, carb, fat));
                    }
                }
                d.set("nutritionTotals", dayTotal.toJson(mapper));
                validator.markTarget(d, dayTotal, target);
            }
        }

        boolean isPreview = Boolean.TRUE.equals(request.preview());
        if (isPreview) {
            return new MealPlanEditResponse(true, "Bản xem trước chỉnh sửa thực đơn thành công", mapper.convertValue(root, Object.class), null);
        }

        String summary = (request.changeSummary() != null && !request.changeSummary().isBlank())
                ? request.changeSummary().trim()
                : "Chỉnh sửa thực đơn thủ công";

        if (versionRepository != null) {
            versionRepository.save(new MealPlanVersion(
                    entity,
                    entity.getVersion() != null ? entity.getVersion() : 1,
                    entity.getName(),
                    entity.getGoal(),
                    entity.getPlanJson(),
                    summary
            ));
        }

        entity.bumpVersion(root.toString());
        repository.save(entity);

        return new MealPlanEditResponse(false, "Chỉnh sửa thực đơn thành công", mapper.convertValue(root, Object.class), view(entity));
    }

    public List<MealPlanVersionResponse> listVersions(Long userId, Long mealPlanId) {
        owned(userId, mealPlanId);
        if (versionRepository == null) return List.of();
        return versionRepository.findByMealPlanIdOrderByVersionDesc(mealPlanId).stream()
                .map(this::viewVersion)
                .toList();
    }

    public MealPlanVersionResponse getVersion(Long userId, Long mealPlanId, Integer version) {
        owned(userId, mealPlanId);
        if (versionRepository == null) throw new AppException(ErrorCode.VERSION_NOT_FOUND);
        MealPlanVersion ver = versionRepository.findByMealPlanIdAndVersion(mealPlanId, version)
                .orElseThrow(() -> new AppException(ErrorCode.VERSION_NOT_FOUND));
        return viewVersion(ver);
    }

    private MealPlanVersionResponse viewVersion(MealPlanVersion v) {
        return new MealPlanVersionResponse(
                v.getId(),
                v.getMealPlan().getId(),
                v.getVersion(),
                v.getName(),
                v.getGoal(),
                mapper.convertValue(read(v.getPlanJson()), Object.class),
                v.getChangeSummary(),
                v.getCreatedAt()
        );
    }

    private JsonNode fetchAndValidateRecipe(Long recipeId, JsonNode allergies, JsonNode allRecipes) {
        if (recipeId == null || recipeId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ID món ăn không hợp lệ");
        }
        JsonNode recipe = null;
        if (allRecipes != null && allRecipes.isArray()) {
            for (JsonNode r : allRecipes) {
                if (r.path("id").asLong() == recipeId) {
                    recipe = r;
                    break;
                }
            }
        }
        if (recipe == null) {
            try {
                recipe = result(get(recipeUrl + "/recipes/" + recipeId, null));
            } catch (Exception ex) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công thức với ID: " + recipeId);
            }
        }
        if (!validator.isVegan(recipe)) {
            throw new AppException(ErrorCode.RECIPE_NOT_VEGAN);
        }
        if (!validator.isAllergySafe(recipe, allergies)) {
            throw new AppException(ErrorCode.RECIPE_CONTAINS_ALLERGEN);
        }
        if (!validator.canCalculateMacro(recipe)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Công thức không thể tính toán giá trị dinh dưỡng");
        }
        return recipe;
    }

    private ObjectNode findDayNode(ObjectNode root, String dayName) {
        JsonNode days = root.path("days");
        if (days.isArray()) {
            for (JsonNode d : days) {
                if (d.path("day").asText().equalsIgnoreCase(dayName) && d instanceof ObjectNode on) {
                    return on;
                }
            }
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy ngày: " + dayName);
    }

    private ObjectNode findMealNode(ArrayNode mealsArray, String mealType) {
        for (JsonNode m : mealsArray) {
            if (m.path("mealType").asText().equalsIgnoreCase(mealType) && m instanceof ObjectNode on) {
                return on;
            }
        }
        return null;
    }

    private void removeMeal(ArrayNode mealsArray, String mealType) {
        for (int i = 0; i < mealsArray.size(); i++) {
            if (mealsArray.get(i).path("mealType").asText().equalsIgnoreCase(mealType)) {
                mealsArray.remove(i);
                return;
            }
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy bữa ăn để xóa: " + mealType);
    }

    private String inferAction(MealEditItem edit, ArrayNode mealsArray, String mealType) {
        ObjectNode existing = findMealNode(mealsArray, mealType);
        if (edit.recipeId() == null && edit.servingMultiplier() == null) {
            return "REMOVE";
        }
        if (existing == null) {
            return "ADD";
        }
        if (edit.recipeId() != null) {
            return "REPLACE";
        }
        return "UPDATE_SERVING";
    }

    private BigDecimal safeDecimal(JsonNode node) {
        if (node == null || !node.isNumber()) return BigDecimal.ZERO;
        return node.decimalValue();
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
        boolean isFav = false;
        if (favoriteRepository != null) {
            isFav = favoriteRepository.existsByUserIdAndMealPlanId(plan.getUserId(), plan.getId());
        }
        return view(plan, isFav);
    }

    private MealPlanResponse view(MealPlan plan, boolean isFavorite) {
        return new MealPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getGoal(),
                mapper.convertValue(read(plan.getPlanJson()), Object.class),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getVersion() != null ? plan.getVersion() : 1,
                isFavorite
        );
    }

    private List<String> nullToEmpty(List<String> values) { return values == null ? List.of() : values; }
    private String cleanJson(String raw) { return raw.replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim(); }
    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
}
