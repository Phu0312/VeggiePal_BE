package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class VideoProcessingServiceTest {

    private VideoJobRepository repository;
    private GroqClient groq;
    private ModelProperties models;
    private ObjectMapper mapper;
    private UsageService usage;
    private VideoProcessingService service;

    @BeforeEach
    void setUp() {
        repository = mock(VideoJobRepository.class);
        groq = mock(GroqClient.class);
        models = new ModelProperties("c", "m", "r", "p", "v", "w");
        mapper = new ObjectMapper();
        usage = mock(UsageService.class);
        service = new VideoProcessingService(
                repository,
                groq,
                models,
                mapper,
                usage,
                "ffmpeg",
                "ffprobe",
                50_000_000L,
                1800L
        );
    }

    @Test
    void processThrowsInvalidRequestWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", new byte[0]);

        AppException ex = assertThrows(AppException.class, () -> service.process(1L, file));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void processThrowsInvalidRequestWhenContentTypeIsNotVideo() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "fake pdf content".getBytes());

        AppException ex = assertThrows(AppException.class, () -> service.process(1L, file));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void processThrowsFileTooLargeWhenFileSizeExceedsLimit() {
        VideoProcessingService strictService = new VideoProcessingService(
                repository, groq, models, mapper, usage, "ffmpeg", "ffprobe", 10L, 1800L
        );
        MockMultipartFile file = new MockMultipartFile("file", "large.mp4", "video/mp4", new byte[100]);

        AppException ex = assertThrows(AppException.class, () -> strictService.process(1L, file));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getErrorCode());
    }

    @Test
    void getThrowsResourceNotFoundWhenJobDoesNotExist() {
        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.get(1L, 99L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getReturnsJobResponseWhenFound() {
        VideoJob job = new VideoJob(1L, "cooking.mp4");
        job.complete("transcribed text", "{\"title\":\"Vegan Soup\",\"ingredients\":[],\"steps\":[]}");
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(job));

        VideoDtos.VideoJobResponse response = service.get(1L, 10L);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals("cooking.mp4", response.filename());
        assertEquals("transcribed text", response.transcript());
    }
}
