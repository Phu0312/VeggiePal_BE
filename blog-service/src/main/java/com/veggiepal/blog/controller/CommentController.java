package com.veggiepal.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Comment", description = "Comments and one level of replies")
public class CommentController {

    CommentService commentService;

    @Operation(summary = "Top-level comments on a piece of content")
    @GetMapping
    ApiResponse<PageResponse<CommentResponse>> getRootComments(
            @RequestParam(name = "targetType", defaultValue = "BLOG") TargetType targetType,
            @RequestParam(name = "targetId") Long targetId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<CommentResponse>>builder()
                .result(commentService.getRootComments(targetType, targetId, page, size))
                .build();
    }

    @Operation(summary = "Replies under one comment")
    @GetMapping("/{id}/replies")
    ApiResponse<PageResponse<CommentResponse>> getReplies(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<CommentResponse>>builder()
                .result(commentService.getReplies(id, page, size))
                .build();
    }

    @Operation(summary = "Post a comment or a reply")
    @PostMapping
    ApiResponse<CommentResponse> createComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CommentRequest request
    ) {

        return ApiResponse
                .<CommentResponse>builder()
                .result(commentService.createComment(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(summary = "Edit your own comment")
    @PutMapping("/{id}")
    ApiResponse<CommentResponse> updateComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid CommentRequest request
    ) {

        return ApiResponse
                .<CommentResponse>builder()
                .result(commentService.updateComment(CurrentUser.id(jwt), id, request))
                .build();
    }

    @Operation(summary = "Delete a comment; the owner or an admin")
    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        commentService.deleteComment(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id);
        return ApiResponse.<Void>builder().build();
    }
}
