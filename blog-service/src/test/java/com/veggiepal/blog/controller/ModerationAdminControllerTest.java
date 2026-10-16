package com.veggiepal.blog.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.service.BlogService;
import com.veggiepal.blog.service.CommentService;
import com.veggiepal.blog.service.VideoService;

@WebMvcTest(ModerationAdminController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class ModerationAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BlogService blogService;

    @MockitoBean
    CommentService commentService;

    @MockitoBean
    VideoService videoService;

    // ai-service sends an admin's decision on a REVIEW_REQUIRED video here
    @Test
    void applyVideoDecision_asAdmin_reachesTheVideoService() throws Exception {
        mockMvc.perform(put("/internal/moderation/VIDEO/10")
                        .with(jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","authorId":7}
                                """))
                .andExpect(status().isOk());

        verify(videoService).applyAdminModeration(10L, 7L, true);
    }

    @Test
    void applyVideoDecision_asMember_isForbidden() throws Exception {
        mockMvc.perform(put("/internal/moderation/VIDEO/10")
                        .with(jwt().jwt(token -> token.claim("userId", 7L).claim("role", "USER"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","authorId":7}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1009));

        verifyNoInteractions(videoService);
    }

    @Test
    void applyVideoDecision_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(put("/internal/moderation/VIDEO/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","authorId":7}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }
}
