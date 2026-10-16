package com.veggiepal.ai.video;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
public final class VideoDtos { private VideoDtos(){} public record VideoJobResponse(Long id,String status,String filename,String transcript,JsonNode extraction,String error,LocalDateTime createdAt,LocalDateTime updatedAt){} }
