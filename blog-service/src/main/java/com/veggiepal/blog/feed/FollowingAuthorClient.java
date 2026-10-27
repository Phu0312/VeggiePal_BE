package com.veggiepal.blog.feed;

import java.util.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import com.veggiepal.blog.exception.*;

@Component
public class FollowingAuthorClient {
    private final RestClient http;
    private final String identityUrl;
    public FollowingAuthorClient(@Value("${services.identity.base-url:http://localhost:8081}") String identityUrl) {
        this.identityUrl=identityUrl;
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofSeconds(3));factory.setReadTimeout(Duration.ofSeconds(10));
        http=RestClient.builder().requestFactory(factory).build();
    }
    public List<Long> activeFollowingIds(String token) {
        try {
            Map<?,?> response=http.get().uri(identityUrl+"/users/me/following/ids")
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class);
            if(response==null || !(response.get("code") instanceof Number code) || code.intValue()!=1000
                    || !(response.get("result") instanceof List<?> ids)) throw unavailable();
            var result=new ArrayList<Long>();
            for(Object id:ids) { if(!(id instanceof Number n) || n.longValue()<=0) throw unavailable();result.add(n.longValue()); }
            return result.stream().distinct().toList();
        } catch(RestClientResponseException e) {
            if(e.getStatusCode().value()==401) throw new AppException(ErrorCode.UNAUTHENTICATED);
            if(e.getStatusCode().value()==403 || e.getStatusCode().value()==404) throw new AppException(ErrorCode.UNAUTHORIZED);
            throw unavailable();
        } catch(RestClientException e) { throw unavailable(); }
    }
    private AppException unavailable() { return new AppException(ErrorCode.COMMUNITY_UNAVAILABLE); }
}
