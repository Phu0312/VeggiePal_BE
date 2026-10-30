package com.veggiepal.ai.quota;

import com.veggiepal.ai.subscription.*;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PremiumQuotaService {
    private static final Logger log = LoggerFactory.getLogger(PremiumQuotaService.class);
    private final SubscriptionProvider subscriptions;
    private final QuotaService quotas;

    public PremiumQuotaService(SubscriptionProvider subscriptions, QuotaService quotas) {
        this.subscriptions = subscriptions; this.quotas = quotas;
    }

    public int reserveChat(Long userId) {
        SubscriptionEntitlement entitlement = effective(userId);
        return quotas.reserveFeature("user:" + userId, "CHAT", QuotaPeriod.DAILY, entitlement.chatPerDay());
    }

    public int reserveMealPlan(Long userId) {
        SubscriptionEntitlement entitlement = effective(userId);
        return quotas.reserveFeature("user:" + userId, "MEAL_PLAN", QuotaPeriod.MONTHLY,
                entitlement.mealPlansPerMonth());
    }

    /** task_be C14. Uses effective() like reserveMealPlan, so what the FE shows matches what generate enforces. */
    public FeatureUsage mealPlanUsage(Long userId) {
        SubscriptionEntitlement entitlement = effective(userId);
        int limit = entitlement.mealPlansPerMonth();
        LocalDate periodStart = QuotaCalendar.today().withDayOfMonth(1);
        int used = quotas.usedOnDate("user:" + userId, "MEAL_PLAN", QuotaPeriod.MONTHLY, periodStart);
        return new FeatureUsage(entitlement.planCode(), limit, used, Math.max(0, limit - used),
                periodStart, periodStart.plusMonths(1));
    }

    public LocalDate reserveVideo(Long userId) {
        SubscriptionEntitlement entitlement = effective(userId);
        LocalDate periodStart = QuotaCalendar.today().withDayOfMonth(1);
        quotas.reserveFeatureOnDate("user:" + userId, "VIDEO", QuotaPeriod.MONTHLY,
                entitlement.videosPerMonth(), periodStart);
        return periodStart;
    }

    public void refundVideo(Long userId) {
        quotas.refundFeature("user:" + userId, "VIDEO", QuotaPeriod.MONTHLY);
    }

    public void refundVideo(Long userId, LocalDate quotaPeriodDate) {
        if (quotaPeriodDate == null) refundVideo(userId);
        else quotas.refundFeature("user:" + userId, "VIDEO", quotaPeriodDate);
    }

    private SubscriptionEntitlement effective(Long userId) {
        try {
            SubscriptionEntitlement entitlement = subscriptions.getEffectiveSubscription(userId);
            if (entitlement == null || entitlement.isExpired(Instant.now())) return SubscriptionEntitlement.free();
            return entitlement;
        } catch (RuntimeException exception) {
            log.warn("Payment Service subscription lookup failed for user {}; applying FREE quotas", userId);
            return SubscriptionEntitlement.free();
        }
    }
}
