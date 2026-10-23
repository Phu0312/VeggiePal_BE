package com.veggiepal.payment.payos;

import com.veggiepal.payment.exception.PayOsOperationException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

@Component
public class PayOsSdkAdapter implements PayOsGateway {
    private final String clientId;
    private final String apiKey;
    private final String checksumKey;

    public PayOsSdkAdapter(@Value("${payos.client-id}") String clientId,
                           @Value("${payos.api-key}") String apiKey,
                           @Value("${payos.checksum-key}") String checksumKey) {
        this.clientId = clientId; this.apiKey = apiKey; this.checksumKey = checksumKey;
    }

    @Override
    public PaymentCancellationResult cancelPaymentLink(long orderCode, String reason) {
        PayOS payOS = null;
        try {
            payOS = client();
            var response = payOS.paymentRequests().cancel(orderCode, reason);
            if (response.getStatus() == PaymentLinkStatus.CANCELLED) return PaymentCancellationResult.CANCELLED;
            if (response.getStatus() == PaymentLinkStatus.PAID) return PaymentCancellationResult.PAID;
            return PaymentCancellationResult.NOT_CANCELLED;
        } catch (PayOSException | IllegalArgumentException exception) {
            throw new PayOsOperationException(exception);
        } finally {
            if (payOS != null) payOS.close();
        }
    }

    @Override
    public PaymentLinkResult createPaymentLink(long orderCode, long amount, String description,
                                               String returnUrl, String cancelUrl) {
        PayOS payOS = null;
        try {
            payOS = client();
            var response = payOS.paymentRequests().create(CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode).amount(amount).description(description)
                    .returnUrl(returnUrl).cancelUrl(cancelUrl).build());
            Instant expiredAt = response.getExpiredAt() == null ? null
                    : Instant.ofEpochSecond(response.getExpiredAt());
            return new PaymentLinkResult(response.getPaymentLinkId(), response.getCheckoutUrl(), response.getQrCode(),
                    response.getAccountName(), response.getAccountNumber(), response.getBin(),
                    response.getDescription(), expiredAt);
        } catch (PayOSException | IllegalArgumentException exception) {
            throw new PayOsOperationException(exception);
        } finally {
            if (payOS != null) payOS.close();
        }
    }

    @Override
    public VerifiedPayment verifyWebhook(Object webhookBody) {
        PayOS payOS = null;
        try {
            payOS = client();
            var data = payOS.webhooks().verify(webhookBody);
            return new VerifiedPayment(data.getOrderCode(), data.getAmount(), data.getCode(), data.getCurrency(),
                    data.getPaymentLinkId(), data.getReference());
        } catch (RuntimeException exception) {
            throw new PayOsOperationException(exception);
        } finally {
            if (payOS != null) payOS.close();
        }
    }

    private PayOS client() { return new PayOS(clientId, apiKey, checksumKey); }
}
