package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import com.veggiepal.blog.configuration.VideoProperties;
import com.veggiepal.blog.dto.request.VideoRequest;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.dto.response.VideoResponse;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.entity.Video;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.enums.VideoSource;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.VideoMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationDecision;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.VideoRepository;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    static final Long AUTHOR_ID = 7L;
    static final Long STRANGER_ID = 8L;
    static final Long ADMIN_ID = 1L;

    static final String YOUTUBE_LINK = "https://youtu.be/dQw4w9WgXcQ";

    static final String CLOUDINARY_URL = "https://res.cloudinary.com/demo/video/upload/v1/videos/10/abc.mp4";

    @Mock
    VideoRepository videoRepository;

    @Mock
    CategoryService categoryService;

    @Mock
    ContentModerationService contentModerationService;

    @Mock
    FileStorageService fileStorageService;

    VideoMapper videoMapper = Mappers.getMapper(VideoMapper.class);

    VideoService videoService;

    @BeforeEach
    void setUp() {
        // 1KB keeps the oversize case cheap; the real limit comes from application.properties
        videoService = new VideoService(
                videoRepository, categoryService, contentModerationService,
                videoMapper, fileStorageService, new VideoProperties(DataSize.ofKilobytes(1)));
    }

    static Category category() {
        return Category.builder()
                .id(3L).name("Món chính").type(CategoryType.RECIPE_TYPE)
                .displayOrder((short) 0).active(true)
                .build();
    }

    static byte[] mp4Bytes() {
        byte[] content = new byte[32];
        content[3] = 0x20;
        System.arraycopy("ftypisom".getBytes(StandardCharsets.US_ASCII), 0, content, 4, 8);
        return content;
    }

    static MockMultipartFile mp4File() {
        return new MockMultipartFile("file", "canh-chua.mp4", "video/mp4", mp4Bytes());
    }

    static VideoRequest request(String youtubeUrl, boolean publish) {
        return VideoRequest.builder()
                .title("  Canh chua chay  ").description("Nấu trong 15 phút")
                .categoryId(3L).youtubeUrl(youtubeUrl).publish(publish)
                .build();
    }

    static Video video(ContentStatus status) {
        return Video.builder()
                .id(10L).authorId(AUTHOR_ID).category(category())
                .title("Canh chua chay").description("Nấu trong 15 phút")
                .sourceType(VideoSource.UPLOAD).videoUrl(CLOUDINARY_URL)
                .thumbnailUrl("https://res.cloudinary.com/demo/video/upload/v1/videos/10/abc.jpg")
                .status(status).viewCount(4)
                .build();
    }

    static void assertFailsWith(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    // In the real database saveAndFlush is what assigns the id
    void givenSaveAssignsId() {
        when(videoRepository.saveAndFlush(any(Video.class))).thenAnswer(call -> {
            Video video = call.getArgument(0);
            video.setId(10L);
            return video;
        });
    }

    // ---------------------------------------------------------------- createVideo

    @Test
    void createVideo_youtubeLinkAsDraft_buildsItsOwnUrlsAndSkipsModerationAndStorage() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        givenSaveAssignsId();

        VideoResponse response = videoService.createVideo(AUTHOR_ID, request(YOUTUBE_LINK, false), null);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.getSourceType()).isEqualTo(VideoSource.YOUTUBE);
        assertThat(response.getTitle()).isEqualTo("Canh chua chay");
        assertThat(response.getCategoryName()).isEqualTo("Món chính");
        assertThat(response.getVideoUrl()).isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ?enablejsapi=1");
        assertThat(response.getThumbnailUrl()).isEqualTo("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
        verify(contentModerationService, never()).moderate(anyString(), any(), any(), anyString());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createVideo_mp4FileApproved_uploadsUnderTheVideoIdAndPublishes() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        givenSaveAssignsId();
        when(contentModerationService.moderate(eq("VIDEO"), eq(10L), eq(AUTHOR_ID), anyString()))
                .thenReturn(ModerationResult.approved());
        when(fileStorageService.upload(startsWith("videos/10/"), any(byte[].class), eq("video/mp4")))
                .thenReturn(CLOUDINARY_URL);

        VideoResponse response = videoService.createVideo(AUTHOR_ID, request(null, true), mp4File());

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
        assertThat(response.getSourceType()).isEqualTo(VideoSource.UPLOAD);
        assertThat(response.getVideoUrl()).isEqualTo(CLOUDINARY_URL);
        assertThat(response.getThumbnailUrl())
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/v1/videos/10/abc.jpg");
    }

    @Test
    void createVideo_rejected_sendsTitleAndDescriptionAndReturnsTheReason() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        givenSaveAssignsId();
        when(contentModerationService.moderate(anyString(), any(), any(), anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REJECTED, "spam"));

        VideoResponse response = videoService.createVideo(AUTHOR_ID, request(YOUTUBE_LINK, true), null);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.REJECTED);
        assertThat(response.getModerationReason()).isEqualTo("spam");
        assertThat(response.getPublishedAt()).isNull();
        verify(contentModerationService).moderate("VIDEO", 10L, AUTHOR_ID, "Canh chua chay\nNấu trong 15 phút");
    }

    @Test
    void createVideo_reviewRequired_holdsTheVideoBackAsPending() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        givenSaveAssignsId();
        when(contentModerationService.moderate(anyString(), any(), any(), anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REVIEW_REQUIRED, "cần xem lại"));

        VideoResponse response = videoService.createVideo(AUTHOR_ID, request(YOUTUBE_LINK, true), null);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        assertThat(response.getPublishedAt()).isNull();
    }

    @Test
    void createVideo_bothFileAndLink_throwsSourceRequired() {
        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request(YOUTUBE_LINK, false), mp4File()),
                ErrorCode.VIDEO_SOURCE_REQUIRED);
        verifyNoInteractions(videoRepository, categoryService, fileStorageService);
    }

    @Test
    void createVideo_neitherFileNorLink_throwsSourceRequired() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.mp4", "video/mp4", new byte[0]);

        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request(null, false), null),
                ErrorCode.VIDEO_SOURCE_REQUIRED);
        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request("   ", false), empty),
                ErrorCode.VIDEO_SOURCE_REQUIRED);
        verifyNoInteractions(videoRepository, categoryService, fileStorageService);
    }

    @Test
    void createVideo_fileOverTheLimit_throwsTooLarge() {
        MockMultipartFile big = new MockMultipartFile("file", "big.mp4", "video/mp4", new byte[1025]);

        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request(null, false), big),
                ErrorCode.VIDEO_TOO_LARGE);
        verifyNoInteractions(videoRepository, categoryService, fileStorageService);
    }

    @Test
    void createVideo_notDeclaredAsMp4_throwsInvalidType() {
        MockMultipartFile mov = new MockMultipartFile("file", "a.mov", "video/quicktime", mp4Bytes());

        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request(null, false), mov),
                ErrorCode.INVALID_VIDEO_TYPE);
        verifyNoInteractions(videoRepository, categoryService, fileStorageService);
    }

    @Test
    void createVideo_declaredMp4ButNotAnMp4_throwsInvalidType() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile fake = new MockMultipartFile("file", "fake.mp4", "video/mp4", png);

        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request(null, false), fake),
                ErrorCode.INVALID_VIDEO_TYPE);
        verifyNoInteractions(videoRepository, categoryService, fileStorageService);
    }

    @Test
    void createVideo_linkThatIsNotYouTube_throwsInvalidYouTubeUrl() {
        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request("https://vimeo.com/123", false), null),
                ErrorCode.INVALID_YOUTUBE_URL);
        verifyNoInteractions(videoRepository, categoryService, fileStorageService);
    }

    @Test
    void createVideo_moderationUnavailable_neverUploads() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        givenSaveAssignsId();
        when(contentModerationService.moderate(anyString(), any(), any(), anyString()))
                .thenThrow(new AppException(ErrorCode.MODERATION_UNAVAILABLE));

        assertFailsWith(() -> videoService.createVideo(AUTHOR_ID, request(null, true), mp4File()),
                ErrorCode.MODERATION_UNAVAILABLE);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createVideo_saveFailsAfterUpload_deletesTheUploadedFile() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(videoRepository.saveAndFlush(any(Video.class)))
                .thenAnswer(call -> {
                    Video video = call.getArgument(0);
                    video.setId(10L);
                    return video;
                })
                .thenThrow(new DataIntegrityViolationException("boom"));
        when(fileStorageService.upload(anyString(), any(byte[].class), anyString())).thenReturn(CLOUDINARY_URL);

        assertThatThrownBy(() -> videoService.createVideo(AUTHOR_ID, request(null, false), mp4File()))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(fileStorageService).delete(CLOUDINARY_URL);
    }

    // ---------------------------------------------------------------- reads

    @Test
    void getPublishedVideos_withoutCategory_listsPublishedNewestPublishedFirst() {
        when(videoRepository.findByStatus(eq(ContentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video(ContentStatus.PUBLISHED))));

        PageResponse<VideoResponse> page = videoService.getPublishedVideos(null, 0, 10);

        assertThat(page.getItems()).extracting(VideoResponse::getId).containsExactly(10L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findByStatus(eq(ContentStatus.PUBLISHED), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(BlogService.RECENTLY_PUBLISHED_FIRST);
    }

    @Test
    void getPublishedVideos_withCategory_filtersByIt() {
        when(videoRepository.findByStatusAndCategoryId(eq(ContentStatus.PUBLISHED), eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video(ContentStatus.PUBLISHED))));

        PageResponse<VideoResponse> page = videoService.getPublishedVideos(3L, 0, 10);

        assertThat(page.getItems()).extracting(VideoResponse::getCategoryName).containsExactly("Món chính");
    }

    @Test
    void getPublishedVideo_published_countsAView() {
        when(videoRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(video(ContentStatus.PUBLISHED)));

        VideoResponse response = videoService.getPublishedVideo(10L);

        assertThat(response.getViewCount()).isEqualTo(5);
        verify(videoRepository).incrementViewCount(10L);
    }

    @Test
    void getPublishedVideo_notPublished_throwsNotExistedWithoutCounting() {
        when(videoRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertFailsWith(() -> videoService.getPublishedVideo(10L), ErrorCode.VIDEO_NOT_EXISTED);
        verify(videoRepository, never()).incrementViewCount(any());
    }

    @Test
    void getOwnVideos_withStatus_filtersTheCallersVideos() {
        when(videoRepository.findByAuthorIdAndStatus(eq(AUTHOR_ID), eq(ContentStatus.DRAFT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video(ContentStatus.DRAFT))));

        PageResponse<VideoResponse> page = videoService.getOwnVideos(AUTHOR_ID, ContentStatus.DRAFT, 0, 10);

        assertThat(page.getItems()).extracting(VideoResponse::getStatus).containsExactly(ContentStatus.DRAFT);
    }

    @Test
    void getOwnVideos_withoutStatus_returnsEveryStatus() {
        when(videoRepository.findByAuthorId(eq(AUTHOR_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video(ContentStatus.BANNED))));

        PageResponse<VideoResponse> page = videoService.getOwnVideos(AUTHOR_ID, null, 0, 10);

        assertThat(page.getItems()).extracting(VideoResponse::getStatus).containsExactly(ContentStatus.BANNED);
    }

    // ---------------------------------------------------------------- updateVideo

    @Test
    void updateVideo_draft_editsWithoutModerationAndIgnoresPublish() {
        Video draft = video(ContentStatus.DRAFT);
        when(videoRepository.findById(10L)).thenReturn(Optional.of(draft));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        VideoRequest edit = VideoRequest.builder()
                .title("  Canh chua mới  ").description("  Thêm me chua  ")
                .categoryId(3L).publish(true)
                .build();

        VideoResponse response = videoService.updateVideo(AUTHOR_ID, false, 10L, edit);

        assertThat(response.getTitle()).isEqualTo("Canh chua mới");
        assertThat(response.getDescription()).isEqualTo("Thêm me chua");
        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        verify(contentModerationService, never()).moderate(anyString(), any(), any(), anyString());
        verify(videoRepository).save(draft);
    }

    @Test
    void updateVideo_published_goesBackThroughModeration() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.PUBLISHED)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString(), any(), any(), anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REVIEW_REQUIRED, "cần xem lại"));

        VideoResponse response = videoService.updateVideo(AUTHOR_ID, false, 10L, request(null, false));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        assertThat(response.getModerationReason()).isEqualTo("cần xem lại");
    }

    @Test
    void updateVideo_banned_throwsInvalidTransition() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.BANNED)));

        assertFailsWith(() -> videoService.updateVideo(AUTHOR_ID, false, 10L, request(null, false)),
                ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
        verify(videoRepository, never()).save(any());
    }

    @Test
    void updateVideo_byStranger_throwsUnauthorized() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.PUBLISHED)));

        assertFailsWith(() -> videoService.updateVideo(STRANGER_ID, false, 10L, request(null, false)),
                ErrorCode.UNAUTHORIZED);
    }

    @Test
    void updateVideo_missing_throwsNotExisted() {
        when(videoRepository.findById(10L)).thenReturn(Optional.empty());

        assertFailsWith(() -> videoService.updateVideo(AUTHOR_ID, false, 10L, request(null, false)),
                ErrorCode.VIDEO_NOT_EXISTED);
    }

    // ---------------------------------------------------------------- submitVideo

    @Test
    void submitVideo_draftApproved_publishes() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.DRAFT)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString(), any(), any(), anyString()))
                .thenReturn(ModerationResult.approved());

        VideoResponse response = videoService.submitVideo(AUTHOR_ID, 10L);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void submitVideo_notDraft_throwsInvalidTransition() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.PUBLISHED)));

        assertFailsWith(() -> videoService.submitVideo(AUTHOR_ID, 10L), ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
    }

    @Test
    void submitVideo_bySomeoneElse_throwsUnauthorized() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.DRAFT)));

        assertFailsWith(() -> videoService.submitVideo(STRANGER_ID, 10L), ErrorCode.UNAUTHORIZED);
    }

    // ---------------------------------------------------------------- deleteVideo

    @Test
    void deleteVideo_byOwner_deletesTheRowAndTheFile() {
        Video video = video(ContentStatus.PUBLISHED);
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video));

        videoService.deleteVideo(AUTHOR_ID, false, 10L);

        InOrder inOrder = inOrder(videoRepository, fileStorageService);
        inOrder.verify(videoRepository).delete(video);
        inOrder.verify(videoRepository).flush();
        inOrder.verify(fileStorageService).delete(CLOUDINARY_URL);
    }

    @Test
    void deleteVideo_byAdminOnSomeoneElsesVideo_bansItAndKeepsTheFile() {
        Video video = video(ContentStatus.PUBLISHED);
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video));

        videoService.deleteVideo(ADMIN_ID, true, 10L);

        assertThat(video.getStatus()).isEqualTo(ContentStatus.BANNED);
        verify(videoRepository).save(video);
        verify(videoRepository, never()).delete(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void deleteVideo_storageFails_stillDeletesTheRow() {
        Video video = video(ContentStatus.PUBLISHED);
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video));
        doThrow(new AppException(ErrorCode.FILE_UPLOAD_FAILED)).when(fileStorageService).delete(CLOUDINARY_URL);

        videoService.deleteVideo(AUTHOR_ID, false, 10L);

        verify(videoRepository).delete(video);
    }

    @Test
    void deleteVideo_byStranger_throwsUnauthorized() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.PUBLISHED)));

        assertFailsWith(() -> videoService.deleteVideo(STRANGER_ID, false, 10L), ErrorCode.UNAUTHORIZED);
        verify(videoRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- applyAdminModeration

    @Test
    void applyAdminModeration_approved_publishesAndStampsPublishedAt() {
        Video video = video(ContentStatus.PENDING);
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video));

        videoService.applyAdminModeration(10L, AUTHOR_ID, true);

        assertThat(video.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(video.getPublishedAt()).isNotNull();
    }

    @Test
    void applyAdminModeration_wrongAuthor_throwsTargetMismatch() {
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video(ContentStatus.PENDING)));

        assertFailsWith(() -> videoService.applyAdminModeration(10L, STRANGER_ID, true),
                ErrorCode.MODERATION_TARGET_MISMATCH);
    }

    @Test
    void applyAdminModeration_bannedVideo_staysBanned() {
        Video video = video(ContentStatus.BANNED);
        when(videoRepository.findById(10L)).thenReturn(Optional.of(video));

        assertFailsWith(() -> videoService.applyAdminModeration(10L, AUTHOR_ID, true),
                ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
        assertThat(video.getStatus()).isEqualTo(ContentStatus.BANNED);
    }
}
