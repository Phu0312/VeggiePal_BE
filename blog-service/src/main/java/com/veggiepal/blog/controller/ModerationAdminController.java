package com.veggiepal.blog.controller;

import com.veggiepal.blog.dto.request.ModerationDecisionRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.service.BlogService;
import com.veggiepal.blog.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/moderation")
@RequiredArgsConstructor
public class ModerationAdminController {

    private final BlogService blogService;
    private final CommentService commentService;

    @PutMapping("/{contentType}/{id}")
    ApiResponse<Void> apply(
            @PathVariable String contentType,
            @PathVariable Long id,
            @RequestBody @Valid ModerationDecisionRequest request
    ) {
        boolean approved = request.decision() == ModerationDecisionRequest.Decision.APPROVED;
        switch (contentType.toUpperCase()) {
            case "BLOG" -> blogService.applyAdminModeration(id, request.authorId(), approved);
            case "COMMENT" -> commentService.applyAdminModeration(id, request.authorId(), approved);
            default -> throw new AppException(ErrorCode.UNSUPPORTED_TARGET_TYPE);
        }
        return ApiResponse.<Void>builder().build();
    }
}
