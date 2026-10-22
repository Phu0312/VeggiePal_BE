package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.quota.PremiumQuotaService;
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
}
