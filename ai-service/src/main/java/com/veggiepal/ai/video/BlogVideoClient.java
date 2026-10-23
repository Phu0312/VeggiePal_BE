package com.veggiepal.ai.video;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BlogVideoClient {
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient http;
    private final String blogUrl;
    private final String token;

    public BlogVideoClient(@Qualifier("serviceRestClient") RestClient http,
                           @Value("${services.blog.base-url}") String blogUrl,
                           @Value("${services.internal-token:}") String token) {
        this.http = http;
        this.blogUrl = blogUrl;
        this.token = token;
    }

    public BlogVideoMetadata get(Long videoId) {
        if (token == null || token.isBlank()) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        try {
            Envelope response = http.get()
                    .uri(blogUrl + "/internal/videos/{id}", videoId)
                    .header(INTERNAL_TOKEN_HEADER, token)
                    .retrieve()
                    .body(Envelope.class);
            if (response == null || response.result() == null) {
                throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
            }
            return response.result();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, exception);
            }
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE, exception);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE, exception);
        }
    }

    record Envelope(int code, String message, BlogVideoMetadata result) { }
    public record BlogVideoMetadata(Long id, String status, String sourceType, String videoUrl) { }
}
