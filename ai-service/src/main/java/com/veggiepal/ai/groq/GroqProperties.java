package com.veggiepal.ai.groq;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq.api")
public record GroqProperties(String baseUrl, String key, Duration connectTimeout, Duration readTimeout, int maxRetries) {}
