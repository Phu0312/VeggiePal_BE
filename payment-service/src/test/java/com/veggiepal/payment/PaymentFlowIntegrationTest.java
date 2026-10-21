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
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SubscriptionPlanRepository plans;
    @Autowired PaymentOrderRepository orders;
    @Autowired UserSubscriptionRepository subscriptions;
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
                .thenReturn(new PaymentLinkResult("new-link", "https://pay.test/new-link"));

        mvc.perform(post("/payments/checkout")
                        .with(jwt().jwt(token -> token.claim("userId", 55L).claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"PLUS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(29000))
                .andExpect(jsonPath("$.result.checkoutUrl").value("https://pay.test/new-link"));

        PaymentOrder saved = orders.findAllByUserIdOrderByCreatedAtDesc(55L).getFirst();
        assertThat(saved.getAmount()).isEqualTo(29_000L);
        assertThat(saved.getUserId()).isEqualTo(55L);
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
