package com.veggiepal.blog.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/** Task sheet US4: the video size limit lives in application.properties, not in code. */
@ConfigurationProperties(prefix = "video")
public record VideoProperties(DataSize maxFileSize) {
}
