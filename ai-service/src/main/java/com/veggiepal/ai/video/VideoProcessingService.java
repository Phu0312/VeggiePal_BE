package com.veggiepal.ai.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.common.PageResponse;
import com.veggiepal.ai.quota.PremiumQuotaService;
import com.veggiepal.ai.video.VideoDtos.VideoJobResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoProcessingService {
    private final VideoJobRepository repository;
    private final PremiumQuotaService premiumQuota;
    private final VideoStorage storage;
    private final VideoMediaTools media;
    private final VideoJobStateService states;
    private final VideoProcessingWorker worker;
    private final Executor executor;
    private final ObjectMapper mapper;
    private final VideoProperties properties;

    public VideoProcessingService(VideoJobRepository repository, PremiumQuotaService premiumQuota,
                                  VideoStorage storage, VideoMediaTools media, VideoJobStateService states,
                                  VideoProcessingWorker worker,
                                  @Qualifier("videoTaskExecutor") Executor executor,
                                  ObjectMapper mapper, VideoProperties properties) {
        this.repository = repository;
        this.premiumQuota = premiumQuota;
        this.storage = storage;
        this.media = media;
        this.states = states;
        this.worker = worker;
        this.executor = executor;
        this.mapper = mapper;
        this.properties = properties;
    }

    public VideoJobResponse process(Long userId, MultipartFile file) {
        validateUpload(file);
        Path video = null;
        boolean quotaReserved = false;
        VideoJob job = null;
        try {
            video = storage.store(file);
            media.probe(video); // Real media validation always happens before quota reservation.
            premiumQuota.reserveVideo(userId);
            quotaReserved = true;
            job = repository.save(new VideoJob(userId, safeName(file.getOriginalFilename()),
                    video.toString(), true, LocalDate.now().withDayOfMonth(1)));
            VideoJobResponse accepted = view(job);
            Long jobId = job.getId();
            Path storedVideo = video;
            try {
                executor.execute(() -> worker.process(jobId));
            } catch (RuntimeException rejected) {
                states.failAndRefund(jobId, "Video processing queue is full");
                storage.delete(storedVideo);
                throw new AppException(ErrorCode.VIDEO_QUEUE_FULL, rejected);
            }
            return accepted;
        } catch (AppException exception) {
            if (job == null && quotaReserved) premiumQuota.refundVideo(userId);
            if (job == null) storage.delete(video);
            throw exception;
        } catch (RuntimeException exception) {
            if (job == null && quotaReserved) premiumQuota.refundVideo(userId);
            if (job == null) storage.delete(video);
            throw exception;
        }
    }

    public VideoJobResponse get(Long userId, Long id) {
        return view(repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    public PageResponse<VideoJobResponse> history(Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Page<VideoJob> result = repository.findAllByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(safePage, safeSize));
        return PageResponse.<VideoJobResponse>builder()
                .content(result.getContent().stream().map(this::view).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .first(result.isFirst()).last(result.isLast()).build();
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null
                || !file.getContentType().toLowerCase().startsWith("video/")) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (file.getSize() > properties.maxFileBytes()) throw new AppException(ErrorCode.FILE_TOO_LARGE);
    }

    private VideoJobResponse view(VideoJob job) {
        try {
            return new VideoJobResponse(job.getId(), job.getStatus(), job.getOriginalFilename(),
                    job.getTranscript(), job.getExtractionJson() == null ? null
                    : mapper.readValue(job.getExtractionJson(), Object.class), job.getErrorMessage(),
                    job.getCreatedAt(), job.getUpdatedAt());
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE, exception);
        }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "video";
        String filename = Path.of(name).getFileName().toString();
        return filename.substring(0, Math.min(255, filename.length()));
    }
}
