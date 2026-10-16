package com.veggiepal.ai.quota;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.chat")
public record QuotaProperties(int guestDailyLimit, int memberDailyLimit, int maxHistoryMessages, int maxInputCharacters) {}
