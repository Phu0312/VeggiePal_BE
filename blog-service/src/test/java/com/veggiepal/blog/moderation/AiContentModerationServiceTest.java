package com.veggiepal.blog.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// A real HTTP server rather than a mocked RestClient: the defect this guards against lives in
// the default message converters, which only run when a response is actually deserialized.
class AiContentModerationServiceTest {

    HttpServer server;
    final AtomicReference<String> receivedToken = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    // No SecurityContext on purpose: blog moderation runs on a background thread with no request
    @Test
    void moderate_sendsTheInternalTokenAndReadsDecisionAndReason() throws Exception {
        String baseUrl = respondWith(200, "{\"code\":1000,\"result\":{\"decision\":\"REJECTED\",\"reason\":\"spam\"}}");
        var service = new AiContentModerationService(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(2), "secret-token");

        ModerationResult result = service.moderate("BLOG", 2L, 7L, "text");

        assertThat(result).isEqualTo(new ModerationResult(ModerationDecision.REJECTED, "spam"));
        assertThat(receivedToken.get()).isEqualTo("secret-token");
    }

    @Test
    void moderate_rejectedToken_isReportedAsUnavailable() throws Exception {
        String baseUrl = respondWith(401, "{\"code\":6001,\"message\":\"Unauthenticated\"}");
        var service = new AiContentModerationService(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(2), "wrong");

        assertThatThrownBy(() -> service.moderate("BLOG", 2L, 7L, "text"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODERATION_UNAVAILABLE);
    }

    private String respondWith(int status, String json) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/internal/moderation/check", exchange -> {
            receivedToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Token"));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }
}
