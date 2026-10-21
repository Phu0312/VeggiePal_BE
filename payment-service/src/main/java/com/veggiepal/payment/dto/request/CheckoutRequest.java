package com.veggiepal.payment.dto.request;

import com.veggiepal.payment.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull(message = "PLAN_REQUIRED") PlanCode planCode) {}
