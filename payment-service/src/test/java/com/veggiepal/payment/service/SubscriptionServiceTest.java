package com.veggiepal.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.repository.*;
import com.veggiepal.payment.exception.*;
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
    SubscriptionPlan pro;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptions, plans, Clock.fixed(now, ZoneOffset.UTC));
        free = plan(PlanCode.FREE, 0, 5, 2, 0);
        plus = plan(PlanCode.PLUS, 30, 20, 8, 3);
        pro = plan(PlanCode.PRO, 30, 50, 20, 8);
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

    @Test
    void activate_upgradeConvertsTwentyRemainingPlusDaysIntoNineProDays() {
        UserSubscription current = active(plus, now.plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(subscriptions.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));

        service.activate(7L, pro, now);

        assertThat(current.getPlan()).isSameAs(pro);
        assertThat(current.getStartedAt()).isEqualTo(now);
        assertThat(current.getExpiresAt()).isEqualTo(now.plus(39, java.time.temporal.ChronoUnit.DAYS));
    }

    @Test
    void checkoutBlocksDowngradeWhileHigherPlanIsActive() {
        when(subscriptions.findByUserId(7L)).thenReturn(Optional.of(active(pro, now.plusSeconds(3600))));

        assertThatThrownBy(() -> service.preview(7L, plus, now))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAN_DOWNGRADE_NOT_ALLOWED);
    }

    @Test
    void activate_expiredExactlyAtPaymentTimeStartsFreshWithoutBonus() {
        UserSubscription current = active(plus, now);
        when(subscriptions.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));

        service.activate(7L, pro, now);

        assertThat(current.getPlan()).isSameAs(pro);
        assertThat(current.getExpiresAt()).isEqualTo(now.plus(30, java.time.temporal.ChronoUnit.DAYS));
    }

    @Test
    void activate_lateLowerTierPaymentCreditsDaysWithoutDowngrading() {
        Instant expiry = now.plus(20, java.time.temporal.ChronoUnit.DAYS);
        UserSubscription current = active(pro, expiry);
        Instant originalStart = current.getStartedAt();
        when(subscriptions.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));

        service.activate(7L, plus, now);

        assertThat(current.getPlan()).isSameAs(pro);
        assertThat(current.getStartedAt()).isEqualTo(originalStart);
        assertThat(current.getExpiresAt()).isEqualTo(expiry.plus(14, java.time.temporal.ChronoUnit.DAYS));
    }

    @Test
    void previewAndActivationUseTheSameUpgradeCalculation() {
        UserSubscription current = active(plus, now.plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(subscriptions.findByUserId(7L)).thenReturn(Optional.of(current));
        when(subscriptions.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));

        var preview = service.preview(7L, pro, now);
        service.activate(7L, pro, now);

        assertThat(preview.remainingDays()).isEqualTo(20);
        assertThat(preview.bonusDays()).isEqualTo(9);
        assertThat(preview.newExpiresAt()).isEqualTo(current.getExpiresAt());
    }

    private UserSubscription active(SubscriptionPlan plan, Instant expiresAt) {
        return UserSubscription.builder().userId(7L).plan(plan).startedAt(now.minusSeconds(30 * 86400L))
                .expiresAt(expiresAt).updatedAt(now.minusSeconds(1)).build();
    }

    private SubscriptionPlan plan(PlanCode code, int days, int chat, int meals, int videos) {
        return SubscriptionPlan.builder().code(code).price(code == PlanCode.PLUS ? 29_000 : code == PlanCode.PRO ? 59_000 : 0)
                .durationDays(days).chatPerDay(chat)
                .mealPlansPerMonth(meals).videosPerMonth(videos).active(true).build();
    }
}
