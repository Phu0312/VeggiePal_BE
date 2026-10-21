package com.veggiepal.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.exception.*;
import com.veggiepal.payment.payos.*;
import com.veggiepal.payment.repository.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentOrderRepository orders;
    @Mock SubscriptionPlanRepository plans;
    @Mock SubscriptionService subscriptions;
    @Mock PayOsGateway payOs;
    @Mock OrderCodeGenerator codes;
    final Clock clock = Clock.fixed(Instant.parse("2026-10-21T05:00:00Z"), ZoneOffset.UTC);
    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(orders, plans, subscriptions, payOs, codes, clock,
                "https://app.test/success", "https://app.test/cancel");
    }

    @Test
    void checkout_usesCatalogPriceAndConfiguredUrls() {
        SubscriptionPlan plus = plan(1L, PlanCode.PLUS, 29_000);
        when(plans.findByCodeAndActiveTrue(PlanCode.PLUS)).thenReturn(Optional.of(plus));
        when(codes.next()).thenReturn(123L);
        when(orders.save(any())).thenAnswer(i -> { PaymentOrder o = i.getArgument(0); o.setId(9L); return o; });
        when(payOs.createPaymentLink(123L, 29_000L, "VeggiePal PLUS",
                "https://app.test/success", "https://app.test/cancel"))
                .thenReturn(new PaymentLinkResult("link-1", "https://pay.payos.vn/link-1"));

        var result = service.checkout(7L, PlanCode.PLUS);

        assertThat(result.amount()).isEqualTo(29_000);
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.checkoutUrl()).isEqualTo("https://pay.payos.vn/link-1");
    }

    @Test
    void processWebhook_verifiedMatchingPayment_marksPaidAndActivatesOnce() {
        PaymentOrder order = order(PaymentStatus.PENDING);
        when(payOs.verifyWebhook(any())).thenReturn(new VerifiedPayment(123L, 29_000L, "00", "VND", "link-1", "ref-1"));
        when(orders.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(order));

        service.processWebhook(java.util.Map.of());

        assertThat(order.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(clock.instant());
        verify(subscriptions).activate(7L, order.getPlan(), clock.instant());

        service.processWebhook(java.util.Map.of());
        verify(subscriptions, times(1)).activate(anyLong(), any(), any());
    }

    @Test
    void processWebhook_wrongAmountDoesNotActivate() {
        when(payOs.verifyWebhook(any())).thenReturn(new VerifiedPayment(123L, 1L, "00", "VND", "link-1", "ref"));
        when(orders.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(order(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.processWebhook(java.util.Map.of()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        verifyNoInteractions(subscriptions);
    }

    @Test
    void processWebhook_verifiedUnknownOrderAcknowledgesProbeWithoutActivation() {
        when(payOs.verifyWebhook(any())).thenReturn(
                new VerifiedPayment(999L, 3_000L, "00", "VND", "probe-link", "probe-reference"));
        when(orders.findByOrderCodeForUpdate(999L)).thenReturn(Optional.empty());

        assertThatCode(() -> service.processWebhook(java.util.Map.of())).doesNotThrowAnyException();

        verifyNoInteractions(subscriptions);
        verify(orders, never()).save(any());
    }

    @Test
    void checkout_freePlanIsRejected() {
        when(plans.findByCodeAndActiveTrue(PlanCode.FREE)).thenReturn(Optional.of(plan(1L, PlanCode.FREE, 0)));
        assertThatThrownBy(() -> service.checkout(7L, PlanCode.FREE))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.PLAN_NOT_PURCHASABLE);
        verifyNoInteractions(payOs);
    }

    private PaymentOrder order(PaymentStatus status) {
        return PaymentOrder.builder().id(1L).orderCode(123L).userId(7L).plan(plan(2L, PlanCode.PLUS, 29_000))
                .amount(29_000).status(status).paymentLinkId("link-1").createdAt(clock.instant()).build();
    }
    private SubscriptionPlan plan(Long id, PlanCode code, long price) {
        return SubscriptionPlan.builder().id(id).code(code).price(price).durationDays(code == PlanCode.FREE ? 0 : 30)
                .chatPerDay(20).mealPlansPerMonth(8).videosPerMonth(3).active(true).build();
    }
}
