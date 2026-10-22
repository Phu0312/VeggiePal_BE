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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.quota.PremiumQuotaService;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class VideoProcessingServiceTest {
    private VideoJobRepository repository;
    private PremiumQuotaService quota;
    private VideoStorage storage;
    private VideoMediaTools media;
    private VideoJobStateService states;
    private VideoProcessingWorker worker;
    private Executor executor;
    private VideoProcessingService service;
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
        when(storage.store(any())).thenReturn(stored);
        when(media.probe(stored)).thenReturn(new VideoMediaTools.Probe(60, "mp4"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = service(100);
    }

    private VideoProcessingService service(long maxBytes) {
        VideoProperties properties = new VideoProperties("ffmpeg", "ffprobe", maxBytes, 1800,
                Duration.ofMinutes(2), Duration.ofSeconds(20), 2, 6, "build/video", Duration.ofMinutes(20),
                Duration.ofDays(30), Duration.ofMinutes(5));
        return new VideoProcessingService(repository, quota, storage, media, states, worker,
                executor, new ObjectMapper(), properties);
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
        assertEquals(1, page.getContent().size());
        verify(repository).findAllByUserIdOrderByCreatedAtDesc(8L, PageRequest.of(1, 10));
    }

    private MockMultipartFile video() {
        return new MockMultipartFile("file", "meal.mp4", "video/mp4", new byte[]{1});
    }
}
