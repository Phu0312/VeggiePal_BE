package com.veggiepal.payment.security;

import com.veggiepal.payment.exception.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceAuthenticator {
    private final byte[] expected;

    public InternalServiceAuthenticator(@Value("${internal.service-token:}") String expected) {
        this.expected = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
    }

    public void requireValid(String supplied) {
        byte[] actual = supplied == null ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
