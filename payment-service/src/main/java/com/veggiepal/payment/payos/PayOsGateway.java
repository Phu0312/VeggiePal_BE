package com.veggiepal.payment.payos;

public interface PayOsGateway {
    PaymentLinkResult createPaymentLink(long orderCode, long amount, String description,
                                        String returnUrl, String cancelUrl);
    PaymentCancellationResult cancelPaymentLink(long orderCode, String reason);
    VerifiedPayment verifyWebhook(Object webhookBody);
}
