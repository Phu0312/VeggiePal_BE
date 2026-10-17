package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
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
                validator
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
        when(http.get().uri("http://recipe-service/recipes?size=100")).thenReturn(recipeUriSpec);
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
        when(http.get().uri("http://recipe-service/recipes?size=100")).thenReturn(recipeUriSpec);
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
        when(http.get().uri("http://recipe-service/recipes?size=100")).thenReturn(recipeUriSpec);
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
}
