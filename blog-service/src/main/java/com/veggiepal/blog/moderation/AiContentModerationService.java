package com.veggiepal.blog.moderation;

import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
// Jackson 3: Spring Boot 4's RestClient cannot deserialize into Jackson 2's com.fasterxml JsonNode.
import tools.jackson.databind.JsonNode;

@Service
public class AiContentModerationService implements ContentModerationService {

    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient client;
    private final String internalToken;

    public AiContentModerationService(
            @Value("${services.ai.base-url}") String baseUrl,
            @Value("${services.ai.connect-timeout:3s}") Duration connectTimeout,
            @Value("${services.ai.read-timeout:20s}") Duration readTimeout,
            @Value("${services.internal-token:}") String internalToken
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.internalToken = internalToken;
    }

    // The service token, not the caller's JWT: blog moderation runs on a background thread with
    // no request behind it, and moderation is a platform cost that must not eat the user's AI quota.
    @Override
    public ModerationResult moderate(
            String contentType,
            Long externalContentId,
            Long authorId,
            String text
    ) {
        try {
            JsonNode response = client.post()
                    .uri("/internal/moderation/check")
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .body(Map.of(
                            "contentType", contentType,
                            "externalContentId", externalContentId,
                            "authorId", authorId,
                            "text", text
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode result = response == null ? null : response.path("result");
            if (result == null || result.isMissingNode() || result.path("decision").asString().isBlank()) {
                throw new AppException(ErrorCode.INVALID_MODERATION_RESPONSE);
            }

            ModerationDecision decision;
            try {
                decision = ModerationDecision.valueOf(result.path("decision").asString());
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_MODERATION_RESPONSE);
            }
            return new ModerationResult(decision, result.path("reason").asString(null));
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.MODERATION_UNAVAILABLE);
        }
    }
}
