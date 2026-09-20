package com.veggiepal.blog.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.request.VoteRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.dto.response.VoteResponse;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.BlogService;
import com.veggiepal.blog.service.VoteService;

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
    VoteService voteService;

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

    @Operation(summary = "Upload or replace the cover image")
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<BlogResponse> uploadThumbnail(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.uploadThumbnail(
                        CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id, file))
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

    @Operation(summary = "Published blogs; keyword searches title and content")
    @GetMapping
    ApiResponse<PageResponse<BlogSummaryResponse>> getPublishedBlogs(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<BlogSummaryResponse>>builder()
                .result(blogService.getPublishedBlogs(categoryId, keyword, sort, page, size))
                .build();
    }

    @Operation(summary = "One published blog; counts a view")
    @GetMapping("/{id}")
    ApiResponse<BlogResponse> getPublishedBlog(
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.getPublishedBlog(id))
                .build();
    }

    @Operation(summary = "Up to five published blogs in the same category")
    @GetMapping("/{id}/related")
    ApiResponse<List<BlogSummaryResponse>> getRelatedBlogs(
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<List<BlogSummaryResponse>>builder()
                .result(blogService.getRelatedBlogs(id))
                .build();
    }

    @Operation(summary = "Set your vote on a blog; 1 or -1")
    @PutMapping("/{id}/vote")
    ApiResponse<VoteResponse> vote(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid VoteRequest request
    ) {

        return ApiResponse
                .<VoteResponse>builder()
                .result(voteService.vote(CurrentUser.id(jwt), id, request.getValue()))
                .build();
    }

    @Operation(summary = "Remove your vote; doing it twice is harmless")
    @DeleteMapping("/{id}/vote")
    ApiResponse<VoteResponse> removeVote(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<VoteResponse>builder()
                .result(voteService.removeVote(CurrentUser.id(jwt), id))
                .build();
    }

    @Operation(summary = "My vote on a list of blogs, to overlay on a public listing")
    @GetMapping("/me/votes")
    ApiResponse<List<VoteResponse>> getMyVotes(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "blogIds") List<Long> blogIds
    ) {

        return ApiResponse
                .<List<VoteResponse>>builder()
                .result(voteService.getMyVotes(CurrentUser.id(jwt), blogIds))
                .build();
    }
}
