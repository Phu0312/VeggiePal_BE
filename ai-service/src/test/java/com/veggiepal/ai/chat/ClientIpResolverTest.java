package com.veggiepal.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void directUntrustedCaller_ignoresSpoofedXForwardedFor() {
        ClientIpResolver resolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.55");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader("X-Real-IP", "5.6.7.8");

        String ip = resolver.resolve(request);

        assertEquals("198.51.100.55", ip, "Direct untrusted caller must not be able to spoof IP via headers");
    }

    @Test
    void trustedGateway_acceptsForwardedClientIp() {
        ClientIpResolver resolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 127.0.0.1");

        String ip = resolver.resolve(request);

        assertEquals("203.0.113.10", ip, "Trusted gateway forwarded IP must be accepted");
    }

    @Test
    void trustedGateway_acceptsRealIpHeader() {
        ClientIpResolver resolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        request.addHeader("X-Real-IP", "203.0.113.10");

        String ip = resolver.resolve(request);

        assertEquals("203.0.113.10", ip, "Trusted gateway X-Real-IP must be accepted");
    }

    @Test
    void customTrustedProxy_isRespected() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.10");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "192.168.1.100");

        String ip = resolver.resolve(request);

        assertEquals("192.168.1.100", ip);
    }
}
