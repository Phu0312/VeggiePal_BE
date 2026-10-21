package com.veggiepal.payment.controller;

import com.veggiepal.payment.exception.*;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {
    private CurrentUser() {}
    public static Long id(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) return number.longValue();
        if (claim instanceof String value) {
            try { return Long.valueOf(value); } catch (NumberFormatException ignored) {}
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
