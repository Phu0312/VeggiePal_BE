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
                mock(PremiumQuotaService.class)
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
                null
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
                null
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
                null
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
                null
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
}
