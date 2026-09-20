package com.veggiepal.blog.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;

public final class CurrentUser {

    private static final String USER_ID_CLAIM = "userId";

    private static final String ROLE_CLAIM = "role";

    private static final String ADMIN_ROLE = "ADMIN";

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {

        Object userId = jwt.getClaim(USER_ID_CLAIM);

        if (userId instanceof Number number) {
            return number.longValue();
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    public static boolean isAdmin(Jwt jwt) {

        return ADMIN_ROLE.equals(jwt.getClaimAsString(ROLE_CLAIM));
    }
}
