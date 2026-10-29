package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.quota.PremiumQuotaService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;

class VideoProcessingServiceTest {
    private VideoJobRepository repository;
    private PremiumQuotaService quota;
    private VideoStorage storage;
    private VideoMediaTools media;
    private VideoJobStateService states;
    private VideoProcessingWorker worker;
    private Executor executor;
    private VideoProcessingService service;
    private BlogVideoClient blogVideos;
    private CloudinaryVideoDownloader downloader;
    private final Path stored = Path.of("build", "video-test.upload");

    @BeforeEach
    void setUp() {
        repository = mock(VideoJobRepository.class);
        quota = mock(PremiumQuotaService.class);
        storage = mock(VideoStorage.class);
        media = mock(VideoMediaTools.class);
        states = mock(VideoJobStateService.class);
        worker = mock(VideoProcessingWorker.class);
        executor = mock(Executor.class);
        blogVideos = mock(BlogVideoClient.class);
        downloader = mock(CloudinaryVideoDownloader.class);
        when(storage.store(any())).thenReturn(stored);
        when(media.probe(stored)).thenReturn(new VideoMediaTools.Probe(60, "mp4"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = service(100);
    }

    private VideoProcessingService service(long maxBytes) {
        VideoProperties properties = new VideoProperties("ffmpeg", "ffprobe", maxBytes, 1800,
                Duration.ofMinutes(2), Duration.ofSeconds(20), 2, 6, "build/video", Duration.ofMinutes(20),
                Duration.ofDays(30), Duration.ofMinutes(5), 4000);
        return new VideoProcessingService(repository, quota, storage, media, states, worker,
                executor, new ObjectMapper(), properties, blogVideos, downloader);
    }

    @Test
    void rejectsBasicInvalidUploadsBeforeStorageAndQuota() {
        var empty = new MockMultipartFile("file", "test.mp4", "video/mp4", new byte[0]);
        var wrongMime = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1});

        assertEquals(ErrorCode.INVALID_REQUEST,
                assertThrows(AppException.class, () -> service.process(1L, empty)).getErrorCode());
        assertEquals(ErrorCode.INVALID_REQUEST,
                assertThrows(AppException.class, () -> service.process(1L, wrongMime)).getErrorCode());
        verify(quota, never()).reserveVideo(any());
    }

    @Test
    void rejectsOversizedUploadBeforeQuota() {
        var file = new MockMultipartFile("file", "large.mp4", "video/mp4", new byte[101]);
        assertEquals(ErrorCode.FILE_TOO_LARGE,
                assertThrows(AppException.class, () -> service.process(1L, file)).getErrorCode());
        verify(quota, never()).reserveVideo(any());
    }

    @Test
    void probesActualFormatBeforeReservingQuota() {
        var file = video();
        when(media.probe(stored)).thenThrow(new AppException(ErrorCode.INVALID_REQUEST));

        assertEquals(ErrorCode.INVALID_REQUEST,
                assertThrows(AppException.class, () -> service.process(1L, file)).getErrorCode());
        verify(quota, never()).reserveVideo(any());
        verify(storage).delete(stored);
    }

    @Test
    void acceptsJobAndQueuesWorkerOnlyAfterProbeQuotaAndSave() {
        var response = service.process(7L, video());

        assertEquals(VideoJob.PROCESSING, response.status());
        InOrder order = inOrder(media, quota, repository, executor);
        order.verify(media).probe(stored);
        order.verify(quota).reserveVideo(7L);
        order.verify(repository).save(any(VideoJob.class));
        order.verify(executor).execute(any(Runnable.class));
    }

    @Test
    void executorRejectionFailsJobRefundsAndCleansFile() {
        doThrow(new RuntimeException("full")).when(executor).execute(any(Runnable.class));

        AppException exception = assertThrows(AppException.class, () -> service.process(7L, video()));

        assertEquals(ErrorCode.VIDEO_QUEUE_FULL, exception.getErrorCode());
        verify(states).failAndRefund(null, "Video processing queue is full");
        verify(storage).delete(stored);
    }

    @Test
    void failedJobPersistenceRefundsReservationAndCleansFile() {
        when(repository.save(any())).thenThrow(new RuntimeException("database"));

        assertThrows(RuntimeException.class, () -> service.process(7L, video()));

        verify(quota).refundVideo(7L);
        verify(storage).delete(stored);
    }

    @Test
    void detailUsesUserScopedQuery() {
        VideoJob job = new VideoJob(1L, "cooking.mp4");
        job.complete("transcribed", "{\"title\":\"Soup\",\"ingredients\":[],\"steps\":[]}");
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(job));

        var response = service.get(1L, 10L);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        verify(repository).findByIdAndUserId(10L, 1L);
    }

    @Test
    void otherUsersJobIsNotDisclosed() {
        when(repository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,
                assertThrows(AppException.class, () -> service.get(2L, 10L)).getErrorCode());
    }

    @Test
    void historyIsPaginatedAndScopedToAuthenticatedUser() {
        VideoJob job = new VideoJob(8L, "history.mp4");
        when(repository.findAllByUserIdOrderByCreatedAtDesc(8L, PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(job), PageRequest.of(1, 10), 11));

        var page = service.history(8L, 1, 10);

        assertEquals(1, page.getPage());
        assertEquals(11, page.getTotalElements());
        assertEquals(1, page.getItems().size());
        verify(repository).findAllByUserIdOrderByCreatedAtDesc(8L, PageRequest.of(1, 10));
    }

    @Test
    void publishedCloudinaryUploadIsProbedBeforeQuotaAndQueued() {
        publishedUpload();
        when(downloader.download("https://res.cloudinary.com/veggiepal/video/upload/v1/meal.mp4"))
                .thenReturn(stored);
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.processExisting(7L, 8L);

        assertEquals(8L, response.sourceVideoId());
        InOrder order = inOrder(downloader, media, quota, repository, executor);
        order.verify(downloader).download(any());
        order.verify(media).probe(stored);
        order.verify(quota).reserveVideo(7L);
        order.verify(repository).saveAndFlush(any(VideoJob.class));
        order.verify(executor).execute(any(Runnable.class));
    }

    @Test
    void youtubeAndUnpublishedSourcesAreRejectedBeforeDownloadOrQuota() {
        when(blogVideos.get(8L)).thenReturn(new BlogVideoClient.BlogVideoMetadata(
                8L, "PUBLISHED", "YOUTUBE", "https://youtube.com/embed/x"));
        assertEquals(ErrorCode.VIDEO_SOURCE_UNSUPPORTED,
                assertThrows(AppException.class, () -> service.processExisting(7L, 8L)).getErrorCode());

        when(blogVideos.get(9L)).thenReturn(new BlogVideoClient.BlogVideoMetadata(
                9L, "DRAFT", "UPLOAD", "https://res.cloudinary.com/veggiepal/video/upload/x.mp4"));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,
                assertThrows(AppException.class, () -> service.processExisting(7L, 9L)).getErrorCode());
        verifyNoInteractions(downloader);
        verify(quota, never()).reserveVideo(any());
    }

    @Test
    void downloadOversizeAndFailureDoNotReserveQuota() {
        publishedUpload();
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.empty());
        when(downloader.download(any())).thenThrow(new AppException(ErrorCode.FILE_TOO_LARGE),
                new AppException(ErrorCode.VIDEO_PROCESSING_FAILED));
        assertEquals(ErrorCode.FILE_TOO_LARGE,
                assertThrows(AppException.class, () -> service.processExisting(7L, 8L)).getErrorCode());

        assertEquals(ErrorCode.VIDEO_PROCESSING_FAILED,
                assertThrows(AppException.class, () -> service.processExisting(7L, 8L)).getErrorCode());
        verify(quota, never()).reserveVideo(any());
    }

    @Test
    void cachedCompletedResultIsReusedWithoutDownloadQuotaOrTranscriptDisclosure() {
        publishedUpload();
        VideoJob completed = new VideoJob(22L, "source.mp4", null, true, LocalDate.now(), 8L);
        completed.complete("private transcript", "{\"title\":\"Soup\"}");
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.of(completed));

        var response = service.processExisting(7L, 8L);
        var shared = service.sharedResult(8L);

        assertEquals(8L, response.sourceVideoId());
        assertEquals(null, response.transcript());
        assertEquals("Soup", ((java.util.Map<?, ?>) shared.extraction()).get("title"));
        verifyNoInteractions(downloader);
        verify(quota, never()).reserveVideo(any());
    }

    @Test
    void failedSourceJobIsReleasedAndRetryCreatesAChargedJob() {
        publishedUpload();
        VideoJob failed = new VideoJob(22L, "source.mp4", null, true, LocalDate.now(), 8L);
        failed.fail("first attempt failed");
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.of(failed), Optional.empty());
        when(downloader.download(any())).thenReturn(stored);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.processExisting(7L, 8L);

        assertEquals(VideoJob.PROCESSING, response.status());
        verify(states).releaseReusableSource(8L);
        verify(quota).reserveVideo(7L);
        verify(repository).saveAndFlush(any(VideoJob.class));
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    void completedSourceWithoutRetainedExtractionRunsAgain() {
        publishedUpload();
        VideoJob expired = new VideoJob(22L, "source.mp4", null, true, LocalDate.now(), 8L);
        expired.complete("old transcript", "{\"title\":\"Old\"}");
        expired.purgeOutput();
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.of(expired), Optional.empty());
        when(downloader.download(any())).thenReturn(stored);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.processExisting(7L, 8L);

        assertEquals(VideoJob.PROCESSING, response.status());
        verify(states).releaseReusableSource(8L);
        verify(quota).reserveVideo(7L);
    }

    @Test
    void sharedExpiredResultIsNotFoundInsteadOfEmptySuccess() {
        publishedUpload();
        VideoJob expired = new VideoJob(22L, "source.mp4", null, true, LocalDate.now(), 8L);
        expired.complete("old transcript", "{\"title\":\"Old\"}");
        expired.purgeOutput();
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.of(expired));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,
                assertThrows(AppException.class, () -> service.sharedResult(8L)).getErrorCode());
    }

    @Test
    void responseTimestampsAreExplicitUtcInstants() {
        VideoJob job = new VideoJob(8L, "history.mp4");
        when(repository.findByIdAndUserId(10L, 8L)).thenReturn(Optional.of(job));

        var response = service.get(8L, 10L);

        assertEquals(job.getCreatedAt().toInstant(ZoneOffset.UTC), response.createdAt());
        assertEquals(job.getUpdatedAt().toInstant(ZoneOffset.UTC), response.updatedAt());
    }

    @Test
    void uniqueConstraintRaceRefundsLoserAndReturnsWinnerWithoutSecondWorker() {
        publishedUpload();
        VideoJob winner = new VideoJob(22L, "source.mp4", null, true, LocalDate.now(), 8L);
        when(repository.findBySourceVideoId(8L))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(winner));
        when(downloader.download(any())).thenReturn(stored);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        var response = service.processExisting(7L, 8L);

        assertEquals(8L, response.sourceVideoId());
        verify(quota).reserveVideo(7L);
        verify(quota).refundVideo(7L);
        verify(storage).delete(stored);
        verify(executor, never()).execute(any());
    }

    @Test
    void persistenceFailureAfterReservationRefundsExactlyOnce() {
        publishedUpload();
        when(repository.findBySourceVideoId(8L)).thenReturn(Optional.empty());
        when(downloader.download(any())).thenReturn(stored);
        when(repository.saveAndFlush(any())).thenThrow(new RuntimeException("database"));

        assertEquals(ErrorCode.VIDEO_PROCESSING_FAILED,
                assertThrows(AppException.class, () -> service.processExisting(7L, 8L)).getErrorCode());
        verify(quota, times(1)).refundVideo(7L);
        verify(storage).delete(stored);
    }

    private void publishedUpload() {
        when(blogVideos.get(8L)).thenReturn(new BlogVideoClient.BlogVideoMetadata(8L, "PUBLISHED", "UPLOAD",
                "https://res.cloudinary.com/veggiepal/video/upload/v1/meal.mp4"));
    }

    private MockMultipartFile video() {
        return new MockMultipartFile("file", "meal.mp4", "video/mp4", new byte[]{1});
    }
}
