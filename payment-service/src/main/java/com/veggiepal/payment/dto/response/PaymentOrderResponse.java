package com.veggiepal.payment.dto.response;

import com.veggiepal.payment.entity.*;
import java.time.Instant;

public record PaymentOrderResponse(Long id, Long orderCode, PlanCode planCode, long amount,
                                   PaymentStatus status, String checkoutUrl, Instant createdAt, Instant paidAt) {}
