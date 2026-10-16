package com.veggiepal.ai.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq.model")
public record ModelProperties(String chat, String moderation, String recommendation, String mealPlan,
                              String videoExtraction, String transcription) {}
