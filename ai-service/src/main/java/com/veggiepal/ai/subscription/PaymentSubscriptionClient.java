package com.veggiepal.ai.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentSubscriptionClient implements SubscriptionProvider {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";
    private final RestClient http;
    private final ObjectMapper mapper;
    private final String paymentUrl;
    private final String serviceToken;

    public PaymentSubscriptionClient(@Qualifier("serviceRestClient") RestClient http, ObjectMapper mapper,
                                     @Value("${services.payment.base-url}") String paymentUrl,
                                     @Value("${services.internal-token}") String serviceToken) {
        this.http = http; this.mapper = mapper; this.paymentUrl = paymentUrl; this.serviceToken = serviceToken;
    }

    @Override
    public SubscriptionEntitlement getEffectiveSubscription(Long userId) {
        if (serviceToken == null || serviceToken.isBlank()) {
            throw new IllegalStateException("Internal service token is not configured");
        }
        Map<String, Object> envelope = http.get()
                .uri(paymentUrl + "/internal/subscriptions/{userId}", userId)
                .header(INTERNAL_TOKEN_HEADER, serviceToken)
                .retrieve().body(Map.class);
        Object result = envelope == null ? null : envelope.get("result");
        if (result == null) throw new IllegalStateException("Payment Service returned no subscription");
        return mapper.convertValue(result, new TypeReference<>() {});
    }
}
