package com.veggiepal.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.repository.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    @Mock UserSubscriptionRepository subscriptions;
    @Mock SubscriptionPlanRepository plans;
    final Instant now = Instant.parse("2026-10-21T05:00:00Z");
    SubscriptionService service;
    SubscriptionPlan free;
    SubscriptionPlan plus;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptions, plans, Clock.fixed(now, ZoneOffset.UTC));
        free = plan(PlanCode.FREE, 0, 5, 2, 0);
        plus = plan(PlanCode.PLUS, 30, 20, 8, 3);
    }

    @Test
    void getForUser_expiredPaidSubscriptionFallsBackToFree() {
        UserSubscription expired = UserSubscription.builder().userId(7L).plan(plus)
                .startedAt(now.minusSeconds(3_000_000)).expiresAt(now).updatedAt(now.minusSeconds(1)).build();
        when(subscriptions.findByUserIdForUpdate(7L)).thenReturn(Optional.of(expired));
        when(plans.findByCodeAndActiveTrue(PlanCode.FREE)).thenReturn(Optional.of(free));

        var result = service.getForUser(7L);

        assertThat(result.planCode()).isEqualTo(PlanCode.FREE);
        assertThat(result.expiresAt()).isNull();
        assertThat(result.chatPerDay()).isEqualTo(5);
    }

    @Test
    void activate_sameActivePlanRenewsFromCurrentExpiration() {
        Instant existingExpiry = now.plusSeconds(3600);
        UserSubscription current = UserSubscription.builder().userId(7L).plan(plus).startedAt(now.minusSeconds(10))
                .expiresAt(existingExpiry).updatedAt(now.minusSeconds(10)).build();
        when(subscriptions.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));

        service.activate(7L, plus, now);

        assertThat(current.getStartedAt()).isEqualTo(now.minusSeconds(10));
        assertThat(current.getExpiresAt()).isEqualTo(existingExpiry.plus(30, java.time.temporal.ChronoUnit.DAYS));
    }

    private SubscriptionPlan plan(PlanCode code, int days, int chat, int meals, int videos) {
        return SubscriptionPlan.builder().code(code).durationDays(days).chatPerDay(chat)
                .mealPlansPerMonth(meals).videosPerMonth(videos).active(true).build();
    }
}
