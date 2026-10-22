package com.veggiepal.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.exception.*;
import com.veggiepal.payment.payos.*;
import com.veggiepal.payment.repository.*;
import java.time.*;
import java.util.List;
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
        when(plans.findByCodeAndActiveTrueForUpdate(PlanCode.PLUS)).thenReturn(Optional.of(plus));
        when(orders.findAllByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(7L, 1L, PaymentStatus.PENDING))
                .thenReturn(List.of());
        when(codes.next()).thenReturn(123L);
        when(orders.save(any())).thenAnswer(i -> { PaymentOrder o = i.getArgument(0); o.setId(9L); return o; });
        when(payOs.createPaymentLink(123L, 29_000L, "VeggiePal PLUS",
                "https://app.test/success", "https://app.test/cancel"))
                .thenReturn(link("0002010102123854VIETQR", clock.instant().plusSeconds(900)));

        var result = service.checkout(7L, PlanCode.PLUS);

        assertThat(result.amount()).isEqualTo(29_000);
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.checkoutUrl()).isEqualTo("https://pay.payos.vn/link-1");
        assertThat(result.qrCode()).isEqualTo("0002010102123854VIETQR");
        assertThat(result.accountName()).isEqualTo("VEGGIEPAL");
        assertThat(result.accountNumber()).isEqualTo("123456789");
        assertThat(result.bin()).isEqualTo("970422");
        assertThat(result.description()).isEqualTo("VP PLUS");
        assertThat(result.expiredAt()).isEqualTo(clock.instant().plusSeconds(900));

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(orders, times(2)).save(captor.capture());
        PaymentOrder saved = captor.getAllValues().getLast();
        assertThat(saved.getQrCode()).isEqualTo("0002010102123854VIETQR");
        assertThat(saved.getAccountName()).isEqualTo("VEGGIEPAL");
    }

    @Test
    void checkout_missingQrCodeRetainsCheckoutUrlFallback() {
        SubscriptionPlan plus = plan(1L, PlanCode.PLUS, 29_000);
        when(plans.findByCodeAndActiveTrueForUpdate(PlanCode.PLUS)).thenReturn(Optional.of(plus));
        when(orders.findAllByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(7L, 1L, PaymentStatus.PENDING))
                .thenReturn(List.of());
        when(codes.next()).thenReturn(123L);
        when(orders.save(any())).thenAnswer(i -> i.getArgument(0));
        when(payOs.createPaymentLink(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(link(null, null));

        var result = service.checkout(7L, PlanCode.PLUS);

        assertThat(result.qrCode()).isNull();
        assertThat(result.checkoutUrl()).isEqualTo("https://pay.payos.vn/link-1");
    }

    @Test
    void getOrder_restoresActiveQrButHidesExpiredOrPaidQr() {
        PaymentOrder order = order(PaymentStatus.PENDING);
        order.setQrCode("persisted-qr");
        order.setExpiredAt(clock.instant().plusSeconds(60));
        when(orders.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(order));

        assertThat(service.getOrder(7L, 1L).qrCode()).isEqualTo("persisted-qr");

        order.setExpiredAt(clock.instant());
        assertThat(service.getOrder(7L, 1L).qrCode()).isNull();
        order.setExpiredAt(clock.instant().plusSeconds(60));
        order.setStatus(PaymentStatus.PAID);
        assertThat(service.getOrder(7L, 1L).qrCode()).isNull();
    }

    @Test
    void checkout_reusesValidPendingOrderWithoutCallingPayOs() {
        SubscriptionPlan plus = plan(1L, PlanCode.PLUS, 29_000);
        PaymentOrder existing = order(PaymentStatus.PENDING);
        existing.setPlan(plus);
        existing.setQrCode("persisted-emv");
        existing.setCheckoutUrl("https://pay.test/existing");
        existing.setExpiredAt(clock.instant().plusSeconds(60));
        when(plans.findByCodeAndActiveTrueForUpdate(PlanCode.PLUS)).thenReturn(Optional.of(plus));
        when(orders.findAllByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(7L, 1L, PaymentStatus.PENDING))
                .thenReturn(List.of(existing));

        var result = service.checkout(7L, PlanCode.PLUS);

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.qrCode()).isEqualTo("persisted-emv");
        assertThat(result.checkoutUrl()).isEqualTo("https://pay.test/existing");
        verifyNoInteractions(payOs, codes);
        verify(orders, never()).save(any());
    }

    @Test
    void checkout_doesNotReuseExpiredCancelledOrPaidOrders() {
        SubscriptionPlan plus = plan(1L, PlanCode.PLUS, 29_000);
        PaymentOrder expired = order(PaymentStatus.PENDING);
        expired.setPlan(plus);
        expired.setQrCode("expired");
        expired.setCheckoutUrl("https://pay.test/expired");
        expired.setExpiredAt(clock.instant());
        PaymentOrder cancelled = order(PaymentStatus.CANCELLED);
        PaymentOrder paid = order(PaymentStatus.PAID);
        when(plans.findByCodeAndActiveTrueForUpdate(PlanCode.PLUS)).thenReturn(Optional.of(plus));
        when(orders.findAllByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(7L, 1L, PaymentStatus.PENDING))
                .thenReturn(List.of(expired, cancelled, paid));
        when(codes.next()).thenReturn(456L);
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payOs.createPaymentLink(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(link("new-emv", clock.instant().plusSeconds(300)));

        var result = service.checkout(7L, PlanCode.PLUS);

        assertThat(result.orderCode()).isEqualTo(456L);
        assertThat(result.qrCode()).isEqualTo("new-emv");
        verify(payOs).createPaymentLink(anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void cancel_requiresPendingAndIsIdempotentAfterConfirmation() {
        PaymentOrder pending = order(PaymentStatus.PENDING);
        when(orders.findByIdAndUserIdForUpdate(1L, 7L)).thenReturn(Optional.of(pending));
        when(payOs.cancelPaymentLink(123L, "Cancelled by customer"))
                .thenReturn(PaymentCancellationResult.CANCELLED);
        when(orders.save(pending)).thenReturn(pending);

        assertThat(service.cancel(7L, 1L).status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(service.cancel(7L, 1L).status()).isEqualTo(PaymentStatus.CANCELLED);

        verify(payOs, times(1)).cancelPaymentLink(anyLong(), anyString());
        verify(orders, times(1)).save(pending);
    }

    @Test
    void cancel_rejectsPaidOrderWithoutCallingPayOs() {
        when(orders.findByIdAndUserIdForUpdate(1L, 7L)).thenReturn(Optional.of(order(PaymentStatus.PAID)));

        assertThatThrownBy(() -> service.cancel(7L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ORDER_NOT_CANCELLABLE);
        verifyNoInteractions(payOs);
    }

    @Test
    void cancel_doesNotChangeLocalStatusWithoutProviderConfirmation() {
        PaymentOrder pending = order(PaymentStatus.PENDING);
        when(orders.findByIdAndUserIdForUpdate(1L, 7L)).thenReturn(Optional.of(pending));
        when(payOs.cancelPaymentLink(123L, "Cancelled by customer"))
                .thenReturn(PaymentCancellationResult.NOT_CANCELLED);

        assertThatThrownBy(() -> service.cancel(7L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_CANCELLATION_NOT_CONFIRMED);
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(orders, never()).save(any());
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
    void processWebhook_doesNotActivateCancelledOrder() {
        PaymentOrder cancelled = order(PaymentStatus.CANCELLED);
        when(payOs.verifyWebhook(any())).thenReturn(new VerifiedPayment(123L, 29_000L, "00", "VND", "link-1", "ref"));
        when(orders.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(cancelled));

        service.processWebhook(java.util.Map.of());

        assertThat(cancelled.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verifyNoInteractions(subscriptions);
        verify(orders, never()).save(any());
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
        when(plans.findByCodeAndActiveTrueForUpdate(PlanCode.FREE)).thenReturn(Optional.of(plan(1L, PlanCode.FREE, 0)));
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
    private PaymentLinkResult link(String qrCode, Instant expiredAt) {
        return new PaymentLinkResult("link-1", "https://pay.payos.vn/link-1", qrCode,
                "VEGGIEPAL", "123456789", "970422", "VP PLUS", expiredAt);
    }
}
