package com.veggiepal.ai.moderation;

import static com.veggiepal.ai.moderation.ModerationDtos.*;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.security.InternalServiceToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * blog-service's moderation entry point. Not routed by the gateway, which only forwards AI paths,
 * and guarded by the shared internal token rather than a user's JWT.
 */
@RestController @RequiredArgsConstructor
public class InternalModerationController {
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final ModerationService service;
    private final InternalServiceToken tokens;

    @PostMapping("/internal/moderation/check")
    ApiResponse<ModerationResponse> check(
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String token,
            @RequestBody @Valid ModerationRequest request) {
        tokens.require(token);
        return ApiResponse.success(service.moderateForService(request));
    }
}
