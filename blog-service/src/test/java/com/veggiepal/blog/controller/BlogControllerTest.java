package com.veggiepal.blog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.BlogService;

@WebMvcTest(BlogController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class BlogControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BlogService blogService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"));
    }

    static String validBody() {
        return """
                {"title": "Đậu hũ sốt cà", "content": "%s", "categoryId": 3, "publish": true}
                """.formatted("x".repeat(50));
    }

    @Test
    void createBlog_withToken_usesUserIdFromClaim() throws Exception {
        when(blogService.createBlog(eq(USER_ID), any(BlogRequest.class)))
                .thenReturn(BlogResponse.builder().id(10L).status(ContentStatus.PUBLISHED).build());

        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"));

        verify(blogService).createBlog(eq(USER_ID), any(BlogRequest.class));
    }

    @Test
    void createBlog_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(post("/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        verify(blogService, never()).createBlog(any(), any());
    }

    @Test
    void createBlog_blankTitle_returnsTitleRequired() throws Exception {
        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  ", "content": "%s", "categoryId": 3}
                                """.formatted("x".repeat(50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3010));
    }

    @Test
    void createBlog_titleTooLong_returnsInvalidTitleWithMaxFilledIn() throws Exception {
        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "content": "%s", "categoryId": 3}
                                """.formatted("t".repeat(201), "x".repeat(50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3011))
                .andExpect(jsonPath("$.message").value("Blog title must be at most 200 characters"));
    }

    @Test
    void createBlog_contentTooShort_returnsInvalidContentWithMinFilledIn() throws Exception {
        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Đậu hũ", "content": "ngắn", "categoryId": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3013))
                .andExpect(jsonPath("$.message").value("Blog content must be at least 20 characters"));
    }

    // The trap from the spec: /blogs/me must NOT be treated as /blogs/{id}
    @Test
    void getOwnBlogs_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/blogs/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void getOwnBlogs_withToken_reachesTheService() throws Exception {
        when(blogService.getOwnBlogs(USER_ID, null, 0, 20))
                .thenReturn(PageResponse.<com.veggiepal.blog.dto.response.BlogSummaryResponse>builder()
                        .items(java.util.List.of()).page(0).size(20).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/blogs/me").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0));

        verify(blogService).getOwnBlogs(USER_ID, null, 0, 20);
    }

    @Test
    void deleteBlog_asAdmin_passesAdminFlag() throws Exception {
        mockMvc.perform(delete("/blogs/10").with(admin()))
                .andExpect(status().isOk());

        verify(blogService).deleteBlog(1L, true, 10L);
    }

    @Test
    void deleteBlog_asMember_passesAdminFlagFalse() throws Exception {
        mockMvc.perform(delete("/blogs/10").with(member()))
                .andExpect(status().isOk());

        verify(blogService).deleteBlog(USER_ID, false, 10L);
    }

    @Test
    void getPublishedBlogs_withoutToken_isPublic() throws Exception {
        when(blogService.getPublishedBlogs(null, null, null, 0, 20))
                .thenReturn(PageResponse.<BlogSummaryResponse>builder()
                        .items(List.of()).page(0).size(20).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void getPublishedBlog_withoutToken_isPublic() throws Exception {
        when(blogService.getPublishedBlog(10L))
                .thenReturn(BlogResponse.builder().id(10L).viewCount(42).build());

        mockMvc.perform(get("/blogs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(42));
    }

    // A guest arriving with a leftover expired token must still be able to read
    @Test
    void getPublishedBlogs_withExpiredLookingToken_stillPublic() throws Exception {
        when(blogService.getPublishedBlogs(null, null, null, 0, 20))
                .thenReturn(PageResponse.<BlogSummaryResponse>builder()
                        .items(List.of()).page(0).size(20).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/blogs").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getRelatedBlogs_withoutToken_isPublic() throws Exception {
        when(blogService.getRelatedBlogs(10L)).thenReturn(List.of());

        mockMvc.perform(get("/blogs/10/related"))
                .andExpect(status().isOk());
    }
}
