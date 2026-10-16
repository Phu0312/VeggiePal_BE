package com.veggiepal.ai.recommendation;

import com.fasterxml.jackson.databind.JsonNode;

public final class RecommendationDtos {
    private RecommendationDtos() {}
    public record Recommendation(String type, Long id, String reason, JsonNode content) {}
}
