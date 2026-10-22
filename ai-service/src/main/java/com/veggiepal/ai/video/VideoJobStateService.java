package com.veggiepal.ai.video;

import com.veggiepal.ai.quota.PremiumQuotaService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoJobStateService {
    private final VideoJobRepository repository;
    private final PremiumQuotaService quota;

    public VideoJobStateService(VideoJobRepository repository, PremiumQuotaService quota) {
        this.repository = repository; this.quota = quota;
    }

    @Transactional
    public Optional<WorkItem> begin(Long id) {
        return repository.findForUpdate(id).filter(job -> VideoJob.PROCESSING.equals(job.getStatus()))
                .map(job -> { job.touch(); return new WorkItem(job.getId(), job.getUserId(), job.getTempFilePath()); });
    }

    @Transactional
    public void touch(Long id) { repository.findForUpdate(id).ifPresent(VideoJob::touch); }

    @Transactional
    public boolean complete(Long id, String transcript, String extractionJson) {
        return repository.findForUpdate(id).map(job -> job.complete(transcript, extractionJson)).orElse(false);
    }

    @Transactional
    public boolean failAndRefund(Long id, String error) {
        VideoJob job = repository.findForUpdate(id).orElse(null);
        if (job == null) return false;
        boolean changed = job.fail(error);
        if (job.isQuotaReserved() && !job.isQuotaRefunded() && !VideoJob.COMPLETED.equals(job.getStatus())) {
            quota.refundVideo(job.getUserId(), job.getQuotaPeriodDate());
            job.markQuotaRefunded();
        }
        return changed;
    }

    @Transactional
    public int purgeBefore(LocalDateTime cutoff) {
        var jobs = repository.findPurgeCandidates(VideoJob.PROCESSING, cutoff,
                org.springframework.data.domain.PageRequest.of(0, 100));
        jobs.forEach(VideoJob::purgeOutput);
        return jobs.size();
    }

    public record WorkItem(Long id, Long userId, String tempFilePath) { }
}
