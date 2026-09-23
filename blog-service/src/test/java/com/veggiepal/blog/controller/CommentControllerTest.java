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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.service.CommentService;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class CommentControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommentService commentService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    static PageResponse<CommentResponse> emptyPage() {
        return PageResponse.<CommentResponse>builder()
                .items(List.of()).page(0).size(20).totalElements(0).totalPages(0).build();
    }

    @Test
    void getRootComments_withoutToken_isPublic() throws Exception {
        when(commentService.getRootComments(TargetType.BLOG, 10L, 0, 20)).thenReturn(emptyPage());

        mockMvc.perform(get("/comments").param("targetId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    // Without the MissingServletRequestParameterException handler, this falls through
    // to the catch-all and answers 500 instead of 400.
    @Test
    void getRootComments_missingTargetId_returnsInvalidRequest() throws Exception {
        mockMvc.perform(get("/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void getReplies_withoutToken_isPublic() throws Exception {
        when(commentService.getReplies(5L, 0, 20)).thenReturn(emptyPage());

        mockMvc.perform(get("/comments/5/replies"))
                .andExpect(status().isOk());
    }

    @Test
    void createComment_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "ngon quá"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        verify(commentService, never()).createComment(any(), any());
    }

    @Test
    void createComment_withToken_usesUserIdFromClaim() throws Exception {
        when(commentService.createComment(eq(USER_ID), any(CommentRequest.class)))
                .thenReturn(CommentResponse.builder().id(5L).content("ngon quá").build());

        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "ngon quá"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(5));

        verify(commentService).createComment(eq(USER_ID), any(CommentRequest.class));
    }

    @Test
    void createComment_blankContent_returnsContentRequired() throws Exception {
        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3030));
    }

    static String words(int count) {
        return "ngon ".repeat(count).trim();
    }

    // Task sheet US5: "Độ dài nội dung < 500 từ". The sheet writes "< 150 ký tự" for the
    // title and pairs it with @Size(max = 150), so "< 500 từ" is read the same way: at most 500.
    @Test
    void createComment_moreThanFiveHundredWords_returnsInvalidContentWithMaxFilledIn() throws Exception {
        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "%s"}
                                """.formatted(words(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3031))
                .andExpect(jsonPath("$.message").value("Comment must be at most 500 words"));
    }

    @Test
    void createComment_exactlyFiveHundredWords_isAccepted() throws Exception {
        when(commentService.createComment(eq(USER_ID), any(CommentRequest.class)))
                .thenReturn(CommentResponse.builder().id(5L).build());

        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "%s"}
                                """.formatted(words(500))))
                .andExpect(status().isOk());
    }

    // A word count alone does not bound length: one 6,000-character "word" is a single word.
    // Without a character ceiling it would reach the TEXT column and fail there as a 500.
    @Test
    void createComment_oneEnormousWord_returnsCommentTooLong() throws Exception {
        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "%s"}
                                """.formatted("x".repeat(5001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3037))
                .andExpect(jsonPath("$.message").value("Comment must be at most 5000 characters"));
    }

    @Test
    void updateComment_withToken_usesUserIdFromClaim() throws Exception {
        when(commentService.updateComment(eq(USER_ID), eq(5L), any(CommentRequest.class)))
                .thenReturn(CommentResponse.builder().id(5L).content("ngon hơn nữa").build());

        mockMvc.perform(put("/comments/5").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "ngon hơn nữa"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").value("ngon hơn nữa"));

        verify(commentService).updateComment(eq(USER_ID), eq(5L), any(CommentRequest.class));
    }

    @Test
    void deleteComment_passesAdminFlagFromClaim() throws Exception {
        mockMvc.perform(delete("/comments/5").with(member()))
                .andExpect(status().isOk());

        verify(commentService).deleteComment(USER_ID, false, 5L);
    }
}
