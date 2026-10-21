package com.veggiepal.payment.controller;

import com.veggiepal.payment.dto.response.*;
import com.veggiepal.payment.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@Tag(name = "Subscriptions")
public class SubscriptionController {
    private final SubscriptionService service;
    public SubscriptionController(SubscriptionService service) { this.service = service; }

    @GetMapping("/me")
    @Operation(summary = "Get the current effective subscription")
    public ApiResponse<SubscriptionResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<SubscriptionResponse>builder().result(service.getForUser(CurrentUser.id(jwt))).build();
    }
}
