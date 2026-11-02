package com.veggiepal.blog.controller;

import com.veggiepal.blog.dto.request.AdminStatusRequest;
import com.veggiepal.blog.dto.response.*;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.AdminContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContentController {
    private final AdminContentService service;

    @GetMapping("/blogs")
    ApiResponse<PageResponse<BlogSummaryResponse>> blogs(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<BlogSummaryResponse>>builder()
                .result(service.blogs(status, authorId, keyword, page, size)).build();
    }

    @GetMapping("/videos")
    ApiResponse<PageResponse<VideoResponse>> videos(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<VideoResponse>>builder()
                .result(service.videos(status, authorId, keyword, page, size)).build();
    }

    @GetMapping("/videos/{id}")
    ApiResponse<VideoResponse> video(@PathVariable Long id) {
        return ApiResponse.<VideoResponse>builder().result(service.video(id)).build();
    }

    @PutMapping("/blogs/{id}/status")
    ApiResponse<BlogResponse> blogStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @RequestBody @Valid AdminStatusRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .result(service.changeBlogStatus(id, CurrentUser.id(jwt), request.status(), request.reason())).build();
    }

    @PutMapping("/videos/{id}/status")
    ApiResponse<VideoResponse> videoStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                           @RequestBody @Valid AdminStatusRequest request) {
        return ApiResponse.<VideoResponse>builder()
                .result(service.changeVideoStatus(id, CurrentUser.id(jwt), request.status(), request.reason())).build();
    }
}
