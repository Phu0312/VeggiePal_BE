package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.common.AppException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

class VideoProcessingControllerTest {
    @Test
    void processReturnsAcceptedContractWithJobId() {
        VideoProcessingService service = mock(VideoProcessingService.class);
        var file = new MockMultipartFile("file", "meal.mp4", "video/mp4", new byte[]{1});
        var response = new VideoDtos.VideoJobResponse(42L, VideoJob.PROCESSING, "meal.mp4", null,
                null, null, LocalDateTime.now(), LocalDateTime.now());
        when(service.process(7L, file)).thenReturn(response);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("userId", 7L));

        var result = new VideoProcessingController(service).process(jwt, file);

        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        assertEquals(42L, result.getBody().result().id());
        verify(service).process(7L, file);
    }

    @Test
    void missingJwtIsRejectedBeforeServiceCall() {
        VideoProcessingService service = mock(VideoProcessingService.class);
        var file = new MockMultipartFile("file", "meal.mp4", "video/mp4", new byte[]{1});
        assertThrows(AppException.class, () -> new VideoProcessingController(service).process(null, file));
    }
}
