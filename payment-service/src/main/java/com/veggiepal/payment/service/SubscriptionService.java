package com.veggiepal.payment.service;

import com.veggiepal.payment.dto.response.SubscriptionResponse;
import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.exception.*;
import com.veggiepal.payment.repository.*;
import java.time.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {
    private final UserSubscriptionRepository subscriptions;
    private final SubscriptionPlanRepository plans;
    private final Clock clock;

    public SubscriptionService(UserSubscriptionRepository subscriptions, SubscriptionPlanRepository plans, Clock clock) {
        this.subscriptions = subscriptions; this.plans = plans; this.clock = clock;
    }

    @Transactional
    public SubscriptionResponse getForUser(Long userId) {
        Instant now = clock.instant();
        UserSubscription subscription = subscriptions.findByUserIdForUpdate(userId)
                .orElseGet(() -> createFree(userId, now));
        if (isExpired(subscription, now)) fallbackToFree(subscription, now);
        return response(subscription);
    }

    @Transactional
    public void activate(Long userId, SubscriptionPlan purchasedPlan, Instant paidAt) {
        UserSubscription subscription = subscriptions.findByUserIdForUpdate(userId).orElse(null);
        if (subscription == null) {
            subscription = UserSubscription.builder().userId(userId).build();
        }
        boolean sameActivePlan = subscription.getPlan() != null
                && subscription.getPlan().getCode() == purchasedPlan.getCode()
                && subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(paidAt);
        Instant startsAt = sameActivePlan ? subscription.getStartedAt() : paidAt;
        Instant durationBase = sameActivePlan ? subscription.getExpiresAt() : paidAt;
        subscription.setPlan(purchasedPlan);
        subscription.setStartedAt(startsAt);
        subscription.setExpiresAt(durationBase.plus(purchasedPlan.getDurationDays(), java.time.temporal.ChronoUnit.DAYS));
        subscription.setUpdatedAt(paidAt);
        subscriptions.save(subscription);
    }

    @Scheduled(fixedDelayString = "${subscription.expiration-check-ms:3600000}")
    @Transactional
    public void expireSubscriptions() {
        Instant now = clock.instant();
        subscriptions.findExpired(now).forEach(subscription -> fallbackToFree(subscription, now));
    }

    private UserSubscription createFree(Long userId, Instant now) {
        return subscriptions.save(UserSubscription.builder().userId(userId).plan(freePlan())
                .startedAt(now).updatedAt(now).build());
    }
    private void fallbackToFree(UserSubscription subscription, Instant now) {
        subscription.setPlan(freePlan());
        subscription.setStartedAt(now);
        subscription.setExpiresAt(null);
        subscription.setUpdatedAt(now);
        subscriptions.save(subscription);
    }
    private boolean isExpired(UserSubscription s, Instant now) {
        return s.getExpiresAt() != null && !s.getExpiresAt().isAfter(now);
    }
    private SubscriptionPlan freePlan() {
        return plans.findByCodeAndActiveTrue(PlanCode.FREE).orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
    }
    private SubscriptionResponse response(UserSubscription s) {
        SubscriptionPlan p = s.getPlan();
        return new SubscriptionResponse(p.getCode(), s.getStartedAt(), s.getExpiresAt(), p.getChatPerDay(),
                p.getMealPlansPerMonth(), p.getVideosPerMonth());
    }
}
