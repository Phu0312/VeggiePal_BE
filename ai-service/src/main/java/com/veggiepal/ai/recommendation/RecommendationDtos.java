package com.veggiepal.ai.recommendation;

// content is a Map, not a JsonNode: Spring MVC writes with Jackson 3, which serializes
// this service's Jackson 2 JsonNode as a bean ({"array":false,"nodeType":...}) instead of its JSON.
public final class RecommendationDtos {
    private RecommendationDtos() {}
    public record Recommendation(String type, Long id, String reason, Object content) {}
}
