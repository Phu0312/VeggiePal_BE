package com.veggiepal.ai.security;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {
    private CurrentUser() {}
    public static Long id(Jwt jwt) {
        if (jwt == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        Object value = jwt.getClaim("userId");
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String string) {
            try { return Long.valueOf(string); } catch (NumberFormatException ignored) { }
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
