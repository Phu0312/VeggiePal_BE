package com.veggiepal.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.BlogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Blog", description = "Community blog posts")
public class BlogController {

    BlogService blogService;

    @Operation(summary = "Create a blog; publish=true runs moderation right away")
    @PostMapping
    ApiResponse<BlogResponse> createBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid BlogRequest request
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.createBlog(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(summary = "My blogs in any status")
    @GetMapping("/me")
    ApiResponse<PageResponse<BlogSummaryResponse>> getOwnBlogs(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "status", required = false) ContentStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<BlogSummaryResponse>>builder()
                .result(blogService.getOwnBlogs(CurrentUser.id(jwt), status, page, size))
                .build();
    }

    @Operation(summary = "Edit a blog; anything already reviewed goes back through moderation")
    @PutMapping("/{id}")
    ApiResponse<BlogResponse> updateBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid BlogRequest request
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.updateBlog(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id, request))
                .build();
    }

    @Operation(summary = "Submit a draft for moderation")
    @PostMapping("/{id}/submit")
    ApiResponse<BlogResponse> submitBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.submitBlog(CurrentUser.id(jwt), id))
                .build();
    }

    @Operation(summary = "Delete a blog; the owner or an admin")
    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        blogService.deleteBlog(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id);
        return ApiResponse.<Void>builder().build();
    }
}
