package com.veggiepal.payment.configuration;

import com.veggiepal.payment.dto.response.ApiResponse;
import com.veggiepal.payment.exception.ErrorCode;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper mapper;
    public SecurityExceptionHandler(JsonMapper mapper) { this.mapper = mapper; }
    @Override public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        write(res, ErrorCode.UNAUTHENTICATED);
    }
    @Override public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        write(res, ErrorCode.UNAUTHORIZED);
    }
    private void write(HttpServletResponse response, ErrorCode error) throws IOException {
        response.setStatus(error.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(mapper.writeValueAsString(ApiResponse.builder().code(error.getCode()).message(error.getMessage()).build()));
    }
}
