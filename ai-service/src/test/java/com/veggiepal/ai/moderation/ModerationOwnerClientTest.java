package com.veggiepal.ai.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class ModerationOwnerClientTest {

    private RestClient http;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private ModerationOwnerClient client;

    @BeforeEach
    void setUp() {
        http = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(http.put()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        client = new ModerationOwnerClient(http, "http://blog-service");
    }

    @Test
    void applyThrowsInvalidRequestWhenIdOrAuthorIsNull() {
        AppException ex1 = assertThrows(AppException.class, () ->
                client.apply("BLOG", null, 1L, ModerationDtos.Decision.APPROVED, "token"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex1.getErrorCode());

        AppException ex2 = assertThrows(AppException.class, () ->
                client.apply("BLOG", 10L, null, ModerationDtos.Decision.APPROVED, "token"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex2.getErrorCode());
    }

    @Test
    void applyThrowsDownstreamUnavailableWhenContentTypeIsUnsupported() {
        AppException ex = assertThrows(AppException.class, () ->
                client.apply("VIDEO", 10L, 1L, ModerationDtos.Decision.APPROVED, "token"));
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    void applySendsPutRequestToBlogService() {
        client.apply("BLOG", 42L, 7L, ModerationDtos.Decision.APPROVED, "admin-token");

        verify(uriSpec).uri("http://blog-service/internal/moderation/{type}/{id}", "BLOG", 42L);
        verify(bodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token");
        verify(bodySpec).body(Map.of("decision", "APPROVED", "authorId", 7L));
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void applyThrowsDownstreamUnavailableWhenCallFails() {
        when(responseSpec.toBodilessEntity()).thenThrow(new RestClientException("Downstream timeout"));

        AppException ex = assertThrows(AppException.class, () ->
                client.apply("BLOG", 42L, 7L, ModerationDtos.Decision.REJECTED, "admin-token"));
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, ex.getErrorCode());
    }
}
