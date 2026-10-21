package com.veggiepal.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.veggiepal.payment.entity.*;
import java.time.Instant;

public record PaymentOrderResponse(Long id, Long orderCode, PlanCode planCode, long amount,
                                   PaymentStatus status, String checkoutUrl, String qrCode,
                                   Instant createdAt, Instant paidAt) {
    @JsonProperty("orderId")
    public Long orderId() { return id; }
}
