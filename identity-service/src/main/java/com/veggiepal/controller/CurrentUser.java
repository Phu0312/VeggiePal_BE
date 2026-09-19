package com.veggiepal.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

public final class CurrentUser {

    private static final String USER_ID_CLAIM = "userId";

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {

        Object userId = jwt.getClaim(USER_ID_CLAIM);

        if (userId instanceof Number number) {
            return number.longValue();
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
