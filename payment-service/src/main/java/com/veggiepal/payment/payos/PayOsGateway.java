package com.veggiepal.payment.payos;

public interface PayOsGateway {
    PaymentLinkResult createPaymentLink(long orderCode, long amount, String description,
                                        String returnUrl, String cancelUrl);
    VerifiedPayment verifyWebhook(Object webhookBody);
}
