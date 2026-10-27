package com.veggiepal.ai.security;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Copy of blog-service's class: checks the X-Internal-Service-Token header on service-to-service calls. */
@Component
public class InternalServiceToken {
    private final byte[] configured;

    public InternalServiceToken(@Value("${services.internal-token:}") String token) {
        configured = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    public void require(String supplied) {
        byte[] candidate = supplied == null ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        if (configured.length == 0 || !MessageDigest.isEqual(configured, candidate)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
