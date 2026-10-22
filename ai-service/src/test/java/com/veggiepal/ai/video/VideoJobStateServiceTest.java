package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.quota.PremiumQuotaService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VideoJobStateServiceTest {
    @Test
    void failureRefundIsIdempotentEvenWhenRecoveryAndWorkerRace() {
        VideoJobRepository repository = mock(VideoJobRepository.class);
        PremiumQuotaService quota = mock(PremiumQuotaService.class);
        VideoJob job = new VideoJob(5L, "meal.mp4", "tmp/video", true);
        when(repository.findForUpdate(3L)).thenReturn(Optional.of(job));
        VideoJobStateService states = new VideoJobStateService(repository, quota);

        assertTrue(states.failAndRefund(3L, "timeout"));
        states.failAndRefund(3L, "worker failed later");

        assertEquals(VideoJob.FAILED, job.getStatus());
        assertTrue(job.isQuotaRefunded());
        verify(quota).refundVideo(5L, null);
        verifyNoMoreInteractions(quota);
    }

    @Test
    void completedJobCanNeverBeFailedOrRefunded() {
        VideoJobRepository repository = mock(VideoJobRepository.class);
        PremiumQuotaService quota = mock(PremiumQuotaService.class);
        VideoJob job = new VideoJob(5L, "meal.mp4", "tmp/video", true);
        job.complete("text", "{}");
        when(repository.findForUpdate(3L)).thenReturn(Optional.of(job));

        new VideoJobStateService(repository, quota).failAndRefund(3L, "late timeout");

        assertEquals(VideoJob.COMPLETED, job.getStatus());
        verifyNoMoreInteractions(quota);
    }

    @Test
    void failedJobReleasesUniqueSourceForRetry() {
        VideoJobRepository repository = mock(VideoJobRepository.class);
        VideoJob failed = new VideoJob(5L, "meal.mp4", null, true, LocalDate.now(), 8L);
        failed.fail("bad response");
        when(repository.findBySourceVideoIdForUpdate(8L)).thenReturn(Optional.of(failed));

        new VideoJobStateService(repository, mock(PremiumQuotaService.class)).releaseReusableSource(8L);

        assertEquals(null, failed.getSourceVideoId());
    }

    @Test
    void purgeClearsOutputAndUniqueSourceForRerun() {
        VideoJobRepository repository = mock(VideoJobRepository.class);
        VideoJob completed = new VideoJob(5L, "meal.mp4", null, true, LocalDate.now(), 8L);
        completed.complete("transcript", "{}");
        when(repository.findPurgeCandidates(anyString(), any(), any())).thenReturn(java.util.List.of(completed));

        int purged = new VideoJobStateService(repository, mock(PremiumQuotaService.class))
                .purgeBefore(java.time.LocalDateTime.now());

        assertEquals(1, purged);
        assertEquals(null, completed.getExtractionJson());
        assertEquals(null, completed.getSourceVideoId());
    }
}
