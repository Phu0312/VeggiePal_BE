package com.veggiepal.ai.video;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.video")
public record VideoProperties(String ffmpegPath, String ffprobePath, long maxFileBytes,
                              long maxDurationSeconds, Duration ffmpegTimeout,
                              Duration ffprobeTimeout, int workerCount, int queueCapacity,
                              String storageDir, Duration staleAfter, Duration retention,
                              Duration maintenanceInterval, int videoExtractionMaxTokens) {
    public VideoProperties {
        if (ffmpegPath == null || ffmpegPath.isBlank()) ffmpegPath = "ffmpeg";
        if (ffprobePath == null || ffprobePath.isBlank()) ffprobePath = "ffprobe";
        if (maxFileBytes <= 0) maxFileBytes = 104_857_600L;
        if (maxDurationSeconds <= 0) maxDurationSeconds = 1_800L;
        if (ffmpegTimeout == null) ffmpegTimeout = Duration.ofMinutes(2);
        if (ffprobeTimeout == null) ffprobeTimeout = Duration.ofSeconds(20);
        if (workerCount <= 0) workerCount = 2;
        if (queueCapacity < 0) queueCapacity = 6;
        if (storageDir == null || storageDir.isBlank()) storageDir = "./tmp/veggiepal-ai-video";
        if (staleAfter == null) staleAfter = Duration.ofMinutes(20);
        if (retention == null) retention = Duration.ofDays(30);
        if (maintenanceInterval == null) maintenanceInterval = Duration.ofMinutes(5);
        if (videoExtractionMaxTokens <= 0) videoExtractionMaxTokens = 4_000;
    }
    public int storageCapacity() { return Math.max(1, workerCount) + Math.max(0, queueCapacity); }
}
