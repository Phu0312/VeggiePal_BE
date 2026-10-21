package com.veggiepal.payment.payos;

public record PaymentLinkResult(String paymentLinkId, String checkoutUrl, String qrCode) {}
