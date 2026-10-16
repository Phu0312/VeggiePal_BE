package com.veggiepal.blog.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AiContentModerationService implements ContentModerationService {

    private final RestClient client;

    public AiContentModerationService(
            @Value("${services.ai.base-url}") String baseUrl,
            @Value("${services.ai.connect-timeout:3s}") Duration connectTimeout,
            @Value("${services.ai.read-timeout:20s}") Duration readTimeout
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public ModerationResult moderate(
            String contentType,
            Long externalContentId,
            Long authorId,
            String text
    ) {
        String token = currentToken();
        try {
            JsonNode response = client.post()
                    .uri("/ai/moderation/check")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of(
                            "contentType", contentType,
                            "externalContentId", externalContentId,
                            "authorId", authorId,
                            "text", text
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode result = response == null ? null : response.path("result");
            if (result == null || result.isMissingNode() || result.path("decision").asText().isBlank()) {
                throw new AppException(ErrorCode.INVALID_MODERATION_RESPONSE);
            }

            ModerationDecision decision;
            try {
                decision = ModerationDecision.valueOf(result.path("decision").asText());
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_MODERATION_RESPONSE);
            }
            return new ModerationResult(decision, result.path("reason").asText(null));
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.MODERATION_UNAVAILABLE);
        }
    }

    private String currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
