package com.veggiepal.blog.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.blog.configuration.VideoProperties;
import com.veggiepal.blog.dto.request.VideoRequest;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.dto.response.VideoResponse;
import com.veggiepal.blog.entity.Video;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.enums.VideoSource;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.VideoMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.VideoRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VideoService {

    static final String MP4_CONTENT_TYPE = "video/mp4";

    VideoRepository videoRepository;
    CategoryService categoryService;
    ContentModerationService contentModerationService;
    VideoMapper videoMapper;
    FileStorageService fileStorageService;
    VideoProperties videoProperties;

    @Transactional
    public VideoResponse createVideo(Long authorId, VideoRequest request, MultipartFile file) {

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasLink = request.getYoutubeUrl() != null && !request.getYoutubeUrl().isBlank();

        // Task sheet US4: a video comes from exactly one place
        if (hasFile == hasLink) {
            throw new AppException(ErrorCode.VIDEO_SOURCE_REQUIRED);
        }

        // Check the source before touching the database or the bucket
        byte[] content = hasFile ? readMp4(file) : null;

        String youtubeId = hasLink
                ? YouTubeUrls.extractId(request.getYoutubeUrl())
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_YOUTUBE_URL))
                : null;

        Video video = Video.builder()
                .authorId(authorId)
                .category(categoryService.requireActiveCategory(request.getCategoryId()))
                .title(request.getTitle().trim())
                .description(normalizeDescription(request.getDescription()))
                .sourceType(hasFile ? VideoSource.UPLOAD : VideoSource.YOUTUBE)
                // An upload gets its real URL below, once the row has an id for the storage key.
                // The placeholder never commits: any failure before that rolls the insert back.
                .videoUrl(hasFile ? "" : youtubeEmbedUrl(youtubeId))
                .thumbnailUrl(hasFile ? null : youtubeThumbnailUrl(youtubeId))
                .status(ContentStatus.DRAFT)
                .viewCount(0)
                .build();

        // Persist first so the moderation case can point to a real resource id
        videoRepository.saveAndFlush(video);

        // Moderation before the upload: if ai-service is down, nothing reaches the bucket
        String reason = Boolean.TRUE.equals(request.getPublish())
                ? applyModeration(video)
                : null;

        if (!hasFile) {
            videoRepository.save(video);
            return withReason(video, reason);
        }

        // ponytail: the DB connection stays open during the upload (≤ 30MB, a few seconds).
        // Move the upload out of the transaction if that ever starves the connection pool.
        String key = "videos/" + video.getId() + "/" + UUID.randomUUID() + ".mp4";
        String videoUrl = fileStorageService.upload(key, content, MP4_CONTENT_TYPE);

        video.setVideoUrl(videoUrl);
        video.setThumbnailUrl(posterUrl(videoUrl));

        try {
            // Flush here so a database failure surfaces inside this try, not at commit
            videoRepository.saveAndFlush(video);
        } catch (RuntimeException exception) {
            deleteQuietly(videoUrl);
            throw exception;
        }

        return withReason(video, reason);
    }

    // Task sheet US4: "Tương tự Blog, phân trang 10 items. Chỉ lấy video có status Public."
    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getPublishedVideos(Long categoryId, int page, int size) {

        PageRequest pageRequest = BlogService.pageRequest(page, size, BlogService.RECENTLY_PUBLISHED_FIRST);

        Page<Video> videos = categoryId == null
                ? videoRepository.findByStatus(ContentStatus.PUBLISHED, pageRequest)
                : videoRepository.findByStatusAndCategoryId(ContentStatus.PUBLISHED, categoryId, pageRequest);

        return toPageResponse(videos);
    }

    @Transactional
    public VideoResponse getPublishedVideo(Long id) {

        Video video = videoRepository
                .findByIdAndStatus(id, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_EXISTED));

        videoRepository.incrementViewCount(id);

        VideoResponse response = videoMapper.toVideoResponse(video);

        // The JPQL update bypasses the persistence context, so the entity we hold
        // still has the old number. Reflect the increment we just made.
        response.setViewCount(video.getViewCount() + 1);

        return response;
    }

    // Task sheet US6: the owner sees every status, Draft, Public and Banned included
    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getOwnVideos(Long authorId, ContentStatus status, int page, int size) {

        PageRequest pageRequest = BlogService.pageRequest(page, size, BlogService.NEWEST_FIRST);

        Page<Video> videos = status == null
                ? videoRepository.findByAuthorId(authorId, pageRequest)
                : videoRepository.findByAuthorIdAndStatus(authorId, status, pageRequest);

        return toPageResponse(videos);
    }

    /** Edits the text and category only. The source is fixed: delete and post again to change it. */
    @Transactional
    public VideoResponse updateVideo(Long userId, boolean admin, Long videoId, VideoRequest request) {

        Video video = findOwnedVideo(userId, admin, videoId);

        // Editing re-runs moderation, which may approve; that must not lift an admin's ban
        if (video.getStatus() == ContentStatus.BANNED) {
            throw new AppException(ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
        }

        video.setCategory(categoryService.requireActiveCategory(request.getCategoryId()));
        video.setTitle(request.getTitle().trim());
        video.setDescription(normalizeDescription(request.getDescription()));

        // A draft is not public yet, so editing it needs no moderation. Anything
        // else has been (or is being) considered already and must be reconsidered.
        String reason = video.getStatus() == ContentStatus.DRAFT
                ? null
                : applyModeration(video);

        videoRepository.save(video);
        return withReason(video, reason);
    }

    @Transactional
    public VideoResponse submitVideo(Long userId, Long videoId) {

        Video video = findOwnedVideo(userId, false, videoId);

        if (video.getStatus() != ContentStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
        }

        // A draft must not go public under a category deactivated since it was saved
        categoryService.requireActiveCategory(video.getCategory().getId());

        String reason = applyModeration(video);

        videoRepository.save(video);
        return withReason(video, reason);
    }

    @Transactional
    public void deleteVideo(Long userId, boolean admin, Long videoId) {

        Video video = findOwnedVideo(userId, admin, videoId);

        // Only an admin gets past findOwnedVideo on someone else's video, and that is a
        // takedown: hide it but keep it, so the owner still sees it as banned (task sheet US6).
        // An owner removing their own video, admin or not, is a real delete.
        if (!video.getAuthorId().equals(userId)) {
            video.setStatus(ContentStatus.BANNED);
            videoRepository.save(video);
            return;
        }

        videoRepository.delete(video);

        // Flush so a failing row delete surfaces before the file is destroyed, not at commit
        videoRepository.flush();

        // FileStorageService ignores URLs it did not upload, so a YouTube embed URL is a no-op
        deleteQuietly(video.getVideoUrl());
    }

    /** Called through ModerationAdminController when an admin settles a REVIEW_REQUIRED case. */
    @Transactional
    public void applyAdminModeration(Long videoId, Long expectedAuthorId, boolean approved) {

        Video video = videoRepository
                .findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_EXISTED));

        if (expectedAuthorId == null || !video.getAuthorId().equals(expectedAuthorId)) {
            throw new AppException(ErrorCode.MODERATION_TARGET_MISMATCH);
        }

        // A ban is terminal: a moderation decision queued before the ban must not lift it
        if (video.getStatus() == ContentStatus.BANNED) {
            throw new AppException(ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
        }

        video.setStatus(approved ? ContentStatus.PUBLISHED : ContentStatus.REJECTED);

        if (approved && video.getPublishedAt() == null) {
            video.setPublishedAt(LocalDateTime.now());
        }
    }

    private byte[] readMp4(MultipartFile file) {

        if (file.getSize() > videoProperties.maxFileSize().toBytes()) {
            throw new AppException(ErrorCode.VIDEO_TOO_LARGE);
        }

        if (!MP4_CONTENT_TYPE.equals(file.getContentType())) {
            throw new AppException(ErrorCode.INVALID_VIDEO_TYPE);
        }

        byte[] content;

        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // The declared content type must match the real file signature
        if (!Mp4Detector.isMp4(content)) {
            throw new AppException(ErrorCode.INVALID_VIDEO_TYPE);
        }

        return content;
    }

    /**
     * BR-02. Returns the moderation reason, or null when the video was approved.
     * Mutates the video's status in place.
     */
    private String applyModeration(Video video) {

        String text = video.getDescription() == null
                ? video.getTitle()
                : video.getTitle() + "\n" + video.getDescription();

        ModerationResult result = contentModerationService.moderate(
                "VIDEO",
                video.getId(),
                video.getAuthorId(),
                text
        );

        switch (result.decision()) {

            case APPROVED -> {
                video.setStatus(ContentStatus.PUBLISHED);

                // Stamped once: re-publishing an edited video must not reshuffle the feed
                if (video.getPublishedAt() == null) {
                    video.setPublishedAt(LocalDateTime.now());
                }

                return null;
            }

            case REJECTED -> video.setStatus(ContentStatus.REJECTED);

            case REVIEW_REQUIRED -> video.setStatus(ContentStatus.PENDING);
        }

        return result.reason();
    }

    private VideoResponse withReason(Video video, String reason) {

        VideoResponse response = videoMapper.toVideoResponse(video);
        response.setModerationReason(reason);
        return response;
    }

    private Video findOwnedVideo(Long userId, boolean admin, Long videoId) {

        Video video = videoRepository
                .findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_EXISTED));

        // BR-07: the owner, or an admin acting on someone else's video
        if (!admin && !video.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return video;
    }

    private PageResponse<VideoResponse> toPageResponse(Page<Video> videos) {

        return PageResponse.<VideoResponse>builder()
                .items(videos.getContent().stream().map(videoMapper::toVideoResponse).toList())
                .page(videos.getNumber())
                .size(videos.getSize())
                .totalElements(videos.getTotalElements())
                .totalPages(videos.getTotalPages())
                .build();
    }

    private void deleteQuietly(String url) {

        try {
            fileStorageService.delete(url);
        } catch (RuntimeException exception) {
            log.warn("Could not delete {} from storage", url, exception);
        }
    }

    private static String normalizeDescription(String description) {

        return description == null || description.isBlank() ? null : description.trim();
    }

    // enablejsapi lets the FE pause the iframe when it scrolls out of view (task sheet US4)
    static String youtubeEmbedUrl(String id) {

        return "https://www.youtube.com/embed/" + id + "?enablejsapi=1";
    }

    static String youtubeThumbnailUrl(String id) {

        return "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
    }

    // The poster URL shares the video's public_id, so it must never be passed to
    // FileStorageService.delete (that would destroy the video itself).
    /** Cloudinary renders a frame of the video as an image when the extension is .jpg. */
    static String posterUrl(String videoUrl) {

        int dot = videoUrl.lastIndexOf('.');
        String base = dot > videoUrl.lastIndexOf('/') ? videoUrl.substring(0, dot) : videoUrl;
        return base + ".jpg";
    }
}
