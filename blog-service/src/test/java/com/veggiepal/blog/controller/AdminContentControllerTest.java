package com.veggiepal.blog.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.service.AdminContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(AdminContentController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class AdminContentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminContentService service;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().jwt(t -> t.claim("userId", 1L).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor member() {
        return jwt().jwt(t -> t.claim("userId", 7L).claim("role", "USER"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test void adminCanListAndFilterBothContentTypes() throws Exception {
        when(service.blogs(nullable(com.veggiepal.blog.enums.ContentStatus.class), nullable(Long.class),
                nullable(String.class), anyInt(), anyInt()))
                .thenReturn(PageResponse.<com.veggiepal.blog.dto.response.BlogSummaryResponse>builder().items(List.of()).build());
        when(service.videos(nullable(com.veggiepal.blog.enums.ContentStatus.class), nullable(Long.class),
                nullable(String.class), anyInt(), anyInt()))
                .thenReturn(PageResponse.<com.veggiepal.blog.dto.response.VideoResponse>builder().items(List.of()).build());
        mvc.perform(get("/admin/blogs").with(admin()).param("status", "REJECTED")
                .param("authorId", "7").param("keyword", "pho"))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/videos").with(admin()).param("status", "PENDING")
                .param("authorId", "7").param("keyword", "pho"))
                .andExpect(status().isOk());
        verify(service).blogs(eq(com.veggiepal.blog.enums.ContentStatus.REJECTED), eq(7L), eq("pho"), eq(0), eq(20));
        verify(service).videos(eq(com.veggiepal.blog.enums.ContentStatus.PENDING), eq(7L), eq("pho"), eq(0), eq(20));
    }

    @Test void memberCannotListOrChangeStatus() throws Exception {
        mvc.perform(get("/admin/blogs").with(member())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/videos").with(member())).andExpect(status().isForbidden());
        mvc.perform(put("/admin/blogs/10/status").with(member()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\",\"reason\":\"False positive\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/admin/videos/10/status").with(member()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\",\"reason\":\"False positive\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void adminDecisionUsesJwtIdentity() throws Exception {
        mvc.perform(put("/admin/blogs/10/status").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\",\"reason\":\"False positive\"}"))
                .andExpect(status().isOk());
        verify(service).changeBlogStatus(10L, 1L, com.veggiepal.blog.enums.ContentStatus.PUBLISHED, "False positive");
    }
}
