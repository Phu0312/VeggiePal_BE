package com.veggiepal.payment.payos;

import java.time.Instant;

/** qrCode is the complete VietQR EMV text payload returned by payOS, not an encoded image. */
public record PaymentLinkResult(String paymentLinkId, String checkoutUrl, String qrCode,
                                String accountName, String accountNumber, String bin,
                                String description, Instant expiredAt) {}
