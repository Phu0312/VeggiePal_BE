package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.quota.PremiumQuotaService;
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
    private PremiumQuotaService premiumQuota;
    private VideoProcessingService service;

    @BeforeEach
    void setUp() {
        repository = mock(VideoJobRepository.class);
        groq = mock(GroqClient.class);
        models = new ModelProperties("c", "m", "r", "p", "v", "w");
        mapper = new ObjectMapper();
        usage = mock(UsageService.class);
        premiumQuota = mock(PremiumQuotaService.class);
        service = new VideoProcessingService(
                repository,
                groq,
                models,
                mapper,
                usage,
                premiumQuota,
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
                repository, groq, models, mapper, usage, mock(PremiumQuotaService.class), "ffmpeg", "ffprobe", 10L, 1800L
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

    @Test
    void getReturnsExtractionThatSpringMvcWritesAsJson() {
        VideoJob job = new VideoJob(1L, "cooking.mp4");
        job.complete("transcribed text", "{\"title\":\"Vegan Soup\",\"ingredients\":[],\"steps\":[]}");
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(job));

        // Spring MVC writes responses with Jackson 3, not this service's Jackson 2 mapper.
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var written = json.readTree(json.writeValueAsString(service.get(1L, 10L)));

        assertEquals("Vegan Soup", written.path("extraction").path("title").asString());
    }

    @Test
    void quotaIsCheckedBeforeCreatingJobOrCallingAiTools() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", new byte[] {1});
        when(premiumQuota.reserveVideo(1L)).thenThrow(new AppException(ErrorCode.QUOTA_EXCEEDED));

        AppException exception = assertThrows(AppException.class, () -> service.process(1L, file));

        assertEquals(ErrorCode.QUOTA_EXCEEDED, exception.getErrorCode());
        verifyNoInteractions(repository, groq, usage);
    }
}
