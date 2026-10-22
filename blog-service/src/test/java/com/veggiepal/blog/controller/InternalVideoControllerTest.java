package com.veggiepal.blog.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veggiepal.blog.configuration.InternalServiceToken;
import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.entity.Video;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.enums.VideoSource;
import com.veggiepal.blog.repository.VideoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalVideoController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class, InternalServiceToken.class})
@TestPropertySource(properties = "services.internal-token=test-internal-token")
class InternalVideoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean VideoRepository videos;

    @Test
    void authorizedReadReturnsMetadataWithoutIncrementingViews() throws Exception {
        Video video = Video.builder().id(8L).status(ContentStatus.PUBLISHED).sourceType(VideoSource.UPLOAD)
                .videoUrl("https://res.cloudinary.com/veggiepal/video/upload/v1/videos/8/a.mp4").build();
        when(videos.findById(8L)).thenReturn(Optional.of(video));

        mvc.perform(get("/internal/videos/8").header("X-Internal-Service-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(8))
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.result.sourceType").value("UPLOAD"));

        verify(videos, never()).incrementViewCount(8L);
    }

    @Test
    void missingOrWrongInternalTokenIsUnauthorizedAndDoesNotReadVideo() throws Exception {
        mvc.perform(get("/internal/videos/8")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/videos/8").header("X-Internal-Service-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        verify(videos, never()).findById(8L);
    }
}
