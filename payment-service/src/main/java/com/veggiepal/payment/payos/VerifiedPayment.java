package com.veggiepal.payment.payos;

public record VerifiedPayment(Long orderCode, Long amount, String code, String currency,
                              String paymentLinkId, String reference) {}
