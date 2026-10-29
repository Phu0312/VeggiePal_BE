package com.veggiepal.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "cors.allowed-origin-patterns=https://veggiepal.id.vn, https://www.veggiepal.id.vn, ")
class CorsIntegrationTests {

    static final HttpServer paymentBackend = startPaymentBackend();

    @Value("${local.server.port}") int port;
    HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void paymentBackendUrl(DynamicPropertyRegistry registry) {
        registry.add("services.payment.url", () -> "http://127.0.0.1:" + paymentBackend.getAddress().getPort());
    }

    @AfterAll
    static void stopPaymentBackend() {
        paymentBackend.stop(0);
    }

    @Test
    void configuredOriginsAllowGetPreflight() throws Exception {
        for (String origin : new String[] {"https://veggiepal.id.vn", "https://www.veggiepal.id.vn"}) {
            HttpResponse<String> response = preflight("/api/payments/plans", origin, "GET", null);
            assertThat(response.statusCode()).isIn(200, 204);
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue(origin);
            assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
            assertThat(response.headers().firstValue("Access-Control-Allow-Methods").orElse(""))
                    .contains("GET");
        }
    }

    @Test
    void configuredOriginAllowsPostWithAuthorizationAndContentType() throws Exception {
        HttpResponse<String> response = preflight("/api/payments/checkout", "https://veggiepal.id.vn",
                "POST", "authorization,content-type");
        assertThat(response.statusCode()).isIn(200, 204);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue("https://veggiepal.id.vn");
        assertThat(response.headers().firstValue("Access-Control-Allow-Headers").orElse(""))
                .containsIgnoringCase("authorization")
                .containsIgnoringCase("content-type");
    }

    @Test
    void unlistedOriginIsRejected() throws Exception {
        HttpResponse<String> response = preflight("/api/payments/plans", "https://unlisted.example", "GET", null);
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    @Test
    void ordinaryRequestStillUsesNormalHandler() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void preflightDoesNotBypassDownstreamAuthenticationForActualPost() throws Exception {
        HttpResponse<String> preflight = preflight("/api/payments/checkout", "https://veggiepal.id.vn",
                "POST", "authorization,content-type");
        assertThat(preflight.statusCode()).isIn(200, 204);

        HttpRequest post = HttpRequest.newBuilder(uri("/api/payments/checkout"))
                .header("Origin", "https://veggiepal.id.vn")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue("https://veggiepal.id.vn");
    }

    private HttpResponse<String> preflight(String path, String origin, String method, String headers)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", method);
        if (headers != null) {
            request.header("Access-Control-Request-Headers", headers);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static HttpServer startPaymentBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/payments/checkout", exchange -> {
                int status = exchange.getRequestHeaders().containsKey("Authorization") ? 200 : 401;
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not start payment test backend", ex);
        }
    }
}
