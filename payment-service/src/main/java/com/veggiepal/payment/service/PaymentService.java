package com.veggiepal.payment.service;

import com.veggiepal.payment.dto.response.PaymentOrderResponse;
import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.exception.*;
import com.veggiepal.payment.payos.*;
import com.veggiepal.payment.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentOrderRepository orders;
    private final SubscriptionPlanRepository plans;
    private final SubscriptionService subscriptionService;
    private final PayOsGateway payOs;
    private final OrderCodeGenerator orderCodes;
    private final Clock clock;
    private final String returnUrl;
    private final String cancelUrl;

    public PaymentService(PaymentOrderRepository orders, SubscriptionPlanRepository plans,
                          SubscriptionService subscriptionService, PayOsGateway payOs,
                          OrderCodeGenerator orderCodes, Clock clock,
                          @Value("${payos.return-url}") String returnUrl,
                          @Value("${payos.cancel-url}") String cancelUrl) {
        this.orders = orders; this.plans = plans; this.subscriptionService = subscriptionService;
        this.payOs = payOs; this.orderCodes = orderCodes; this.clock = clock;
        this.returnUrl = returnUrl; this.cancelUrl = cancelUrl;
    }

    @Transactional
    public PaymentOrderResponse checkout(Long userId, PlanCode planCode) {
        // The catalog row is a stable database mutex for this plan. It closes the
        // first-checkout race where no user/plan order exists yet, across app instances.
        SubscriptionPlan plan = plans.findByCodeAndActiveTrueForUpdate(planCode)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
        if (plan.getCode() == PlanCode.FREE || plan.getPrice() <= 0) {
            throw new AppException(ErrorCode.PLAN_NOT_PURCHASABLE);
        }
        Optional<PaymentOrder> reusable = orders
                .findAllByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(userId, plan.getId(), PaymentStatus.PENDING)
                .stream().filter(this::isReusable).findFirst();
        if (reusable.isPresent()) return response(reusable.get());

        PaymentOrder order = orders.save(PaymentOrder.builder().orderCode(orderCodes.next()).userId(userId)
                .plan(plan).amount(plan.getPrice()).status(PaymentStatus.PENDING).createdAt(clock.instant()).build());
        try {
            PaymentLinkResult link = payOs.createPaymentLink(order.getOrderCode(), order.getAmount(),
                    "VeggiePal " + plan.getCode(), returnUrl, cancelUrl);
            order.setPaymentLinkId(link.paymentLinkId());
            order.setCheckoutUrl(link.checkoutUrl());
            order.setQrCode(link.qrCode());
            order.setAccountName(link.accountName());
            order.setAccountNumber(link.accountNumber());
            order.setBin(link.bin());
            order.setDescription(link.description());
            order.setExpiredAt(link.expiredAt());
            return response(orders.save(order));
        } catch (PayOsOperationException exception) {
            throw new AppException(ErrorCode.PAYOS_UNAVAILABLE);
        }
    }

    @Transactional
    public PaymentOrderResponse cancel(Long userId, Long orderId) {
        PaymentOrder order = orders.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
        if (order.getStatus() == PaymentStatus.CANCELLED) return response(order);
        if (order.getStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_ORDER_NOT_CANCELLABLE);
        }

        PaymentCancellationResult result;
        try {
            result = payOs.cancelPaymentLink(order.getOrderCode(), "Cancelled by customer");
        } catch (PayOsOperationException exception) {
            throw new AppException(ErrorCode.PAYOS_UNAVAILABLE);
        }
        if (result == PaymentCancellationResult.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ORDER_NOT_CANCELLABLE);
        }
        if (result != PaymentCancellationResult.CANCELLED) {
            throw new AppException(ErrorCode.PAYMENT_CANCELLATION_NOT_CONFIRMED);
        }
        order.setStatus(PaymentStatus.CANCELLED);
        return response(orders.save(order));
    }

    @Transactional(readOnly = true)
    public PaymentOrderResponse getOrder(Long userId, Long orderId) {
        return response(orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> history(Long userId) {
        return orders.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList();
    }

    @Transactional
    public void processWebhook(Object webhookBody) {
        VerifiedPayment payment;
        try { payment = payOs.verifyWebhook(webhookBody); }
        catch (PayOsOperationException exception) { throw new AppException(ErrorCode.PAYOS_WEBHOOK_INVALID); }
        if (payment.orderCode() == null) throw new AppException(ErrorCode.PAYOS_WEBHOOK_INVALID);
        if (!"00".equals(payment.code()) || !"VND".equalsIgnoreCase(payment.currency())) {
            throw new AppException(ErrorCode.PAYMENT_NOT_SUCCESSFUL);
        }
        PaymentOrder order = orders.findByOrderCodeForUpdate(payment.orderCode()).orElse(null);
        // payOS registers a webhook by sending a correctly signed probe whose synthetic
        // order does not exist locally. Verification succeeded, so acknowledge it without
        // changing payment or subscription state.
        if (order == null) return;
        if (order.getStatus() == PaymentStatus.PAID) return;
        if (order.getStatus() == PaymentStatus.CANCELLED) return;
        if (payment.amount() == null || payment.amount() != order.getAmount()) {
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (order.getPaymentLinkId() != null && payment.paymentLinkId() != null
                && !order.getPaymentLinkId().equals(payment.paymentLinkId())) {
            throw new AppException(ErrorCode.PAYOS_WEBHOOK_INVALID);
        }
        Instant paidAt = clock.instant();
        order.setStatus(PaymentStatus.PAID);
        order.setPaidAt(paidAt);
        order.setPaymentReference(payment.reference());
        orders.save(order);
        subscriptionService.activate(order.getUserId(), order.getPlan(), paidAt);
    }

    private PaymentOrderResponse response(PaymentOrder o) {
        boolean qrVisible = o.getStatus() == PaymentStatus.PENDING
                && (o.getExpiredAt() == null || o.getExpiredAt().isAfter(clock.instant()));
        return new PaymentOrderResponse(o.getId(), o.getOrderCode(), o.getPlan().getCode(), o.getAmount(),
                o.getStatus(), o.getCheckoutUrl(), qrVisible ? o.getQrCode() : null,
                o.getAccountName(), o.getAccountNumber(), o.getBin(), o.getDescription(), o.getExpiredAt(),
                o.getCreatedAt(), o.getPaidAt());
    }

    private boolean isReusable(PaymentOrder order) {
        return order.getStatus() == PaymentStatus.PENDING
                && (order.getExpiredAt() == null || order.getExpiredAt().isAfter(clock.instant()))
                && order.getPaymentLinkId() != null && order.getCheckoutUrl() != null && order.getQrCode() != null;
    }
}
