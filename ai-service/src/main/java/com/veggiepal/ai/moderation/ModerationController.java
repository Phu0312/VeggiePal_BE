package com.veggiepal.ai.moderation;

import static com.veggiepal.ai.moderation.ModerationDtos.*;
import java.util.List;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.common.PageResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import com.veggiepal.ai.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class ModerationController {
    private final ModerationService service;
    @PostMapping("/ai/moderation/check") ApiResponse<ModerationResponse> check(@AuthenticationPrincipal Jwt jwt,@RequestBody @Valid ModerationRequest request) {
        return ApiResponse.success(service.moderate(CurrentUser.id(jwt),request));
    }
    @GetMapping("/ai/admin/moderation") ApiResponse<List<ModerationResponse>> pending() { return ApiResponse.success(service.pending()); }
    @GetMapping("/ai/admin/moderation/history") ApiResponse<PageResponse<ModerationResponse>> history(
            @RequestParam(required = false) ContentType contentType, @RequestParam(required = false) Decision decision,
            @RequestParam(required = false) Reviewer reviewer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.history(contentType, decision, reviewer, from, to, page, size));
    }
    @PutMapping("/ai/admin/moderation/{id}") ApiResponse<ModerationResponse> review(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,@RequestBody @Valid ReviewRequest request) {
        return ApiResponse.success(service.review(CurrentUser.id(jwt),id,request,jwt.getTokenValue()));
    }
    @DeleteMapping("/ai/admin/moderation/{id}") ApiResponse<ModerationResponse> dismiss(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @RequestParam(required = false) String reason) {
        return ApiResponse.success(service.dismiss(CurrentUser.id(jwt), id, reason));
    }
}
