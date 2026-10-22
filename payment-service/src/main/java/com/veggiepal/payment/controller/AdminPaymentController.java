package com.veggiepal.payment.controller;

import com.veggiepal.payment.dto.response.AdminPaymentDtos;
import com.veggiepal.payment.dto.response.ApiResponse;
import com.veggiepal.payment.entity.PaymentStatus;
import com.veggiepal.payment.entity.PlanCode;
import com.veggiepal.payment.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/payments")
@Tag(name = "Admin Payments")
public class AdminPaymentController {
    private final AdminPaymentService service;

    public AdminPaymentController(AdminPaymentService service) { this.service = service; }

    @GetMapping("/summary")
    @Operation(summary = "Payment dashboard totals")
    public ApiResponse<AdminPaymentDtos.Summary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) PlanCode planCode) {
        return result(service.summary(from, to, planCode));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Status, plan, and daily payment statistics")
    public ApiResponse<AdminPaymentDtos.Statistics> statistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) PlanCode planCode) {
        return result(service.statistics(from, to, planCode));
    }

    @GetMapping("/orders")
    @Operation(summary = "Paginated payment orders")
    public ApiResponse<AdminPaymentDtos.OrderPage> orders(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PlanCode planCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return result(service.findOrders(status, planCode, from, to, page, size));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Payment order detail")
    public ApiResponse<AdminPaymentDtos.Order> order(@PathVariable Long id) {
        return result(service.getOrder(id));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Verified revenue chart")
    public ApiResponse<AdminPaymentDtos.Revenue> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) PlanCode planCode,
            @RequestParam(defaultValue = "DAY") AdminPaymentService.GroupBy groupBy) {
        return result(service.revenue(from, to, planCode, groupBy));
    }

    private static <T> ApiResponse<T> result(T result) {
        return ApiResponse.<T>builder().result(result).build();
    }
}
