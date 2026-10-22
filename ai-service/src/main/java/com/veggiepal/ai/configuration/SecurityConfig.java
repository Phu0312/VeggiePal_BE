package com.veggiepal.ai.configuration;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final List<RequestMatcher> PUBLIC_ENDPOINTS = List.of(
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
            PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/actuator/health"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/ai/chat/guest"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/ai/chat/guest-sessions"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/ai/videos/by-video/{videoId:[0-9]+}")
    );

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityExceptionHandler errors) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ai/chat/guest").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ai/chat/guest-sessions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ai/videos/by-video/{videoId:[0-9]+}").permitAll()
                        .requestMatchers("/ai/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(errors).accessDeniedHandler(errors)
                        .bearerTokenResolver(publicEndpointAwareBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter())));
        return http.build();
    }

    private BearerTokenResolver publicEndpointAwareBearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> PUBLIC_ENDPOINTS.stream().anyMatch(matcher -> matcher.matches(request))
                ? null
                : delegate.resolve(request);
    }
    private JwtAuthenticationConverter converter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
