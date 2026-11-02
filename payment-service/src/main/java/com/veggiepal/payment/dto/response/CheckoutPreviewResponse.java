package com.veggiepal.payment.dto.response;

import com.veggiepal.payment.entity.PlanCode;
import java.time.Instant;

public record CheckoutPreviewResponse(PlanCode currentPlan, PlanCode targetPlan, long remainingDays,
                                      long bonusDays, Instant newExpiresAt, Instant calculatedAt) { }
