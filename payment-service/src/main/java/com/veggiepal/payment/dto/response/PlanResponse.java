package com.veggiepal.payment.dto.response;

import com.veggiepal.payment.entity.PlanCode;

public record PlanResponse(Long id, PlanCode code, long price, int durationDays, int chatPerDay,
                           int mealPlansPerMonth, int videosPerMonth) {}
