package com.veggiepal.blog.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.sun.net.httpserver.HttpServer;

// A real HTTP server rather than a mocked RestClient: the defect this guards against lives in
// the default message converters, which only run when a response is actually deserialized.
class AiContentModerationServiceTest {

    HttpServer server;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").claim("userId", 7L).issuedAt(Instant.now()).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (server != null) server.stop(0);
    }

    @Test
    void moderate_readsDecisionAndReasonFromAiService() throws Exception {
        String baseUrl = respondWith("{\"code\":1000,\"result\":{\"decision\":\"REJECTED\",\"reason\":\"spam\"}}");
        var service = new AiContentModerationService(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(2));

        ModerationResult result = service.moderate("VIDEO", 2L, 7L, "text");

        assertThat(result).isEqualTo(new ModerationResult(ModerationDecision.REJECTED, "spam"));
    }

    private String respondWith(String json) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ai/moderation/check", exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }
}
