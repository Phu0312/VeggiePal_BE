package com.veggiepal.ai.quota;

import static org.mockito.Mockito.*;

import com.veggiepal.ai.subscription.*;
import java.time.Instant;
import org.junit.jupiter.api.*;

class PremiumQuotaServiceTest {
    private SubscriptionProvider subscriptions;
    private QuotaService quotas;
    private PremiumQuotaService service;

    @BeforeEach
    void setUp() {
        subscriptions = mock(SubscriptionProvider.class);
        quotas = mock(QuotaService.class);
        service = new PremiumQuotaService(subscriptions, quotas);
    }

    @Test void freePlanUsesFreeLimits() { assertLimits(new SubscriptionEntitlement("FREE", null, 5, 2, 0), 5, 2, 0); }
    @Test void plusPlanUsesPlusLimits() { assertLimits(new SubscriptionEntitlement("PLUS", Instant.now().plusSeconds(3600), 20, 8, 3), 20, 8, 3); }
    @Test void proPlanUsesProLimits() { assertLimits(new SubscriptionEntitlement("PRO", Instant.now().plusSeconds(3600), 50, 20, 8), 50, 20, 8); }

    @Test
    void expiredSubscriptionFallsBackToFreeLimits() {
        when(subscriptions.getEffectiveSubscription(7L)).thenReturn(
                new SubscriptionEntitlement("PRO", Instant.now().minusSeconds(1), 50, 20, 8));
        service.reserveChat(7L);
        verify(quotas).reserveFeature("user:7", "CHAT", QuotaPeriod.DAILY, 5);
    }

    @Test
    void paymentServiceFailureFallsBackToFreeLimits() {
        when(subscriptions.getEffectiveSubscription(7L)).thenThrow(new RuntimeException("payment unavailable"));
        service.reserveMealPlan(7L);
        verify(quotas).reserveFeature("user:7", "MEAL_PLAN", QuotaPeriod.MONTHLY, 2);
    }

    private void assertLimits(SubscriptionEntitlement entitlement, int chat, int meals, int videos) {
        when(subscriptions.getEffectiveSubscription(7L)).thenReturn(entitlement);
        service.reserveChat(7L);
        service.reserveMealPlan(7L);
        service.reserveVideo(7L);
        verify(quotas).reserveFeature("user:7", "CHAT", QuotaPeriod.DAILY, chat);
        verify(quotas).reserveFeature("user:7", "MEAL_PLAN", QuotaPeriod.MONTHLY, meals);
        verify(quotas).reserveFeature("user:7", "VIDEO", QuotaPeriod.MONTHLY, videos);
    }
}
