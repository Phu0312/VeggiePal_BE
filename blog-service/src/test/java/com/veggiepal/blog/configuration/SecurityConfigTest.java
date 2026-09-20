package com.veggiepal.blog.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigTest {

    static boolean isPublic(String method, String uri) {

        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);

        return SecurityConfig.publicMatchers().stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Test
    void blogListAndDetail_arePublicForGet() {
        assertThat(isPublic("GET", "/blogs")).isTrue();
        assertThat(isPublic("GET", "/blogs/12")).isTrue();
        assertThat(isPublic("GET", "/blogs/12/related")).isTrue();
    }

    @Test
    void blogWrites_areNotPublic() {
        assertThat(isPublic("POST", "/blogs")).isFalse();
        assertThat(isPublic("PUT", "/blogs/12")).isFalse();
        assertThat(isPublic("DELETE", "/blogs/12")).isFalse();
    }

    // The whole reason /blogs/{id} is constrained to digits.
    @Test
    void ownBlogEndpoints_areNotPublic() {
        assertThat(isPublic("GET", "/blogs/me")).isFalse();
        assertThat(isPublic("GET", "/blogs/me/votes")).isFalse();
    }

    @Test
    void categoryReadsArePublic_butWritesAreNot() {
        assertThat(isPublic("GET", "/categories")).isTrue();
        assertThat(isPublic("GET", "/categories/3")).isTrue();
        assertThat(isPublic("POST", "/categories")).isFalse();
        assertThat(isPublic("DELETE", "/categories/3")).isFalse();
    }

    @Test
    void commentReadsArePublic_butWritesAreNot() {
        assertThat(isPublic("GET", "/comments")).isTrue();
        assertThat(isPublic("GET", "/comments/5/replies")).isTrue();
        assertThat(isPublic("POST", "/comments")).isFalse();
    }

    @Test
    void swaggerIsPublicForAnyMethod() {
        assertThat(isPublic("GET", "/swagger-ui.html")).isTrue();
        assertThat(isPublic("GET", "/v3/api-docs")).isTrue();
        assertThat(isPublic("GET", "/v3/api-docs/swagger-config")).isTrue();
    }

    @Test
    void publicMatchers_coversEveryDeclaredEndpoint() {
        assertThat(SecurityConfig.publicMatchers())
                .hasSameSizeAs(SecurityConfig.PUBLIC_ENDPOINTS)
                .allSatisfy(matcher -> assertThat(matcher).isInstanceOf(RequestMatcher.class));
    }
}
