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
        JsonNode emptyRecipe = mapper.readTree("{\"result\":{\"items\":[]}}");

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
        JsonNode recipeData = mapper.readTree("{\"result\":{\"items\":[{\"id\":20,\"title\":\"Avocado Salad\"}]}}");

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
    void recommendReturnsContentThatSpringMvcWritesAsJson() throws Exception {
        JsonNode blogData = mapper.readTree("{\"result\":{\"items\":[{\"id\":10,\"title\":\"Vegan Salad Guide\"}]}}");
        JsonNode recipeData = mapper.readTree("{\"result\":{\"items\":[]}}");
        when(responseSpec.body(JsonNode.class)).thenReturn(blogData, recipeData);
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult("[{\"type\":\"BLOG\",\"id\":10,\"reason\":\"tips\"}]", 100, 50));

        // Spring MVC writes responses with Jackson 3, not this service's Jackson 2 mapper.
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var written = json.readTree(json.writeValueAsString(service.recommend(1L, "salad", 5)));

        assertEquals("Vegan Salad Guide", written.path(0).path("content").path("title").asString());
    }

    @Test
    void recommendThrowsInvalidAiResponseWhenGroqReturnsNonArray() throws Exception {
        JsonNode blogData = mapper.readTree("{\"result\":{\"items\":[{\"id\":10,\"title\":\"Vegan Salad Guide\"}]}}");
        JsonNode recipeData = mapper.readTree("{\"result\":{\"items\":[]}}");

        when(responseSpec.body(JsonNode.class)).thenReturn(blogData, recipeData);

        String groqOutput = "{\"message\":\"not an array\"}";
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult(groqOutput, 100, 50));

        AppException ex = assertThrows(AppException.class, () -> service.recommend(1L, "salad", 5));
        assertEquals(ErrorCode.INVALID_AI_RESPONSE, ex.getErrorCode());
    }

    @Test
    void recommendStillUsesRecipesWhenBlogServiceIsDown() throws Exception {
        JsonNode recipeData = mapper.readTree("{\"result\":{\"items\":[{\"id\":20,\"title\":\"Avocado Salad\"}]}}");
        when(responseSpec.body(JsonNode.class)).thenThrow(new RuntimeException("Connection refused")).thenReturn(recipeData);
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult("[{\"type\":\"RECIPE\",\"id\":20,\"reason\":\"fresh\"}]", 100, 50));

        List<RecommendationDtos.Recommendation> result = service.recommend(1L, null, 5);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).id());
    }

    @Test
    void recommendFallsBackToLatestItemsWhenKeywordMatchesNothing() throws Exception {
        JsonNode emptyBlog = mapper.readTree("{\"result\":{\"items\":[]}}");
        JsonNode emptyRecipe = mapper.readTree("{\"result\":{\"items\":[]}}");
        JsonNode latestBlogs = mapper.readTree("{\"result\":{\"items\":[{\"id\":10,\"title\":\"Rau củ ít calo\"}]}}");
        when(responseSpec.body(JsonNode.class)).thenReturn(emptyBlog, emptyRecipe, latestBlogs, emptyRecipe);
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult("[{\"type\":\"BLOG\",\"id\":10,\"reason\":\"ít calo\"}]", 100, 50));

        List<RecommendationDtos.Recommendation> result = service.recommend(1L, "giảm cân", 5);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        verify(uriSpec).uri("http://blog-service/blogs?size=20");
    }

    @Test
    void recommendReadsJsonArrayWrappedInProse() throws Exception {
        JsonNode blogData = mapper.readTree("{\"result\":{\"items\":[{\"id\":10,\"title\":\"Vegan Salad Guide\"}]}}");
        JsonNode recipeData = mapper.readTree("{\"result\":{\"items\":[]}}");
        when(responseSpec.body(JsonNode.class)).thenReturn(blogData, recipeData);
        String groqOutput = "Dưới đây là gợi ý cho bạn:\n```json\n[{\"type\":\"BLOG\",\"id\":10,\"reason\":\"tips\"}]\n```";
        when(groq.complete(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(new GroqClient.GroqResult(groqOutput, 100, 50));

        List<RecommendationDtos.Recommendation> result = service.recommend(1L, "salad", 5);

        assertEquals(1, result.size());
        assertEquals("tips", result.get(0).reason());
    }

    @Test
    void recommendThrowsDownstreamUnavailableWhenRestClientFails() {
        when(responseSpec.body(JsonNode.class)).thenThrow(new RuntimeException("Connection refused"));

        AppException ex = assertThrows(AppException.class, () -> service.recommend(1L, "salad", 5));
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
    }
}
