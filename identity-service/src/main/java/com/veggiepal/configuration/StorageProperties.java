package com.veggiepal.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.cloudinary")
public record StorageProperties(
        String cloudName,
        String apiKey,
        String apiSecret
) {
}
