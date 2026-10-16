package com.veggiepal.ai.moderation;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ModerationOwnerClient {

    // Content types whose source of truth is blog-service
    private static final Set<String> BLOG_SERVICE_TYPES = Set.of("BLOG", "VIDEO", "COMMENT");

    private final RestClient http;
    private final String blogUrl;

    public ModerationOwnerClient(
            @Qualifier("serviceRestClient") RestClient http,
            @Value("${services.blog.base-url}") String blogUrl
    ) {
        this.http = http;
        this.blogUrl = blogUrl;
    }

    public void apply(
            String contentType,
            Long externalContentId,
            Long authorId,
            ModerationDtos.Decision decision,
            String bearerToken
    ) {
        if (externalContentId == null || authorId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (!BLOG_SERVICE_TYPES.contains(contentType)) {
            // Never report a synchronized decision when there is nowhere safe to apply it
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        }
        try {
            http.put()
                    .uri(blogUrl + "/internal/moderation/{type}/{id}", contentType, externalContentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .body(Map.of("decision", decision.name(), "authorId", authorId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE, exception);
        }
    }
}
