package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.quota.PremiumQuotaService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class MealPlanServiceTest {

    private MealPlanRepository repository;
    private RestClient http;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.ResponseSpec responseSpec;
    private GroqClient groq;
    private ModelProperties models;
    private ObjectMapper mapper;
    private UsageService usage;
    private MealPlanValidator validator;
    private PremiumQuotaService premiumQuota;
    private MealPlanService service;

    @BeforeEach
    void setUp() {
        repository = mock(MealPlanRepository.class);
        http = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        groq = mock(GroqClient.class);
        models = new ModelProperties("chat-model", "meal-model", "rec-model", "profile-model", "video-model", "whisper-model");
        mapper = new ObjectMapper();
        usage = mock(UsageService.class);
        validator = new MealPlanValidator(mapper);
        premiumQuota = mock(PremiumQuotaService.class);

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
                premiumQuota
        );
    }

    @Test
    void throwsHealthRecordRequiredWhenNutritionReturns404() {
        // Mock recipes response: 200
        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        ObjectNode recipe = content.addObject();
        recipe.put("id", 1).put("title", "Đậu hũ kho").put("servings", 1);
        recipe.putArray("ingredients").addObject()
                .put("ingredientName", "Đậu hũ").put("normalizedName", "dau hu")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 76).put("proteinPer100g", 8)
                .put("carbsPer100g", 2).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);

        // Mock allergies response: 200 with empty list
        ObjectNode allergiesResult = mapper.createObjectNode();
        allergiesResult.putArray("result");

        when(responseSpec.body(any(Class.class))).thenAnswer(invocation -> {
            // Check url or sequence: first call is recipes, second is allergies, third is health record
            return recipeResult;
        });

        // Use specific uri stubbing
        RestClient.RequestHeadersUriSpec recipeUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec recipeRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://recipe-service/recipes?size=200")).thenReturn(recipeUriSpec);
        when(recipeUriSpec.retrieve()).thenReturn(recipeRespSpec);
        when(recipeRespSpec.body(any(Class.class))).thenReturn(recipeResult);

        RestClient.RequestHeadersUriSpec allergyUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec allergyRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/allergies")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.retrieve()).thenReturn(allergyRespSpec);
        when(allergyRespSpec.body(any(Class.class))).thenReturn(allergiesResult);

        // Health record endpoint returns 404
        RestClient.RequestHeadersUriSpec healthUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/health-records/latest")).thenReturn(healthUriSpec);
        when(healthUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(healthUriSpec);
        when(healthUriSpec.retrieve()).thenThrow(
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)
        );

        MealPlanDtos.GenerateRequest request = new MealPlanDtos.GenerateRequest(
                "Kế hoạch tuần",
                "Giảm mỡ",
                List.of(),
                List.of(),
                null,
                GoalType.MAINTAIN
        );

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", request));
        assertEquals(ErrorCode.HEALTH_RECORD_REQUIRED, ex.getErrorCode());
        assertEquals("Vui lòng cập nhật chiều cao và cân nặng trong hồ sơ sức khỏe trước khi lập thực đơn.", ex.getMessage());
    }

    @Test
    void throwsDownstreamUnavailableWhenNutritionTimesOutOrFails() {
        ObjectNode recipeResult = mapper.createObjectNode();
        recipeResult.putObject("result").putArray("content");

        ObjectNode allergiesResult = mapper.createObjectNode();
        allergiesResult.putArray("result");

        RestClient.RequestHeadersUriSpec recipeUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec recipeRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://recipe-service/recipes?size=200")).thenReturn(recipeUriSpec);
        when(recipeUriSpec.retrieve()).thenReturn(recipeRespSpec);
        when(recipeRespSpec.body(any(Class.class))).thenReturn(recipeResult);

        RestClient.RequestHeadersUriSpec allergyUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec allergyRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/allergies")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.retrieve()).thenReturn(allergyRespSpec);
        when(allergyRespSpec.body(any(Class.class))).thenReturn(allergiesResult);

        // Health record endpoint throws 503 Service Unavailable
        RestClient.RequestHeadersUriSpec healthUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/health-records/latest")).thenReturn(healthUriSpec);
        when(healthUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(healthUriSpec);
        when(healthUriSpec.retrieve()).thenThrow(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)
        );

        MealPlanDtos.GenerateRequest request = new MealPlanDtos.GenerateRequest(
                "Kế hoạch tuần",
                "Tăng cơ",
                List.of(),
                List.of(),
                null,
                GoalType.MAINTAIN
        );

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", request));
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    void throwsDownstreamUnavailableWhenNetworkConnectionTimesOut() {
        RestClient.RequestHeadersUriSpec recipeUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(http.get().uri("http://recipe-service/recipes?size=200")).thenReturn(recipeUriSpec);
        when(recipeUriSpec.retrieve()).thenThrow(new ResourceAccessException("Connection timed out"));

        MealPlanDtos.GenerateRequest request = new MealPlanDtos.GenerateRequest(
                "Kế hoạch tuần",
                "Tăng cơ",
                List.of(),
                List.of(),
                null,
                GoalType.MAINTAIN
        );

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", request));
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    void generateCompletes21MealPlanSuccessfullyWithDeterministicFallbackOnAiError() {
        // Recipe mock
        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        ObjectNode r1 = content.addObject().put("id", 1).put("title", "Đậu hũ kho").put("servings", 1);
        r1.putArray("ingredients").addObject()
                .put("ingredientName", "Đậu hũ").put("normalizedName", "dau hu")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 76).put("proteinPer100g", 8)
                .put("carbsPer100g", 2).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);

        RestClient.RequestHeadersUriSpec recipeUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec recipeRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://recipe-service/recipes?size=200")).thenReturn(recipeUriSpec);
        when(recipeUriSpec.retrieve()).thenReturn(recipeRespSpec);
        when(recipeRespSpec.body(any(Class.class))).thenReturn(recipeResult);

        // Allergy mock
        ObjectNode allergiesResult = mapper.createObjectNode();
        allergiesResult.putArray("result");
        RestClient.RequestHeadersUriSpec allergyUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec allergyRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/allergies")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.retrieve()).thenReturn(allergyRespSpec);
        when(allergyRespSpec.body(any(Class.class))).thenReturn(allergiesResult);

        // Health record mock
        ObjectNode healthResult = mapper.createObjectNode();
        healthResult.putObject("result").put("weightKg", 65).put("heightCm", 170);
        RestClient.RequestHeadersUriSpec healthUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec healthRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/health-records/latest")).thenReturn(healthUriSpec);
        when(healthUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(healthUriSpec);
        when(healthUriSpec.retrieve()).thenReturn(healthRespSpec);
        when(healthRespSpec.body(any(Class.class))).thenReturn(healthResult);

        stubProfile("1995-01-01", "FEMALE", "LIGHT");

        // Groq fails with an exception
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenThrow(new RuntimeException("Groq timeout or server error"));

        // Save mock
        when(repository.save(any(MealPlan.class))).thenAnswer(invocation -> {
            MealPlan plan = invocation.getArgument(0);
            return new MealPlan(plan.getUserId(), plan.getName(), plan.getGoal(), plan.getPlanJson());
        });

        MealPlanDtos.GenerateRequest request = new MealPlanDtos.GenerateRequest(
                "Kế hoạch 7 ngày",
                "Ăn lành mạnh",
                List.of(),
                List.of(),
                null,
                GoalType.MAINTAIN
        );

        MealPlanDtos.MealPlanResponse response = service.generate(1L, "test-jwt", request);
        verify(groq).complete(any(), argThat((List<com.veggiepal.ai.groq.dto.GroqRequest.Message> messages) ->
                messages.getLast().content().contains("note viết bằng tiếng Việt")), anyDouble(), anyInt());

        assertEquals("Kế hoạch 7 ngày", response.name());
        com.fasterxml.jackson.databind.JsonNode planNode = mapper.valueToTree(response.plan());
        com.fasterxml.jackson.databind.node.ArrayNode days = (com.fasterxml.jackson.databind.node.ArrayNode) planNode.path("days");
        assertEquals(7, days.size());

        int totalMeals = 0;
        for (int i = 0; i < 7; i++) {
            com.fasterxml.jackson.databind.node.ArrayNode meals = (com.fasterxml.jackson.databind.node.ArrayNode) days.get(i).path("meals");
            assertEquals(3, meals.size());
            totalMeals += meals.size();
        }
        assertEquals(21, totalMeals);
    }

    @Test
    void replaceMealSuccessfullyUpdatesRecipeAndEnrichesNutrition() {
        // Initial plan json
        ObjectNode initialPlan = mapper.createObjectNode();
        ArrayNode days = initialPlan.putArray("days");
        for (String dayName : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) {
            ObjectNode d = days.addObject().put("day", dayName);
            ArrayNode meals = d.putArray("meals");
            meals.addObject().put("mealType", "BREAKFAST").put("recipeId", 1L);
            meals.addObject().put("mealType", "LUNCH").put("recipeId", 1L);
            meals.addObject().put("mealType", "DINNER").put("recipeId", 1L);
        }
        ObjectNode constraints = initialPlan.putObject("constraints");
        constraints.putArray("availableIngredients");

        MealPlan existingPlan = new MealPlan(1L, "Thực đơn mẫu", "Sức khỏe", initialPlan.toString());
        when(repository.findByIdAndUserId(100L, 1L)).thenReturn(java.util.Optional.of(existingPlan));

        // Recipe mock with recipe 1 and recipe 2
        ObjectNode recipeResult = mapper.createObjectNode();
        ArrayNode content = recipeResult.putObject("result").putArray("content");
        ObjectNode r1 = content.addObject().put("id", 1).put("title", "Đậu hũ kho").put("servings", 1);
        r1.putArray("ingredients").addObject()
                .put("ingredientName", "Đậu hũ").put("normalizedName", "dau hu")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 76).put("proteinPer100g", 8)
                .put("carbsPer100g", 2).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);

        ObjectNode r2 = content.addObject().put("id", 2).put("title", "Canh cà rốt").put("servings", 1);
        r2.putArray("ingredients").addObject()
                .put("ingredientName", "Cà rốt").put("normalizedName", "ca rot")
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", 41).put("proteinPer100g", 1)
                .put("carbsPer100g", 10).put("fatPer100g", 0.2)
                .put("vegan", true).put("allergen", false);

        RestClient.RequestHeadersUriSpec recipeUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec recipeRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://recipe-service/recipes?size=200")).thenReturn(recipeUriSpec);
        when(recipeUriSpec.retrieve()).thenReturn(recipeRespSpec);
        when(recipeRespSpec.body(any(Class.class))).thenReturn(recipeResult);

        // Allergy mock
        ObjectNode allergiesResult = mapper.createObjectNode();
        allergiesResult.putArray("result");
        RestClient.RequestHeadersUriSpec allergyUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec allergyRespSpec = mock(RestClient.ResponseSpec.class);
        when(http.get().uri("http://nutrition-service/nutrition/me/allergies")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(allergyUriSpec);
        when(allergyUriSpec.retrieve()).thenReturn(allergyRespSpec);
        when(allergyRespSpec.body(any(Class.class))).thenReturn(allergiesResult);

        // Groq returns replacement with recipeId = 2
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenReturn(new com.veggiepal.ai.groq.GroqClient.GroqResult(
                        "{\"recipeId\": 2, \"note\": \"Thay món giàu vitamin\"}", 100, 50));

        MealPlanDtos.ReplaceMealRequest replaceRequest = new MealPlanDtos.ReplaceMealRequest(
                "MONDAY", "BREAKFAST", 1L
        );

        MealPlanDtos.MealPlanResponse response = service.replace(1L, 100L, "test-jwt", replaceRequest);
        verify(groq).complete(any(), argThat((List<com.veggiepal.ai.groq.dto.GroqRequest.Message> messages) ->
                messages.getLast().content().contains("note viết bằng tiếng Việt")), anyDouble(), anyInt());

        com.fasterxml.jackson.databind.JsonNode updatedDay = mapper.valueToTree(response.plan()).path("days").get(0);
        assertEquals("MONDAY", updatedDay.path("day").asText());
        com.fasterxml.jackson.databind.JsonNode updatedBreakfast = updatedDay.path("meals").get(0);
        assertEquals("BREAKFAST", updatedBreakfast.path("mealType").asText());
        assertEquals(2L, updatedBreakfast.path("recipeId").asLong());
        assertEquals(0, new BigDecimal("41.00").compareTo(updatedBreakfast.path("nutrition").path("calories").decimalValue()));
    }

    @Test
    void replaceMealFailsWhenNotOwnedByUser() {
        when(repository.findByIdAndUserId(100L, 999L)).thenReturn(java.util.Optional.empty());

        MealPlanDtos.ReplaceMealRequest replaceRequest = new MealPlanDtos.ReplaceMealRequest(
                "MONDAY", "BREAKFAST", 1L
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.replace(999L, 100L, "test-jwt", replaceRequest));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    private void addRecipe(ArrayNode content, long id, int kcal) {
        content.addObject().put("id", id).put("title", "Món " + id).put("servings", 1)
                .putArray("ingredients").addObject()
                .put("ingredientName", "Nguyên liệu " + id).put("normalizedName", "nguyen lieu " + id)
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", kcal).put("proteinPer100g", 10)
                .put("carbsPer100g", 20).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);
    }

    private void stubGet(String url, ObjectNode body, boolean withJwt) {
        RestClient.RequestHeadersUriSpec spec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec resp = mock(RestClient.ResponseSpec.class);
        when(http.get().uri(url)).thenReturn(spec);
        if (withJwt) when(spec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(spec);
        when(spec.retrieve()).thenReturn(resp);
        when(resp.body(any(Class.class))).thenReturn(body);
    }

    private void stubProfile(String dob, String gender, String activity) {
        ObjectNode profile = mapper.createObjectNode();
        profile.putObject("result").put("dateOfBirth", dob).put("gender", gender).put("activityLevel", activity);
        stubGet("http://identity-service/users/me", profile, true);
    }

    private void stubGenerateDownstreams() {
        ObjectNode recipes = mapper.createObjectNode();
        addRecipe(recipes.putObject("result").putArray("content"), 1, 600);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);
        ObjectNode health = mapper.createObjectNode();
        health.putObject("result").put("weightKg", 80).put("heightCm", 180);
        stubGet("http://nutrition-service/nutrition/me/health-records/latest", health, true);
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class))).thenThrow(new RuntimeException("Groq down"));
        when(repository.save(any(MealPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubProfileJson(java.util.function.Consumer<ObjectNode> fields) {
        ObjectNode profile = mapper.createObjectNode();
        fields.accept(profile.putObject("result"));
        stubGet("http://identity-service/users/me", profile, true);
    }

    private MealPlanDtos.GenerateRequest requestWithoutTarget() {
        return new MealPlanDtos.GenerateRequest("Kế hoạch", "Giữ cân", List.of(), List.of(), null, GoalType.MAINTAIN);
    }

    @Test
    void generateWithNullGenderThrowsProfileIncompleteWithoutSpendingAQuotaSlot() {
        stubGenerateDownstreams();
        stubProfileJson(p -> p.put("dateOfBirth", "1995-01-01").putNull("gender").put("activityLevel", "LIGHT"));

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", requestWithoutTarget()));

        assertEquals(ErrorCode.PROFILE_INCOMPLETE, ex.getErrorCode());
        verify(premiumQuota, org.mockito.Mockito.never()).reserveMealPlan(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void generateWithMissingActivityLevelThrowsProfileIncompleteWithoutSpendingAQuotaSlot() {
        stubGenerateDownstreams();
        stubProfileJson(p -> p.put("dateOfBirth", "1995-01-01").put("gender", "FEMALE"));

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", requestWithoutTarget()));

        assertEquals(ErrorCode.PROFILE_INCOMPLETE, ex.getErrorCode());
        verify(premiumQuota, org.mockito.Mockito.never()).reserveMealPlan(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void generateWithMalformedDateOfBirthThrowsProfileIncomplete() {
        stubGenerateDownstreams();
        stubProfileJson(p -> p.put("dateOfBirth", "not-a-date").put("gender", "FEMALE").put("activityLevel", "LIGHT"));

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", requestWithoutTarget()));

        assertEquals(ErrorCode.PROFILE_INCOMPLETE, ex.getErrorCode());
        verify(premiumQuota, org.mockito.Mockito.never()).reserveMealPlan(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void generateReservesTheQuotaSlotOnlyOnceTheTargetIsResolved() {
        stubGenerateDownstreams();
        stubProfile(java.time.LocalDate.now().minusYears(25).toString(), "MALE", "LIGHT");

        service.generate(1L, "test-jwt", requestWithoutTarget());

        verify(premiumQuota).reserveMealPlan(1L);
    }

    @Test
    void generateWhoseQuotaIsExhaustedThrowsTheQuotaErrorAndNeverCallsGroq() {
        stubGenerateDownstreams();
        stubProfile(java.time.LocalDate.now().minusYears(25).toString(), "MALE", "LIGHT");
        when(premiumQuota.reserveMealPlan(1L)).thenThrow(new AppException(ErrorCode.QUOTA_EXCEEDED));

        AppException ex = assertThrows(AppException.class, () -> service.generate(1L, "test-jwt", requestWithoutTarget()));

        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.getErrorCode());
        verify(groq, org.mockito.Mockito.never()).complete(any(), any(), any(Double.class), any(Integer.class));
    }

    @Test
    void generateWithoutTargetUsesTheTargetComputedFromTheProfile() {
        stubGenerateDownstreams();
        // 25 years old today, so the expected numbers match the calculator's male/LIGHT/GAIN example
        stubProfile(java.time.LocalDate.now().minusYears(25).toString(), "MALE", "LIGHT");

        MealPlanDtos.MealPlanResponse response = service.generate(1L, "test-jwt", new MealPlanDtos.GenerateRequest(
                "Kế hoạch", "Tăng cơ", List.of(), List.of(), null, GoalType.GAIN));

        var target = mapper.valueToTree(response.plan()).path("constraints").path("dailyNutritionTarget");
        assertEquals(2569, target.path("minCalories").asInt());
        assertEquals(3140, target.path("maxCalories").asInt());
        assertEquals("GAIN", response.goalType());
    }

    @Test
    void generateWithAnExplicitTargetKeepsItAndNeverCallsIdentity() {
        stubGenerateDownstreams();

        MealPlanDtos.MealPlanResponse response = service.generate(1L, "test-jwt", new MealPlanDtos.GenerateRequest(
                "Kế hoạch", "Tăng cơ", List.of(), List.of(),
                new MealPlanDtos.NutritionTarget(new BigDecimal("2000"), new BigDecimal("2200"),
                        null, null, null, null, null, null), GoalType.GAIN));

        var target = mapper.valueToTree(response.plan()).path("constraints").path("dailyNutritionTarget");
        assertEquals(2000, target.path("minCalories").asInt());
        verify(http.get(), org.mockito.Mockito.never()).uri("http://identity-service/users/me");
    }

    @Test
    void generatePromptCarriesTheMealBudgets() {
        ObjectNode recipes = mapper.createObjectNode();
        addRecipe(recipes.putObject("result").putArray("content"), 1, 600);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);
        ObjectNode health = mapper.createObjectNode();
        health.putObject("result").put("weightKg", 65).put("heightCm", 170);
        stubGet("http://nutrition-service/nutrition/me/health-records/latest", health, true);
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenThrow(new RuntimeException("Groq down"));
        when(repository.save(any(MealPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(1L, "test-jwt", new MealPlanDtos.GenerateRequest(
                "Kế hoạch", "Tăng cân", List.of(), List.of(),
                new MealPlanDtos.NutritionTarget(new BigDecimal("2400"), new BigDecimal("2600"),
                        null, null, null, null, null, null), GoalType.MAINTAIN));

        verify(groq).complete(any(), argThat((List<com.veggiepal.ai.groq.dto.GroqRequest.Message> messages) ->
                messages.getLast().content().contains("LUNCH khoảng 1000 kcal")), anyDouble(), anyInt());
    }

    @Test
    void replaceRejectsAnAiPickThatCannotReachTheMealBudgetAndLeavesOtherMealsAlone() {
        ObjectNode stored = mapper.createObjectNode();
        ArrayNode days = stored.putArray("days");
        for (String dayName : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) {
            ArrayNode meals = days.addObject().put("day", dayName).putArray("meals");
            meals.addObject().put("mealType", "BREAKFAST").put("recipeId", 3L);
            meals.addObject().put("mealType", "LUNCH").put("recipeId", 4L);
            meals.addObject().put("mealType", "DINNER").put("recipeId", 5L);
        }
        ObjectNode constraints = stored.putObject("constraints");
        constraints.putArray("availableIngredients");
        constraints.putObject("dailyNutritionTarget").put("minCalories", 2522).put("maxCalories", 3082);
        when(repository.findByIdAndUserId(100L, 1L))
                .thenReturn(java.util.Optional.of(new MealPlan(1L, "Thực đơn", "Tăng cân", stored.toString())));

        ObjectNode recipes = mapper.createObjectNode();
        ArrayNode content = recipes.putObject("result").putArray("content");
        addRecipe(content, 1, 120);
        addRecipe(content, 3, 500);
        addRecipe(content, 4, 700);
        addRecipe(content, 5, 650);
        addRecipe(content, 6, 800);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);

        // The AI suggests the 120 kcal recipe for a ~1121 kcal lunch: 120 x 2.00 can never reach it.
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenReturn(new com.veggiepal.ai.groq.GroqClient.GroqResult("{\"recipeId\": 1, \"note\": \"Món nhẹ\"}", 100, 50));

        MealPlanDtos.MealPlanResponse response = service.replace(1L, 100L, "test-jwt",
                new MealPlanDtos.ReplaceMealRequest("MONDAY", "LUNCH", 4L));

        verify(groq).complete(any(), argThat((List<com.veggiepal.ai.groq.dto.GroqRequest.Message> messages) ->
                messages.getLast().content().contains("khoảng 1121 kcal")), anyDouble(), anyInt());
        var planDays = mapper.valueToTree(response.plan()).path("days");
        // 6 is the only reachable recipe that is neither the current lunch (4) nor another Monday meal (3, 5).
        assertEquals(6L, planDays.get(0).path("meals").get(1).path("recipeId").asLong());
        assertEquals("Món ăn thay thế dinh dưỡng", planDays.get(0).path("meals").get(1).path("note").asText(),
                "the AI's note described the rejected recipe");
        for (int d = 0; d < 7; d++) {
            var meals = planDays.get(d).path("meals");
            assertEquals(3L, meals.get(0).path("recipeId").asLong());
            assertEquals(5L, meals.get(2).path("recipeId").asLong());
            if (d > 0) assertEquals(4L, meals.get(1).path("recipeId").asLong());
        }
    }

    @Test
    void replaceFallsBackToAnySafeRecipeWhenTheSmallPoolLeavesNothingAfterSameDayExclusion() {
        ObjectNode stored = mapper.createObjectNode();
        ArrayNode days = stored.putArray("days");
        for (String dayName : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) {
            ArrayNode meals = days.addObject().put("day", dayName).putArray("meals");
            meals.addObject().put("mealType", "BREAKFAST").put("recipeId", 1L);
            // Monday's lunch already uses the only other recipe, so excluding the day's meals leaves nothing.
            meals.addObject().put("mealType", "LUNCH").put("recipeId", dayName.equals("MONDAY") ? 2L : 1L);
            meals.addObject().put("mealType", "DINNER").put("recipeId", 1L);
        }
        ObjectNode constraints = stored.putObject("constraints");
        constraints.putArray("availableIngredients");
        constraints.putObject("dailyNutritionTarget").put("minCalories", 2522).put("maxCalories", 3082);
        when(repository.findByIdAndUserId(100L, 1L))
                .thenReturn(java.util.Optional.of(new MealPlan(1L, "Thực đơn", "Tăng cân", stored.toString())));

        ObjectNode recipes = mapper.createObjectNode();
        ArrayNode content = recipes.putObject("result").putArray("content");
        addRecipe(content, 1, 700);
        addRecipe(content, 2, 650);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);

        // Rejected: it is the current recipe.
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenReturn(new com.veggiepal.ai.groq.GroqClient.GroqResult("{\"recipeId\": 1}", 100, 50));

        MealPlanDtos.MealPlanResponse response = service.replace(1L, 100L, "test-jwt",
                new MealPlanDtos.ReplaceMealRequest("MONDAY", "BREAKFAST", 1L));

        var monday = mapper.valueToTree(response.plan()).path("days").get(0).path("meals");
        assertEquals(2L, monday.get(0).path("recipeId").asLong());
    }

    @Test
    void replaceWithoutACurrentRecipeFallsBackToAnySafeRecipeWhenTheSmallPoolIsUsedUp() {
        ObjectNode stored = mapper.createObjectNode();
        ArrayNode days = stored.putArray("days");
        for (String dayName : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) {
            ArrayNode meals = days.addObject().put("day", dayName).putArray("meals");
            meals.addObject().put("mealType", "BREAKFAST").put("recipeId", 1L);
            // Monday's lunch already uses the only other recipe, so excluding the day's meals leaves nothing.
            meals.addObject().put("mealType", "LUNCH").put("recipeId", dayName.equals("MONDAY") ? 2L : 1L);
            meals.addObject().put("mealType", "DINNER").put("recipeId", 1L);
        }
        ObjectNode constraints = stored.putObject("constraints");
        constraints.putArray("availableIngredients");
        constraints.putObject("dailyNutritionTarget").put("minCalories", 2522).put("maxCalories", 3082);
        when(repository.findByIdAndUserId(100L, 1L))
                .thenReturn(java.util.Optional.of(new MealPlan(1L, "Thực đơn", "Tăng cân", stored.toString())));

        ObjectNode recipes = mapper.createObjectNode();
        ArrayNode content = recipes.putObject("result").putArray("content");
        addRecipe(content, 1, 700);
        addRecipe(content, 2, 650);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);

        // Rejected: it is used by another meal of Monday.
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenReturn(new com.veggiepal.ai.groq.GroqClient.GroqResult("{\"recipeId\": 1}", 100, 50));

        MealPlanDtos.MealPlanResponse response = service.replace(1L, 100L, "test-jwt",
                new MealPlanDtos.ReplaceMealRequest("MONDAY", "BREAKFAST", null));

        var monday = mapper.valueToTree(response.plan()).path("days").get(0).path("meals");
        assertEquals(true, Set.of(1L, 2L).contains(monday.get(0).path("recipeId").asLong()));
    }
}
