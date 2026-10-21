package com.veggiepal.payment.controller;

import com.veggiepal.payment.dto.response.*;
import com.veggiepal.payment.security.InternalServiceAuthenticator;
import com.veggiepal.payment.service.SubscriptionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/subscriptions")
public class InternalSubscriptionController {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";
    private final SubscriptionService subscriptions;
    private final InternalServiceAuthenticator authenticator;

    public InternalSubscriptionController(SubscriptionService subscriptions, InternalServiceAuthenticator authenticator) {
        this.subscriptions = subscriptions; this.authenticator = authenticator;
    }

    @GetMapping("/{userId}")
    public ApiResponse<SubscriptionResponse> get(@PathVariable Long userId,
                                                  @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String token) {
        authenticator.requireValid(token);
        return ApiResponse.<SubscriptionResponse>builder().result(subscriptions.getForUser(userId)).build();
    }
}
