package com.veggiepal.payment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_REQUEST(1018, "Invalid request", HttpStatus.BAD_REQUEST),
    PLAN_REQUIRED(6000, "Subscription plan is required", HttpStatus.BAD_REQUEST),
    PLAN_NOT_FOUND(6001, "Subscription plan does not exist", HttpStatus.NOT_FOUND),
    PLAN_NOT_PURCHASABLE(6002, "This subscription plan cannot be purchased", HttpStatus.BAD_REQUEST),
    PAYMENT_ORDER_NOT_FOUND(6003, "Payment order does not exist", HttpStatus.NOT_FOUND),
    PAYOS_UNAVAILABLE(6004, "Payment provider is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    PAYOS_WEBHOOK_INVALID(6005, "Invalid payOS webhook signature or data", HttpStatus.BAD_REQUEST),
    PAYMENT_AMOUNT_MISMATCH(6006, "Payment amount does not match the order", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_SUCCESSFUL(6007, "Payment was not successful", HttpStatus.BAD_REQUEST),
    PAYMENT_ORDER_NOT_CANCELLABLE(6008, "Only pending payment orders can be cancelled", HttpStatus.CONFLICT),
    PAYMENT_CANCELLATION_NOT_CONFIRMED(6009, "Payment provider did not confirm cancellation", HttpStatus.CONFLICT);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code; this.message = message; this.statusCode = statusCode;
    }
    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
