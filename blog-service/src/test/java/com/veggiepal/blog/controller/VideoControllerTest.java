package com.veggiepal.blog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.request.VideoRequest;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.dto.response.VideoResponse;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.VideoService;

@WebMvcTest(VideoController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class VideoControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    VideoService videoService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"));
    }

    static MockMultipartFile mp4File() {
        return new MockMultipartFile("file", "canh-chua.mp4", "video/mp4", new byte[]{0, 0, 0, 0x20});
    }

    @Test
    void createVideo_multipartWithFile_passesFormFieldsAndFileToTheService() throws Exception {
        when(videoService.createVideo(eq(USER_ID), any(VideoRequest.class), any(MultipartFile.class)))
                .thenReturn(VideoResponse.builder().id(10L).status(ContentStatus.PUBLISHED).build());

        mockMvc.perform(multipart("/videos").file(mp4File()).with(member())
                        .param("title", "Canh chua chay")
                        .param("description", "Nấu trong 15 phút")
                        .param("categoryId", "3")
                        .param("publish", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"));

        ArgumentCaptor<VideoRequest> request = ArgumentCaptor.forClass(VideoRequest.class);
        verify(videoService).createVideo(eq(USER_ID), request.capture(), any(MultipartFile.class));
        assertThat(request.getValue().getTitle()).isEqualTo("Canh chua chay");
        assertThat(request.getValue().getCategoryId()).isEqualTo(3L);
        assertThat(request.getValue().getPublish()).isTrue();
    }

    @Test
    void createVideo_youtubeLinkWithoutFile_passesANullFile() throws Exception {
        mockMvc.perform(multipart("/videos").with(member())
                        .param("title", "Canh chua chay")
                        .param("categoryId", "3")
                        .param("youtubeUrl", "https://youtu.be/dQw4w9WgXcQ"))
                .andExpect(status().isOk());

        verify(videoService).createVideo(eq(USER_ID), any(VideoRequest.class), isNull());
    }

    // @ModelAttribute validation must reach the same handler as @RequestBody validation
    @Test
    void createVideo_blankTitle_returnsVideoTitleRequired() throws Exception {
        mockMvc.perform(multipart("/videos").file(mp4File()).with(member())
                        .param("title", " ")
                        .param("categoryId", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3061));

        verify(videoService, never()).createVideo(any(), any(), any());
    }

    @Test
    void createVideo_withoutToken_returns401() throws Exception {
        mockMvc.perform(multipart("/videos").file(mp4File())
                        .param("title", "Canh chua chay")
                        .param("categoryId", "3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void getPublishedVideos_isPublicAndDefaultsToTenPerPage() throws Exception {
        when(videoService.getPublishedVideos(null, 0, 10))
                .thenReturn(PageResponse.<VideoResponse>builder().items(List.of()).page(0).size(10).build());

        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.size").value(10));
    }

    @Test
    void getPublishedVideo_isPublic() throws Exception {
        when(videoService.getPublishedVideo(10L))
                .thenReturn(VideoResponse.builder().id(10L).viewCount(5).build());

        mockMvc.perform(get("/videos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(5));
    }

    @Test
    void getOwnVideos_usesUserIdFromClaim() throws Exception {
        when(videoService.getOwnVideos(USER_ID, ContentStatus.DRAFT, 0, 10))
                .thenReturn(PageResponse.<VideoResponse>builder().items(List.of()).build());

        mockMvc.perform(get("/videos/me").with(member()).param("status", "DRAFT"))
                .andExpect(status().isOk());

        verify(videoService).getOwnVideos(USER_ID, ContentStatus.DRAFT, 0, 10);
    }

    @Test
    void updateVideo_jsonBody_passesTheAdminFlag() throws Exception {
        mockMvc.perform(put("/videos/10").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Canh chua chay","categoryId":3}
                                """))
                .andExpect(status().isOk());

        verify(videoService).updateVideo(eq(1L), eq(true), eq(10L), any(VideoRequest.class));
    }

    @Test
    void updateVideo_byMember_passesNonAdmin() throws Exception {
        mockMvc.perform(put("/videos/10").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Canh chua chay","categoryId":3}
                                """))
                .andExpect(status().isOk());

        verify(videoService).updateVideo(eq(USER_ID), eq(false), eq(10L), any(VideoRequest.class));
    }

    @Test
    void submitVideo_usesUserIdFromClaim() throws Exception {
        mockMvc.perform(post("/videos/10/submit").with(member()))
                .andExpect(status().isOk());

        verify(videoService).submitVideo(USER_ID, 10L);
    }

    @Test
    void deleteVideo_byMember_passesNonAdmin() throws Exception {
        mockMvc.perform(delete("/videos/10").with(member()))
                .andExpect(status().isOk());

        verify(videoService).deleteVideo(USER_ID, false, 10L);
    }

    @Test
    void deleteVideo_byAdmin_passesAdmin() throws Exception {
        mockMvc.perform(delete("/videos/10").with(admin()))
                .andExpect(status().isOk());

        verify(videoService).deleteVideo(1L, true, 10L);
    }
}
