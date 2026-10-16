package com.veggiepal.ai.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RecommendationServiceTest {

    private RestClient http;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.ResponseSpec responseSpec;
    private GroqClient groq;
    private ModelProperties models;
    private ObjectMapper mapper;
    private UsageService usage;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        http = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        groq = mock(GroqClient.class);
        models = new ModelProperties("chat-model", "meal-model", "rec-model", "profile-model", "video-model", "whisper-model");
        mapper = new ObjectMapper();
        usage = mock(UsageService.class);

        when(http.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);

        service = new RecommendationService(
                http,
                "http://blog-service",
                "http://recipe-service",
                groq,
                models,
                mapper,
                usage
        );
    }

    @Test
    void recommendReturnsEmptyListWhenNoCandidatesFound() throws Exception {
        JsonNode emptyBlog = mapper.readTree("{\"result\":{\"items\":[]}}");
        JsonNode emptyRecipe = mapper.readTree("{\"result\":{\"content\":[]}}");

        when(uriSpec.uri(contains("http://blog-service"))).thenReturn(uriSpec);
        when(uriSpec.uri(contains("http://recipe-service"))).thenReturn(uriSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(emptyBlog, emptyRecipe);

        List<RecommendationDtos.Recommendation> result = service.recommend(1L, "vegan salad", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(groq);
    }

    @Test
    void recommendSuccessfullyRanksAndReturnsCandidates() throws Exception {
        JsonNode blogData = mapper.readTree("{\"result\":{\"items\":[{\"id\":10,\"title\":\"Vegan Salad Guide\"}]}}");
        JsonNode recipeData = mapper.readTree("{\"result\":{\"content\":[{\"id\":20,\"title\":\"Avocado Salad\"}]}}");

        when(responseSpec.body(JsonNode.class)).thenReturn(blogData, recipeData);

        String groqOutput = "[{\"type\":\"BLOG\",\"id\":10,\"reason\":\"Great salad tips\"},{\"type\":\"RECIPE\",\"id\":20,\"reason\":\"Fresh salad recipe\"}]";
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult(groqOutput, 100, 50));

        List<RecommendationDtos.Recommendation> result = service.recommend(1L, "salad", 5);

        assertEquals(2, result.size());
        assertEquals("BLOG", result.get(0).type());
        assertEquals(10L, result.get(0).id());
        assertEquals("Great salad tips", result.get(0).reason());
        assertEquals("RECIPE", result.get(1).type());
        assertEquals(20L, result.get(1).id());
        assertEquals("Fresh salad recipe", result.get(1).reason());
        verify(usage).checkBudget("user:1");
    }

    @Test
    void recommendThrowsInvalidAiResponseWhenGroqReturnsNonArray() throws Exception {
        JsonNode blogData = mapper.readTree("{\"result\":{\"items\":[{\"id\":10,\"title\":\"Vegan Salad Guide\"}]}}");
        JsonNode recipeData = mapper.readTree("{\"result\":{\"content\":[]}}");

        when(responseSpec.body(JsonNode.class)).thenReturn(blogData, recipeData);

        String groqOutput = "{\"message\":\"not an array\"}";
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult(groqOutput, 100, 50));

        AppException ex = assertThrows(AppException.class, () -> service.recommend(1L, "salad", 5));
        assertEquals(ErrorCode.INVALID_AI_RESPONSE, ex.getErrorCode());
    }

    @Test
    void recommendThrowsDownstreamUnavailableWhenRestClientFails() {
        when(responseSpec.body(JsonNode.class)).thenThrow(new RuntimeException("Connection refused"));

        AppException ex = assertThrows(AppException.class, () -> service.recommend(1L, "salad", 5));
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
    }
}
