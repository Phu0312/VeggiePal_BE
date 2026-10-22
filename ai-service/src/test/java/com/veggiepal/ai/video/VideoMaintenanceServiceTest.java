package com.veggiepal.ai.video;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoMaintenanceServiceTest {
    @Test
    void restartAndScheduledMaintenanceFailUnrecoverableJobsAndRunRetention() {
        VideoJobRepository repository = mock(VideoJobRepository.class);
        VideoJobStateService states = mock(VideoJobStateService.class);
        VideoStorage storage = mock(VideoStorage.class);
        VideoProperties properties = new VideoProperties("ffmpeg", "ffprobe", 100, 1800,
                Duration.ofMinutes(2), Duration.ofSeconds(20), 2, 2, "build/video",
                Duration.ofMinutes(20), Duration.ofDays(30), Duration.ofMinutes(5));
        VideoJob restart = new VideoJob(1L, "restart.mp4", "build/restart.upload", true);
        VideoJob stale = new VideoJob(1L, "stale.mp4", "build/stale.upload", true);
        when(repository.findByStatus(VideoJob.PROCESSING)).thenReturn(List.of(restart));
        when(repository.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(eq(VideoJob.PROCESSING), any()))
                .thenReturn(List.of(stale));
        VideoMaintenanceService maintenance = new VideoMaintenanceService(repository, states, storage, properties);

        maintenance.run(null);
        maintenance.maintain();

        verify(states, org.mockito.Mockito.times(2))
                .failAndRefund(null, "Video processing interrupted or timed out");
        verify(storage).delete(Path.of("build/restart.upload"));
        verify(storage).delete(Path.of("build/stale.upload"));
        verify(states).purgeBefore(any());
        verify(storage).cleanupOrphans(Duration.ZERO);
        verify(storage, org.mockito.Mockito.atLeastOnce()).cleanupOrphans(Duration.ofMinutes(20));
    }
}
