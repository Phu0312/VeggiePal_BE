package com.veggiepal.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.payos.*;
import com.veggiepal.payment.exception.PayOsOperationException;
import com.veggiepal.payment.repository.*;
import com.veggiepal.payment.service.PaymentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SubscriptionPlanRepository plans;
    @Autowired PaymentOrderRepository orders;
    @Autowired UserSubscriptionRepository subscriptions;
    @Autowired PaymentService paymentService;
    @MockitoBean PayOsGateway payOs;

    @BeforeEach
    void clean() {
        subscriptions.deleteAll();
        orders.deleteAll();
        reset(payOs);
    }

    @Test
    void plansArePublicAndCheckoutRequiresJwt() throws Exception {
        mvc.perform(get("/payments/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].code").value("FREE"))
                .andExpect(jsonPath("$.result[1].price").value(29000))
                .andExpect(jsonPath("$.result[2].videosPerMonth").value(8));

        mvc.perform(post("/payments/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"PLUS\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutDerivesUserFromJwtAndUsesCatalogPrice() throws Exception {
        when(payOs.createPaymentLink(anyLong(), eq(29_000L), eq("VeggiePal PLUS"), any(), any()))
                .thenReturn(new PaymentLinkResult("new-link", "https://pay.test/new-link", "0002010102123854VIETQR",
                        "VEGGIEPAL JSC", "0123456789", "970422", "VP PLUS", Instant.now().plusSeconds(900)));

        mvc.perform(post("/payments/checkout")
                        .with(jwt().jwt(token -> token.claim("userId", 55L).claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"PLUS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").isNumber())
                .andExpect(jsonPath("$.result.orderId").isNumber())
                .andExpect(jsonPath("$.result.orderCode").isNumber())
                .andExpect(jsonPath("$.result.planCode").value("PLUS"))
                .andExpect(jsonPath("$.result.amount").value(29000))
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.checkoutUrl").value("https://pay.test/new-link"))
                .andExpect(jsonPath("$.result.qrCode").value("0002010102123854VIETQR"))
                .andExpect(jsonPath("$.result.accountName").value("VEGGIEPAL JSC"))
                .andExpect(jsonPath("$.result.accountNumber").value("0123456789"))
                .andExpect(jsonPath("$.result.bin").value("970422"))
                .andExpect(jsonPath("$.result.description").value("VP PLUS"))
                .andExpect(jsonPath("$.result.expiredAt").isNotEmpty());

        PaymentOrder saved = orders.findAllByUserIdOrderByCreatedAtDesc(55L).getFirst();
        assertThat(saved.getAmount()).isEqualTo(29_000L);
        assertThat(saved.getUserId()).isEqualTo(55L);

        mvc.perform(get("/payments/orders/{id}", saved.getId())
                        .with(jwt().jwt(token -> token.claim("userId", 55L).claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(saved.getId()))
                .andExpect(jsonPath("$.result.orderCode").value(saved.getOrderCode()))
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.checkoutUrl").value("https://pay.test/new-link"))
                .andExpect(jsonPath("$.result.qrCode").value("0002010102123854VIETQR"))
                .andExpect(jsonPath("$.result.accountName").value("VEGGIEPAL JSC"));

        mvc.perform(get("/payments/orders/{id}", saved.getId())
                        .with(jwt().jwt(token -> token.claim("userId", 56L).claim("role", "USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelChecksOwnershipCallsPayOsOnceAndIsIdempotent() throws Exception {
        SubscriptionPlan plus = plans.findByCode(PlanCode.PLUS).orElseThrow();
        PaymentOrder order = orders.save(PaymentOrder.builder().orderCode(2222L).userId(70L).plan(plus)
                .amount(29_000).status(PaymentStatus.PENDING).paymentLinkId("cancel-link")
                .qrCode("persisted-emv").checkoutUrl("https://pay.test/cancel-link")
                .createdAt(Instant.now()).expiredAt(Instant.now().plusSeconds(900)).build());
        when(payOs.cancelPaymentLink(2222L, "Cancelled by customer"))
                .thenReturn(PaymentCancellationResult.CANCELLED);

        mvc.perform(post("/payments/orders/{id}/cancel", order.getId()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/payments/orders/{id}/cancel", order.getId())
                        .with(jwt().jwt(token -> token.claim("userId", 71L).claim("role", "USER"))))
                .andExpect(status().isNotFound());

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/payments/orders/{id}/cancel", order.getId())
                            .with(jwt().jwt(token -> token.claim("userId", 70L).claim("role", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.result.qrCode").isEmpty());
        }

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(payOs, times(1)).cancelPaymentLink(2222L, "Cancelled by customer");
    }

    @Test
    void concurrentCheckoutsCreateOnePayOsLinkAndReturnSameOrder() throws Exception {
        when(payOs.createPaymentLink(anyLong(), eq(29_000L), eq("VeggiePal PLUS"), any(), any()))
                .thenReturn(new PaymentLinkResult("one-link", "https://pay.test/one-link", "one-emv",
                        "VEGGIEPAL", "0123456789", "970422", "VP PLUS", Instant.now().plusSeconds(900)));
        int requestCount = 4;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return paymentService.checkout(88L, PlanCode.PLUS).id();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Long> ids = new ArrayList<>();
            for (Future<Long> future : futures) ids.add(future.get(10, TimeUnit.SECONDS));

            assertThat(ids).containsOnly(ids.getFirst());
            assertThat(orders.findAllByUserIdOrderByCreatedAtDesc(88L)).hasSize(1);
            verify(payOs, times(1)).createPaymentLink(anyLong(), anyLong(), anyString(), anyString(), anyString());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void confirmedCancellationWinsAgainstWaitingWebhookWithoutSubscriptionActivation() throws Exception {
        SubscriptionPlan plus = plans.findByCode(PlanCode.PLUS).orElseThrow();
        PaymentOrder order = orders.save(PaymentOrder.builder().orderCode(3333L).userId(90L).plan(plus)
                .amount(29_000).status(PaymentStatus.PENDING).paymentLinkId("race-link")
                .createdAt(Instant.now()).build());
        CountDownLatch cancellationAtProvider = new CountDownLatch(1);
        CountDownLatch allowCancellation = new CountDownLatch(1);
        CountDownLatch webhookVerified = new CountDownLatch(1);
        when(payOs.cancelPaymentLink(3333L, "Cancelled by customer")).thenAnswer(ignored -> {
            cancellationAtProvider.countDown();
            allowCancellation.await(5, TimeUnit.SECONDS);
            return PaymentCancellationResult.CANCELLED;
        });
        when(payOs.verifyWebhook(any())).thenAnswer(ignored -> {
            webhookVerified.countDown();
            return new VerifiedPayment(3333L, 29_000L, "00", "VND", "race-link", "race-ref");
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> cancellation = executor.submit(() -> paymentService.cancel(90L, order.getId()));
            assertThat(cancellationAtProvider.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> webhook = executor.submit(() -> paymentService.processWebhook(java.util.Map.of("signed", true)));
            assertThat(webhookVerified.await(5, TimeUnit.SECONDS)).isTrue();
            allowCancellation.countDown();
            cancellation.get(10, TimeUnit.SECONDS);
            webhook.get(10, TimeUnit.SECONDS);
        } finally {
            allowCancellation.countDown();
            executor.shutdownNow();
        }

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(subscriptions.findByUserId(90L)).isEmpty();
    }

    @Test
    void adminDashboardIsAdminOnlyAndUsesOnlyVerifiedPaidRevenue() throws Exception {
        SubscriptionPlan plus = plans.findByCode(PlanCode.PLUS).orElseThrow();
        SubscriptionPlan pro = plans.findByCode(PlanCode.PRO).orElseThrow();
        Instant now = Instant.parse("2026-10-20T12:00:00Z");
        PaymentOrder paid = orders.save(PaymentOrder.builder().orderCode(1001L).userId(10L).plan(plus)
                .amount(29_000).status(PaymentStatus.PAID).createdAt(now.minusSeconds(60)).paidAt(now)
                .paymentReference("verified-reference").build());
        orders.save(PaymentOrder.builder().orderCode(1002L).userId(11L).plan(pro)
                .amount(59_000).status(PaymentStatus.PENDING).createdAt(now).build());
        orders.save(PaymentOrder.builder().orderCode(1003L).userId(12L).plan(pro)
                .amount(59_000).status(PaymentStatus.PAID).createdAt(now).paidAt(null).build());

        mvc.perform(get("/admin/payments/summary")
                        .with(jwt().jwt(token -> token.claim("userId", 1L).claim("role", "USER"))))
                .andExpect(status().isForbidden());

        var admin = jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(get("/admin/payments/summary").param("planCode", "PLUS").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRevenue").value(29000))
                .andExpect(jsonPath("$.result.totalTransactions").value(1))
                .andExpect(jsonPath("$.result.paidTransactions").value(1));

        mvc.perform(get("/admin/payments/revenue").param("groupBy", "DAY").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRevenue").value(29000))
                .andExpect(jsonPath("$.result.paidTransactions").value(1))
                .andExpect(jsonPath("$.result.points[0].period").value("2026-10-20"));

        mvc.perform(get("/admin/payments/statistics")
                        .param("from", "2026-10-20T00:00:00Z")
                        .param("to", "2026-10-20T23:59:59Z").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.statusDistribution.length()").value(4))
                .andExpect(jsonPath("$.result.planDistribution.length()").value(3))
                .andExpect(jsonPath("$.result.transactionTrend[0].period").value("2026-10-20"));

        mvc.perform(get("/admin/payments/orders").param("status", "PAID").param("page", "0").param("size", "1").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(2));

        mvc.perform(get("/admin/payments/orders/{id}", paid.getId()).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.orderId").value(paid.getId()))
                .andExpect(jsonPath("$.result.userId").value(10))
                .andExpect(jsonPath("$.result.paymentReference").value("verified-reference"));
    }

    @Test
    void verifiedWebhookActivatesSubscriptionAndDuplicateIsIdempotent() throws Exception {
        SubscriptionPlan plus = plans.findByCode(PlanCode.PLUS).orElseThrow();
        PaymentOrder order = orders.save(PaymentOrder.builder().orderCode(987654L).userId(42L).plan(plus)
                .amount(29_000).status(PaymentStatus.PENDING).paymentLinkId("pay-link")
                .checkoutUrl("https://pay.test/pay-link").createdAt(Instant.now()).build());
        when(payOs.verifyWebhook(any())).thenReturn(new VerifiedPayment(987654L, 29_000L, "00", "VND", "pay-link", "ref"));

        mvc.perform(post("/payments/webhook/payos").contentType(MediaType.APPLICATION_JSON).content("{\"signed\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/payments/webhook/payos").contentType(MediaType.APPLICATION_JSON).content("{\"signed\":true}"))
                .andExpect(status().isOk());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        UserSubscription subscription = subscriptions.findByUserId(42L).orElseThrow();
        assertThat(subscription.getPlan().getId()).isEqualTo(plus.getId());
        assertThat(subscription.getExpiresAt()).isAfter(subscription.getStartedAt());

        mvc.perform(get("/subscriptions/me").with(jwt().jwt(token -> token.claim("userId", 42L).claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.planCode").value("PLUS"));
        verify(payOs, times(2)).verifyWebhook(any());
    }

    @Test
    void malformedWebhookReturnsBadRequestInsteadOfGenericServerError() throws Exception {
        when(payOs.verifyWebhook(any())).thenThrow(new PayOsOperationException(new NullPointerException("signature")));

        mvc.perform(post("/payments/webhook/payos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6005));

        assertThat(orders.count()).isZero();
        assertThat(subscriptions.count()).isZero();
    }

    @Test
    void verifiedRegistrationProbeIsAcknowledgedWithoutActivation() throws Exception {
        when(payOs.verifyWebhook(any())).thenReturn(
                new VerifiedPayment(123L, 3_000L, "00", "VND", "probe-link", "probe-reference"));

        mvc.perform(post("/payments/webhook/payos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validSignedProbe\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"));

        assertThat(orders.count()).isZero();
        assertThat(subscriptions.count()).isZero();
    }

    @Test
    void internalSubscriptionRequiresServiceTokenAndReturnsEffectivePlan() throws Exception {
        mvc.perform(get("/internal/subscriptions/77"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/internal/subscriptions/77")
                        .header("X-Internal-Service-Token", "test-internal-service-token-at-least-32-characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.planCode").value("FREE"))
                .andExpect(jsonPath("$.result.chatPerDay").value(5))
                .andExpect(jsonPath("$.result.mealPlansPerMonth").value(2))
                .andExpect(jsonPath("$.result.videosPerMonth").value(0));
    }
}
