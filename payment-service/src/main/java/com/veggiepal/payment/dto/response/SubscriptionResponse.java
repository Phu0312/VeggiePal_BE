package com.veggiepal.payment.dto.response;

import com.veggiepal.payment.entity.PlanCode;
import java.time.Instant;

public record SubscriptionResponse(PlanCode planCode, Instant startedAt, Instant expiresAt,
                                   int chatPerDay, int mealPlansPerMonth, int videosPerMonth) {}
