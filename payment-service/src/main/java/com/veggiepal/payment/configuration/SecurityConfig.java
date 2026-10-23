package com.veggiepal.payment.configuration;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {
    private static final List<RequestMatcher> PUBLIC_ENDPOINTS = List.of(
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
            PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/payments/plans"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/payments/webhook/payos"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/internal/subscriptions/{userId}")
    );

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityExceptionHandler errors) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(PUBLIC_ENDPOINTS.toArray(RequestMatcher[]::new)).permitAll()
                        .requestMatchers("/admin/payments/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .oauth2ResourceServer(o -> o.authenticationEntryPoint(errors).accessDeniedHandler(errors)
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
