package com.veggiepal.ai.quota;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="ai.token")
public record TokenProperties(long dailyLimit) {}
