package com.veggiepal.blog.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    /** A public endpoint; a null method means "any method". */
    record PublicEndpoint(HttpMethod method, String pattern) {
    }

    // The [0-9]+ constraint is load-bearing: "/blogs/me" matches a bare "/blogs/{id}",
    // which would make it public, strip its token, and then 401 forever on an
    // authenticated endpoint. Keep the digits.
    static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(null, "/swagger-ui/**"),
            new PublicEndpoint(null, "/swagger-ui.html"),
            new PublicEndpoint(null, "/v3/api-docs/**"),

            new PublicEndpoint(HttpMethod.GET, "/blogs"),
            new PublicEndpoint(HttpMethod.GET, "/blogs/{id:[0-9]+}"),
            new PublicEndpoint(HttpMethod.GET, "/blogs/{id:[0-9]+}/related"),

            new PublicEndpoint(HttpMethod.GET, "/categories"),
            new PublicEndpoint(HttpMethod.GET, "/categories/{id:[0-9]+}"),

            new PublicEndpoint(HttpMethod.GET, "/comments"),
            new PublicEndpoint(HttpMethod.GET, "/comments/{id:[0-9]+}/replies")
    );

    SecurityExceptionHandler securityExceptionHandler;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity
    ) throws Exception {

        httpSecurity
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(publicMatchers().toArray(RequestMatcher[]::new))
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                                .bearerTokenResolver(publicEndpointAwareBearerTokenResolver())
                                .authenticationEntryPoint(securityExceptionHandler)
                                .accessDeniedHandler(securityExceptionHandler)
                )
                .exceptionHandling(
                        exceptions -> exceptions
                                .authenticationEntryPoint(securityExceptionHandler)
                                .accessDeniedHandler(securityExceptionHandler)
                )
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        httpSecurity.csrf(
                AbstractHttpConfigurer::disable
        );

        return httpSecurity.build();
    }

    static List<RequestMatcher> publicMatchers() {

        return PUBLIC_ENDPOINTS.stream()
                .map(SecurityConfig::toMatcher)
                .toList();
    }

    private static RequestMatcher toMatcher(PublicEndpoint endpoint) {

        return endpoint.method() == null
                ? PathPatternRequestMatcher.withDefaults().matcher(endpoint.pattern())
                : PathPatternRequestMatcher.withDefaults().matcher(endpoint.method(), endpoint.pattern());
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // Public endpoints ignore the Authorization header, so a stale token cannot block them.
    private BearerTokenResolver publicEndpointAwareBearerTokenResolver() {

        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        List<RequestMatcher> publicMatchers = publicMatchers();

        return request -> publicMatchers.stream().anyMatch(matcher -> matcher.matches(request))
                ? null
                : defaultResolver.resolve(request);
    }
}
