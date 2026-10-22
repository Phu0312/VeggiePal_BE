package com.veggiepal.payment.payos;

import java.time.Instant;

public record PaymentLinkResult(String paymentLinkId, String checkoutUrl, String qrCode,
                                String accountName, String accountNumber, String bin,
                                String description, Instant expiredAt) {}
