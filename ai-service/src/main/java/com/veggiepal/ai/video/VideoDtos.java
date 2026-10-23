package com.veggiepal.ai.video;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
// extraction is a Map, not a JsonNode: Spring MVC writes with Jackson 3, which serializes
// this service's Jackson 2 JsonNode as a bean ({"array":false,"nodeType":...}) instead of its JSON.
public final class VideoDtos {
    private VideoDtos(){}
    public record ExistingVideoRequest(@NotNull Long videoId) { }
    public record VideoJobResponse(Long id,Long sourceVideoId,String status,String filename,String transcript,Object extraction,String error,Instant createdAt,Instant updatedAt) { }
    public record SharedVideoRecipeResponse(Long sourceVideoId,Object extraction,Instant completedAt) { }
}
