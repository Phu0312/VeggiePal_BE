package com.veggiepal.ai.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.veggiepal.ai.common.AppException;
import org.junit.jupiter.api.Test;

class InternalServiceTokenTest {
    @Test
    void acceptsOnlyExactConfiguredTokenAndFailsClosedWhenUnconfigured() {
        InternalServiceToken configured = new InternalServiceToken("secret-token");
        assertDoesNotThrow(() -> configured.require("secret-token"));
        assertThrows(AppException.class, () -> configured.require("secret-token-x"));
        assertThrows(AppException.class, () -> configured.require(null));

        InternalServiceToken unconfigured = new InternalServiceToken("");
        assertThrows(AppException.class, () -> unconfigured.require(""));
        assertThrows(AppException.class, () -> unconfigured.require(null));
    }
}
