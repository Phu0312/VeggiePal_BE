package com.veggiepal.payment.service;

import com.veggiepal.payment.dto.response.SubscriptionResponse;
import com.veggiepal.payment.dto.response.CheckoutPreviewResponse;
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
        Activation outcome = calculate(subscription, purchasedPlan, paidAt, false);
        subscription.setPlan(outcome.plan());
        subscription.setStartedAt(outcome.startsAt());
        subscription.setExpiresAt(outcome.expiresAt());
        subscription.setUpdatedAt(paidAt);
        subscriptions.save(subscription);
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Long userId, SubscriptionPlan purchasedPlan, Instant at) {
        UserSubscription current = subscriptions.findByUserId(userId).orElse(null);
        Activation outcome = calculate(current, purchasedPlan, at, true);
        PlanCode currentCode = active(current, at) ? current.getPlan().getCode() : PlanCode.FREE;
        return new CheckoutPreviewResponse(currentCode, purchasedPlan.getCode(), outcome.remainingDays(),
                outcome.bonusDays(), outcome.expiresAt(), at);
    }

    private Activation calculate(UserSubscription current, SubscriptionPlan purchased, Instant at,
                                 boolean rejectDowngrade) {
        if (!active(current, at)) {
            return new Activation(purchased, at, at.plus(purchased.getDurationDays(), java.time.temporal.ChronoUnit.DAYS), 0, 0);
        }
        SubscriptionPlan old = current.getPlan();
        long remainingDays = Duration.between(at, current.getExpiresAt()).toDays();
        if (old.getCode() == purchased.getCode()) {
            return new Activation(purchased, current.getStartedAt(),
                    current.getExpiresAt().plus(purchased.getDurationDays(), java.time.temporal.ChronoUnit.DAYS),
                    remainingDays, 0);
        }
        if (old.getCode().rank() < purchased.getCode().rank()) {
            long bonusDays = remainingDays * old.getPrice() / purchased.getPrice();
            return new Activation(purchased, at,
                    at.plus(purchased.getDurationDays() + bonusDays, java.time.temporal.ChronoUnit.DAYS),
                    remainingDays, bonusDays);
        }
        if (rejectDowngrade) {
            String date = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(current.getExpiresAt());
            throw new AppException(ErrorCode.PLAN_DOWNGRADE_NOT_ALLOWED,
                    "Bạn đang dùng " + old.getCode() + " đến " + date + ", hãy mua "
                            + purchased.getCode() + " sau khi hết hạn.");
        }
        // An older, lower-tier order may be paid after a newer upgrade. Keep the higher tier
        // and convert the paid order's value into days rather than downgrading or losing payment.
        long creditDays = (long) purchased.getDurationDays() * purchased.getPrice() / old.getPrice();
        return new Activation(old, current.getStartedAt(),
                current.getExpiresAt().plus(creditDays, java.time.temporal.ChronoUnit.DAYS),
                remainingDays, creditDays);
    }

    private boolean active(UserSubscription current, Instant at) {
        return current != null && current.getPlan() != null && current.getPlan().getCode() != PlanCode.FREE
                && current.getExpiresAt() != null && current.getExpiresAt().isAfter(at);
    }

    private record Activation(SubscriptionPlan plan, Instant startsAt, Instant expiresAt,
                              long remainingDays, long bonusDays) { }

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
