package com.veggiepal.ai.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.common.PageResponse;
import com.veggiepal.ai.quota.PremiumQuotaService;
import com.veggiepal.ai.video.VideoDtos.VideoJobResponse;
import com.veggiepal.ai.video.VideoDtos.SharedVideoRecipeResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final BlogVideoClient blogVideos;
    private final CloudinaryVideoDownloader downloader;

    public VideoProcessingService(VideoJobRepository repository, PremiumQuotaService premiumQuota,
                                  VideoStorage storage, VideoMediaTools media, VideoJobStateService states,
                                  VideoProcessingWorker worker,
                                  @Qualifier("videoTaskExecutor") Executor executor,
                                  ObjectMapper mapper, VideoProperties properties,
                                  BlogVideoClient blogVideos, CloudinaryVideoDownloader downloader) {
        this.repository = repository;
        this.premiumQuota = premiumQuota;
        this.storage = storage;
        this.media = media;
        this.states = states;
        this.worker = worker;
        this.executor = executor;
        this.mapper = mapper;
        this.properties = properties;
        this.blogVideos = blogVideos;
        this.downloader = downloader;
    }

    public VideoJobResponse processExisting(Long userId, Long videoId) {
        BlogVideoClient.BlogVideoMetadata source = requirePublishedUpload(videoId);
        VideoJob cached = repository.findBySourceVideoId(videoId).orElse(null);
        if (cached != null) return sourceView(cached);

        Path video = null;
        boolean quotaReserved = false;
        VideoJob job = null;
        try {
            video = downloader.download(source.videoUrl());
            try {
                media.probe(video);
            } catch (AppException invalidSource) {
                throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, invalidSource);
            }

            // Avoid quota work if another request completed creation while this request downloaded.
            cached = repository.findBySourceVideoId(videoId).orElse(null);
            if (cached != null) {
                storage.delete(video);
                return sourceView(cached);
            }

            premiumQuota.reserveVideo(userId);
            quotaReserved = true;
            try {
                job = repository.saveAndFlush(new VideoJob(userId, "blog-video-" + videoId + ".mp4",
                        video.toString(), true, LocalDate.now().withDayOfMonth(1), videoId));
            } catch (DataIntegrityViolationException duplicate) {
                premiumQuota.refundVideo(userId);
                quotaReserved = false;
                storage.delete(video);
                return sourceView(repository.findBySourceVideoId(videoId)
                        .orElseThrow(() -> new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, duplicate)));
            }

            VideoJobResponse accepted = sourceView(job);
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
            throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, exception);
        }
    }

    public SharedVideoRecipeResponse sharedResult(Long videoId) {
        requirePublishedUpload(videoId);
        VideoJob job = repository.findBySourceVideoId(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (VideoJob.PROCESSING.equals(job.getStatus())) {
            throw new AppException(ErrorCode.VIDEO_EXTRACTION_IN_PROGRESS);
        }
        if (VideoJob.FAILED.equals(job.getStatus())) {
            throw new AppException(ErrorCode.VIDEO_EXTRACTION_FAILED);
        }
        return new SharedVideoRecipeResponse(videoId, parseExtraction(job), job.getUpdatedAt());
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
            return new VideoJobResponse(job.getId(), job.getSourceVideoId(), job.getStatus(), job.getOriginalFilename(),
                    job.getTranscript(), job.getExtractionJson() == null ? null
                    : mapper.readValue(job.getExtractionJson(), Object.class), job.getErrorMessage(),
                    job.getCreatedAt(), job.getUpdatedAt());
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE, exception);
        }
    }

    private VideoJobResponse sourceView(VideoJob job) {
        try {
            return new VideoJobResponse(job.getId(), job.getSourceVideoId(), job.getStatus(),
                    job.getOriginalFilename(), null, parseExtraction(job), job.getErrorMessage(),
                    job.getCreatedAt(), job.getUpdatedAt());
        } catch (AppException exception) {
            throw exception;
        }
    }

    private Object parseExtraction(VideoJob job) {
        if (job.getExtractionJson() == null) return null;
        try {
            return mapper.readValue(job.getExtractionJson(), Object.class);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE, exception);
        }
    }

    private BlogVideoClient.BlogVideoMetadata requirePublishedUpload(Long videoId) {
        if (videoId == null || videoId <= 0) throw new AppException(ErrorCode.INVALID_REQUEST);
        BlogVideoClient.BlogVideoMetadata source = blogVideos.get(videoId);
        if (!"PUBLISHED".equals(source.status())) throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"UPLOAD".equals(source.sourceType())) throw new AppException(ErrorCode.VIDEO_SOURCE_UNSUPPORTED);
        return source;
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "video";
        String filename = Path.of(name).getFileName().toString();
        return filename.substring(0, Math.min(255, filename.length()));
    }
}
