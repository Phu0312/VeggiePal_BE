package com.veggiepal.payment.controller;

import com.veggiepal.payment.dto.request.CheckoutRequest;
import com.veggiepal.payment.dto.response.*;
import com.veggiepal.payment.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments")
public class PaymentController {
    private final PlanService planService;
    private final PaymentService paymentService;
    public PaymentController(PlanService planService, PaymentService paymentService) {
        this.planService = planService; this.paymentService = paymentService;
    }

    @GetMapping("/plans")
    @Operation(summary = "List subscription plans")
    public ApiResponse<List<PlanResponse>> plans() {
        return ApiResponse.<List<PlanResponse>>builder().result(planService.getPlans()).build();
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create a payOS checkout for a paid plan")
    public ApiResponse<PaymentOrderResponse> checkout(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.<PaymentOrderResponse>builder()
                .result(paymentService.checkout(CurrentUser.id(jwt), request.planCode())).build();
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get one of the current user's payment orders")
    public ApiResponse<PaymentOrderResponse> order(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable Long id) {
        return ApiResponse.<PaymentOrderResponse>builder()
                .result(paymentService.getOrder(CurrentUser.id(jwt), id)).build();
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel one of the current user's pending payment orders")
    public ApiResponse<PaymentOrderResponse> cancel(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable Long id) {
        return ApiResponse.<PaymentOrderResponse>builder()
                .result(paymentService.cancel(CurrentUser.id(jwt), id)).build();
    }

    @GetMapping("/history")
    @Operation(summary = "List the current user's payment history")
    public ApiResponse<List<PaymentOrderResponse>> history(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<PaymentOrderResponse>>builder()
                .result(paymentService.history(CurrentUser.id(jwt))).build();
    }

    @PostMapping("/webhook/payos")
    @Operation(summary = "Receive and verify a payOS webhook")
    public ApiResponse<String> webhook(@RequestBody Map<String, Object> body) {
        paymentService.processWebhook(body);
        return ApiResponse.<String>builder().message("Webhook processed").result("success").build();
    }
}
