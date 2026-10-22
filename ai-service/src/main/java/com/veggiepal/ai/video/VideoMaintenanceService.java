package com.veggiepal.ai.video;

import java.nio.file.Path;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VideoMaintenanceService implements ApplicationRunner {
    private final VideoJobRepository repository;
    private final VideoJobStateService states;
    private final VideoStorage storage;
    private final VideoProperties properties;

    public VideoMaintenanceService(VideoJobRepository repository, VideoJobStateService states,
                                   VideoStorage storage, VideoProperties properties) {
        this.repository=repository;this.states=states;this.storage=storage;this.properties=properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        // No in-memory task survives a process restart, so every pre-existing PROCESSING job is unrecoverable.
        repository.findByStatus(VideoJob.PROCESSING).forEach(this::failAndCleanup);
        // The storage directory is dedicated to video jobs. After a JVM restart no worker owns any remaining file.
        storage.cleanupOrphans(java.time.Duration.ZERO);
    }

    @Scheduled(fixedDelayString = "${ai.video.maintenance-interval:5m}")
    public void maintain() {
        LocalDateTime stale = LocalDateTime.now().minus(properties.staleAfter());
        repository.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(VideoJob.PROCESSING, stale)
                .forEach(this::failAndCleanup);
        states.purgeBefore(LocalDateTime.now().minus(properties.retention()));
        storage.cleanupOrphans(properties.staleAfter());
    }

    private void failAndCleanup(VideoJob job) {
        String path = job.getTempFilePath();
        states.failAndRefund(job.getId(), "Video processing interrupted or timed out");
        if (path != null) {
            try { storage.delete(Path.of(path)); }
            catch (RuntimeException ignored) { /* Invalid legacy paths must not stop recovery. */ }
        }
    }
}
