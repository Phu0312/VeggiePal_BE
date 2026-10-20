package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.common.PageResponse;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class MealPlanUpgradeServiceTest {

    private MealPlanRepository repository;
    private MealPlanFavoriteRepository favoriteRepository;
    private MealPlanVersionRepository versionRepository;
    private RestClient http;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.ResponseSpec responseSpec;
    private GroqClient groq;
    private ModelProperties models;
    private ObjectMapper mapper;
    private UsageService usage;
    private MealPlanValidator validator;
    private MealPlanService service;

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long PLAN_ID = 100L;

    @BeforeEach
    void setUp() {
        repository = mock(MealPlanRepository.class);
        favoriteRepository = mock(MealPlanFavoriteRepository.class);
        versionRepository = mock(MealPlanVersionRepository.class);
        http = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        groq = mock(GroqClient.class);
        models = new ModelProperties("chat-model", "meal-model", "rec-model", "profile-model", "video-model", "whisper-model");
        mapper = new ObjectMapper();
        usage = mock(UsageService.class);
        validator = new MealPlanValidator(mapper);

        when(http.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.header(anyString(), anyString())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);

        service = new MealPlanService(
                repository,
                http,
                "http://recipe-service",
                "http://nutrition-service",
                groq,
                models,
                mapper,
                usage,
                validator,
                favoriteRepository,
                versionRepository
        );
    }

    private String samplePlanJson() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode days = root.putArray("days");
        for (String day : MealPlanValidator.STANDARD_DAYS) {
            ObjectNode d = days.addObject();
            d.put("day", day);
            ArrayNode meals = d.putArray("meals");
            for (String mealType : MealPlanValidator.STANDARD_MEALS) {
                ObjectNode m = meals.addObject();
                m.put("mealType", mealType);
                m.put("recipeId", 1);
                m.put("note", "Món ăn cân bằng dinh dưỡng");
                ObjectNode nut = m.putObject("nutrition");
                nut.put("calories", 300);
                nut.put("protein", 15);
                nut.put("carbohydrate", 40);
                nut.put("fat", 10);
            }
            ObjectNode totals = d.putObject("nutritionTotals");
            totals.put("calories", 900);
            totals.put("protein", 45);
            totals.put("carbohydrate", 120);
            totals.put("fat", 30);
            d.put("targetMet", true);
        }
        ObjectNode constraints = root.putObject("constraints");
        constraints.putArray("preferences").add("món Việt");
        constraints.putArray("availableIngredients").add("đậu hũ");
        constraints.putArray("allergies");
        ObjectNode target = constraints.putObject("dailyNutritionTarget");
        target.put("minCalories", 1500).put("maxCalories", 2200);
        target.put("minProtein", 60).put("maxProtein", 120);
        target.put("minCarbohydrate", 150).put("maxCarbohydrate", 300);
        target.put("minFat", 30).put("maxFat", 80);

        return root.toString();
    }

    private ObjectNode sampleRecipe(long id, String title, boolean isVegan, boolean isAllergen) {
        ObjectNode r = mapper.createObjectNode();
        r.put("id", id).put("title", title).put("servings", 1);
        ArrayNode ings = r.putArray("ingredients");
        ObjectNode ing = ings.addObject();
        ing.put("ingredientName", "Đậu hũ non").put("normalizedName", "dau hu non");
        ing.put("quantity", 100).put("unit", "GRAM");
        ing.put("caloriesPer100g", 80).put("proteinPer100g", 10);
        ing.put("carbsPer100g", 3).put("fatPer100g", 4);
        ing.put("vegan", isVegan).put("allergen", isAllergen);
        return r;
    }

    // ==========================================
    // FEATURE 1: FAVORITES TESTS
    // ==========================================

    @Test
    void favorite_success() {
        MealPlan plan = new MealPlan(USER_ID, "Thực đơn mẫu", "Giảm cân", samplePlanJson());
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(favoriteRepository.existsByUserIdAndMealPlanId(USER_ID, PLAN_ID)).thenReturn(false);

        MealPlanDtos.MealPlanResponse response = service.favorite(USER_ID, PLAN_ID);

        assertNotNull(response);
        assertTrue(response.isFavorite());
        verify(favoriteRepository, times(1)).save(any(MealPlanFavorite.class));
    }

    @Test
    void favorite_duplicate_throwsConflict() {
        MealPlan plan = new MealPlan(USER_ID, "Thực đơn mẫu", "Giảm cân", samplePlanJson());
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(favoriteRepository.existsByUserIdAndMealPlanId(USER_ID, PLAN_ID)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.favorite(USER_ID, PLAN_ID));
        assertEquals(ErrorCode.FAVORITE_ALREADY_EXISTS, ex.getErrorCode());
        assertEquals(409, ex.getErrorCode().status.value());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void favorite_unauthorized_throwsAccessDenied() {
        MealPlan plan = new MealPlan(OTHER_USER_ID, "Thực đơn người khác", "Tăng cân", samplePlanJson());
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        AppException ex = assertThrows(AppException.class, () -> service.favorite(USER_ID, PLAN_ID));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertEquals(403, ex.getErrorCode().status.value());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void favorite_planNotFound_throwsResourceNotFound() {
        when(repository.findById(PLAN_ID)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.favorite(USER_ID, PLAN_ID));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void unfavorite_success() {
        MealPlan plan = new MealPlan(USER_ID, "Thực đơn mẫu", "Giảm cân", samplePlanJson());
        MealPlanFavorite favorite = new MealPlanFavorite(USER_ID, plan);
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(favoriteRepository.findByUserIdAndMealPlanId(USER_ID, PLAN_ID)).thenReturn(Optional.of(favorite));

        assertDoesNotThrow(() -> service.unfavorite(USER_ID, PLAN_ID));
        verify(favoriteRepository, times(1)).delete(favorite);
    }

    @Test
    void unfavorite_notFavorited_throwsFavoriteNotFound() {
        MealPlan plan = new MealPlan(USER_ID, "Thực đơn mẫu", "Giảm cân", samplePlanJson());
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(favoriteRepository.findByUserIdAndMealPlanId(USER_ID, PLAN_ID)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.unfavorite(USER_ID, PLAN_ID));
        assertEquals(ErrorCode.FAVORITE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listFavorites_returnsPaginatedResponse() {
        MealPlan plan1 = new MealPlan(USER_ID, "Thực đơn 1", "Giảm cân", samplePlanJson());
        MealPlan plan2 = new MealPlan(USER_ID, "Thực đơn 2", "Tăng cơ", samplePlanJson());
        MealPlanFavorite fav1 = new MealPlanFavorite(USER_ID, plan1);
        MealPlanFavorite fav2 = new MealPlanFavorite(USER_ID, plan2);

        PageImpl<MealPlanFavorite> page = new PageImpl<>(List.of(fav1, fav2), PageRequest.of(0, 10), 2);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(page);
        when(favoriteRepository.existsByUserIdAndMealPlanId(anyLong(), anyLong())).thenReturn(true);

        PageResponse<MealPlanDtos.MealPlanResponse> result = service.listFavorites(USER_ID, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.getContent().get(0).isFavorite());
    }

    @Test
    void getFavoriteDetail_success() {
        MealPlan plan = new MealPlan(USER_ID, "Thực đơn 1", "Giảm cân", samplePlanJson());
        MealPlanFavorite fav = new MealPlanFavorite(USER_ID, plan);
        when(favoriteRepository.findByUserIdAndMealPlanId(USER_ID, PLAN_ID)).thenReturn(Optional.of(fav));
        when(favoriteRepository.existsByUserIdAndMealPlanId(USER_ID, plan.getId())).thenReturn(true);

        MealPlanDtos.MealPlanResponse res = service.getFavoriteDetail(USER_ID, PLAN_ID);
        assertNotNull(res);
        assertTrue(res.isFavorite());
        assertEquals("Thực đơn 1", res.name());
    }

    // ==========================================
    // FEATURE 2: HEALTH METRICS UPDATE & VERSIONS
    // ==========================================

    @Test
    void calculateTargetFromHealth_calculatesAppropriateMacros() {
        ObjectNode health = mapper.createObjectNode();
        health.put("heightCm", 170).put("weightKg", 65).put("bmi", 22.5);

        MealPlanDtos.NutritionTarget target = service.calculateTargetFromHealth(health, "Giảm mỡ toàn thân");

        assertNotNull(target);
        assertTrue(target.minCalories().compareTo(BigDecimal.valueOf(1400)) > 0);
        assertTrue(target.maxCalories().compareTo(target.minCalories()) > 0);
        assertTrue(target.minProtein().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(target.minCarbohydrate().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(target.minFat().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void updateFromHealth_previewWhenNotConfirmed() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        // Mock downstream responses
        ObjectNode healthResult = mapper.createObjectNode();
        healthResult.putObject("result").put("heightCm", 175).put("weightKg", 70).put("bmi", 22.9);

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));

        stubDownstreams(healthResult, allergyResult, recipeResult);

        MealPlanDtos.UpdateFromHealthRequest req = new MealPlanDtos.UpdateFromHealthRequest(false, null, null);
        MealPlanDtos.UpdateFromHealthResponse res = service.updateFromHealth(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertFalse(res.confirmed());
        assertNotNull(res.recalculatedTarget());
        assertNotNull(res.proposedPlan());
        assertNull(res.updatedMealPlan());

        // Verify entity was NOT saved and version was NOT archived
        verify(repository, never()).save(any());
        verify(versionRepository, never()).save(any());
    }

    @Test
    void updateFromHealth_persistsAndArchivesWhenConfirmed() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode healthResult = mapper.createObjectNode();
        healthResult.putObject("result").put("heightCm", 175).put("weightKg", 70).put("bmi", 22.9);

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));

        stubDownstreams(healthResult, allergyResult, recipeResult);

        MealPlanDtos.UpdateFromHealthRequest req = new MealPlanDtos.UpdateFromHealthRequest(true, "Cập nhật cân nặng mới", null);
        MealPlanDtos.UpdateFromHealthResponse res = service.updateFromHealth(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertTrue(res.confirmed());
        assertNotNull(res.updatedMealPlan());
        assertEquals(2, res.updatedMealPlan().version());

        // Verify version 1 was archived and entity updated to version 2
        verify(versionRepository, times(1)).save(argThat(v -> v.getVersion() == 1 && v.getChangeSummary().equals("Cập nhật cân nặng mới")));
        verify(repository, times(1)).save(plan);
        assertEquals(2, plan.getVersion());
    }

    @Test
    void updateFromHealth_throwsHealthRecordRequiredWhen404() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        RestClient.RequestHeadersUriSpec healthUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/health-records/latest")).thenReturn(healthUriSpec);
        when(healthUriSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(healthUriSpec);
        when(healthUriSpec.retrieve()).thenThrow(
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)
        );

        MealPlanDtos.UpdateFromHealthRequest req = new MealPlanDtos.UpdateFromHealthRequest(true, null, null);
        AppException ex = assertThrows(AppException.class, () -> service.updateFromHealth(USER_ID, PLAN_ID, "jwt-token", req));
        assertEquals(ErrorCode.HEALTH_RECORD_REQUIRED, ex.getErrorCode());
    }

    // ==========================================
    // FEATURE 3: MANUAL EDITING TESTS
    // ==========================================

    @Test
    void manualEdit_replaceRecipe_recalculatesMacro() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));
        content.add(sampleRecipe(2, "Canh nấm chay", true, false));

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "DINNER", "REPLACE", 2L, BigDecimal.valueOf(1.5), "Món canh dinh dưỡng"
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(false, "Đổi món bữa tối", List.of(edit));

        MealPlanDtos.MealPlanEditResponse res = service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertFalse(res.preview());
        assertNotNull(res.updatedMealPlan());
        assertEquals(2, res.updatedMealPlan().version());
        verify(versionRepository, times(1)).save(any(MealPlanVersion.class));
        verify(repository, times(1)).save(plan);
    }

    @Test
    void manualEdit_addMeal_recalculatesTotals() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));
        content.add(sampleRecipe(5, "Chè hạt sen", true, false));

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "SNACK", "ADD", 5L, BigDecimal.ONE, "Bữa phụ hạt sen"
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(false, "Thêm bữa phụ", List.of(edit));

        MealPlanDtos.MealPlanEditResponse res = service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertFalse(res.preview());
        assertEquals(2, res.updatedMealPlan().version());
    }

    @Test
    void manualEdit_removeMeal_recalculatesTotals() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "DINNER", "REMOVE", null, null, null
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(false, "Bỏ bữa tối", List.of(edit));

        MealPlanDtos.MealPlanEditResponse res = service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertFalse(res.preview());
        assertEquals(2, res.updatedMealPlan().version());
    }

    @Test
    void manualEdit_updateServingMultiplier_scalesNutrition() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "BREAKFAST", "UPDATE_SERVING", null, BigDecimal.valueOf(2.0), null
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(false, "Tăng gấp đôi phần ăn sáng", List.of(edit));

        MealPlanDtos.MealPlanEditResponse res = service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertFalse(res.preview());
        assertEquals(2, res.updatedMealPlan().version());
    }

    @Test
    void manualEdit_rejectsNonVeganRecipe() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(99, "Món có trứng sữa", false, false));

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "LUNCH", "REPLACE", 99L, BigDecimal.ONE, null
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(false, null, List.of(edit));

        AppException ex = assertThrows(AppException.class, () -> service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req));
        assertEquals(ErrorCode.RECIPE_NOT_VEGAN, ex.getErrorCode());
        assertEquals(400, ex.getErrorCode().status.value());
    }

    @Test
    void manualEdit_rejectsAllergenRecipe() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result").addObject().put("code", "DAU_PHONG").put("name", "Đậu phộng");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        ObjectNode r = sampleRecipe(88, "Món đậu phộng", true, true);
        ((ObjectNode) r.withArray("ingredients").get(0)).put("ingredientName", "Đậu phộng rang").put("normalizedName", "dau phong rang");
        content.add(r);

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "LUNCH", "REPLACE", 88L, BigDecimal.ONE, null
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(false, null, List.of(edit));

        AppException ex = assertThrows(AppException.class, () -> service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req));
        assertEquals(ErrorCode.RECIPE_CONTAINS_ALLERGEN, ex.getErrorCode());
        assertEquals(400, ex.getErrorCode().status.value());
    }

    @Test
    void manualEdit_previewMode_doesNotPersistOrArchive() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");

        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        content.add(sampleRecipe(1, "Đậu hũ kho", true, false));

        stubAllergiesAndRecipes(allergyResult, recipeResult);

        MealPlanDtos.MealEditItem edit = new MealPlanDtos.MealEditItem(
                "MONDAY", "BREAKFAST", "UPDATE_SERVING", null, BigDecimal.valueOf(1.2), null
        );
        MealPlanDtos.ManualEditRequest req = new MealPlanDtos.ManualEditRequest(true, null, List.of(edit));

        MealPlanDtos.MealPlanEditResponse res = service.manualEdit(USER_ID, PLAN_ID, "jwt-token", req);

        assertNotNull(res);
        assertTrue(res.preview());
        assertNull(res.updatedMealPlan());
        assertNotNull(res.plan());

        verify(repository, never()).save(any());
        verify(versionRepository, never()).save(any());
    }

    @Test
    void listVersions_and_getVersionDetail() {
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 2);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));

        MealPlanVersion v1 = new MealPlanVersion(plan, 1, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), "Bản khởi tạo");
        when(versionRepository.findByMealPlanIdOrderByVersionDesc(PLAN_ID)).thenReturn(List.of(v1));
        when(versionRepository.findByMealPlanIdAndVersion(PLAN_ID, 1)).thenReturn(Optional.of(v1));

        List<MealPlanDtos.MealPlanVersionResponse> list = service.listVersions(USER_ID, PLAN_ID);
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).version());

        MealPlanDtos.MealPlanVersionResponse detail = service.getVersion(USER_ID, PLAN_ID, 1);
        assertEquals(1, detail.version());
        assertEquals("Bản khởi tạo", detail.changeSummary());
    }

    // ==========================================
    // HELPER STUBS
    // ==========================================

    private void stubDownstreams(ObjectNode healthResult, ObjectNode allergyResult, ObjectNode recipeResult) {
        RestClient.RequestHeadersUriSpec healthUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec healthRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/health-records/latest")).thenReturn(healthUriSpec);
        when(healthUriSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(healthUriSpec);
        when(healthUriSpec.retrieve()).thenReturn(healthRespSpec);
        when(healthRespSpec.body(any(Class.class))).thenReturn(healthResult);

        stubAllergiesAndRecipes(allergyResult, recipeResult);
    }

    private void stubAllergiesAndRecipes(ObjectNode allergyResult, ObjectNode recipeResult) {
        RestClient.RequestHeadersUriSpec allergyUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec allergyRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/allergies")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(allergyUriSpec);
        when(allergyUriSpec.retrieve()).thenReturn(allergyRespSpec);
        when(allergyRespSpec.body(any(Class.class))).thenReturn(allergyResult);

        RestClient.RequestHeadersUriSpec recipeUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec recipeRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://recipe-service/recipes?size=200")).thenReturn(recipeUriSpec);
        when(recipeUriSpec.retrieve()).thenReturn(recipeRespSpec);
        when(recipeRespSpec.body(any(Class.class))).thenReturn(recipeResult);
    }
}
