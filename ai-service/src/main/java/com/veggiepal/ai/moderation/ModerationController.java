package com.veggiepal.ai.moderation;

import static com.veggiepal.ai.moderation.ModerationDtos.*;
import java.util.List;
import com.veggiepal.ai.common.ApiResponse;
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
    @PutMapping("/ai/admin/moderation/{id}") ApiResponse<ModerationResponse> review(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,@RequestBody @Valid ReviewRequest request) {
        return ApiResponse.success(service.review(CurrentUser.id(jwt),id,request,jwt.getTokenValue()));
    }
}
