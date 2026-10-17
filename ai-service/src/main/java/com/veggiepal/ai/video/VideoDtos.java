package com.veggiepal.ai.video;

import java.time.LocalDateTime;
// extraction is a Map, not a JsonNode: Spring MVC writes with Jackson 3, which serializes
// this service's Jackson 2 JsonNode as a bean ({"array":false,"nodeType":...}) instead of its JSON.
public final class VideoDtos { private VideoDtos(){} public record VideoJobResponse(Long id,String status,String filename,String transcript,Object extraction,String error,LocalDateTime createdAt,LocalDateTime updatedAt){} }
