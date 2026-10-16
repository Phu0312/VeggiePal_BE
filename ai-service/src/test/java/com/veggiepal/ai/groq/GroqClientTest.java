package com.veggiepal.ai.groq;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Duration;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.groq.dto.GroqResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GroqClientTest {
    private MockRestServiceServer mockServer;
    private GroqClient groqClient;
    private GroqProperties properties;
    private ObjectMapper objectMapper;

    private static final String SUCCESS_RESPONSE = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1729000000,
              "model": "openai/gpt-oss-20b",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Ăn chay đủ chất cần kết hợp đậu và hạt."
                  },
                  "logprobs": null,
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "queue_time": 0.04,
                "prompt_tokens": 15,
                "prompt_time": 0.005,
                "completion_tokens": 20,
                "completion_time": 0.01,
                "total_tokens": 35,
                "total_time": 0.015
              },
              "system_fingerprint": "fp_test"
            }
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        properties = new GroqProperties("https://api.groq.com/openai/v1", "gsk-test-key", Duration.ofSeconds(2), Duration.ofSeconds(5), 1);
        
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .messageConverters(items -> items.add(0, new MappingJackson2HttpMessageConverter(objectMapper)));
        
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        groqClient = new GroqClient(restClient, properties);
    }

    @Test
    void complete_SuccessfulResponseWithStandardGroqPayload() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer gsk-test-key"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-20b"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Hello"))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        var result = groqClient.complete("openai/gpt-oss-20b", List.of(new GroqRequest.Message("user", "Hello")), 0.7, 100);

        mockServer.verify();
        assertNotNull(result);
        assertEquals("Ăn chay đủ chất cần kết hợp đậu và hạt.", result.content());
        assertEquals(15, result.promptTokens());
        assertEquals(20, result.completionTokens());
    }

    @Test
    void complete_MissingOrEmptyApiKey_ThrowsGroqNotConfigured() {
        GroqProperties emptyKeyProps = new GroqProperties("https://api.groq.com/openai/v1", "", Duration.ofSeconds(1), Duration.ofSeconds(1), 0);
        GroqClient clientWithoutKey = new GroqClient(RestClient.builder().build(), emptyKeyProps);

        AppException ex = assertThrows(AppException.class, () ->
                clientWithoutKey.complete("openai/gpt-oss-20b", List.of(new GroqRequest.Message("user", "hi")), 0.7, 100));
        assertEquals(ErrorCode.GROQ_NOT_CONFIGURED, ex.getErrorCode());
    }

    @Test
    void complete_Http401Unauthorized_ThrowsGroqUnavailableWithoutRetry() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\":{\"message\":\"Invalid API Key\"}}"));

        AppException ex = assertThrows(AppException.class, () ->
                groqClient.complete("openai/gpt-oss-20b", List.of(new GroqRequest.Message("user", "hi")), 0.7, 100));
        assertEquals(ErrorCode.GROQ_UNAVAILABLE, ex.getErrorCode());
        mockServer.verify();
    }

    @Test
    void complete_Http400BadRequest_ThrowsGroqUnavailableWithoutRetry() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":{\"message\":\"Model not found\"}}"));

        AppException ex = assertThrows(AppException.class, () ->
                groqClient.complete("invalid-model", List.of(new GroqRequest.Message("user", "hi")), 0.7, 100));
        assertEquals(ErrorCode.GROQ_UNAVAILABLE, ex.getErrorCode());
        mockServer.verify();
    }

    @Test
    void complete_Http429RateLimit_RetriesAndSucceeds() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"message\":\"Rate limit exceeded\"}}"));

        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        var result = groqClient.complete("openai/gpt-oss-20b", List.of(new GroqRequest.Message("user", "Hello")), 0.7, 100);

        mockServer.verify();
        assertNotNull(result);
        assertEquals("Ăn chay đủ chất cần kết hợp đậu và hạt.", result.content());
    }

    @Test
    void complete_Http500ServerError_RetriesAndSucceeds() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError().body("{\"error\":{\"message\":\"Internal Server Error\"}}"));

        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        var result = groqClient.complete("openai/gpt-oss-20b", List.of(new GroqRequest.Message("user", "Hello")), 0.7, 100);

        mockServer.verify();
        assertNotNull(result);
        assertEquals("Ăn chay đủ chất cần kết hợp đậu và hạt.", result.content());
    }

    @Test
    void complete_Http429RateLimit_ExhaustsRetries_ThrowsGroqUnavailable() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"message\":\"Rate limit 1\"}}"));

        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"message\":\"Rate limit 2\"}}"));

        AppException ex = assertThrows(AppException.class, () ->
                groqClient.complete("openai/gpt-oss-20b", List.of(new GroqRequest.Message("user", "Hello")), 0.7, 100));

        assertEquals(ErrorCode.GROQ_UNAVAILABLE, ex.getErrorCode());
        mockServer.verify();
    }
}
