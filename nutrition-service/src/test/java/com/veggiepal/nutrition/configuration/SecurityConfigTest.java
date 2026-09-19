package com.veggiepal.nutrition.configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veggiepal.nutrition.controller.CurrentUser;
import com.veggiepal.nutrition.dto.response.ApiResponse;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
// ProbeController is nested inside this test class, so Spring Boot's TestTypeExcludeFilter
// (which excludes inner classes of any class carrying @Test methods) always keeps it out of
// component-scan-based discovery, even though @WebMvcTest(controllers = ...) names it explicitly.
// Importing it directly here bypasses that classpath-scan filter and registers it as a bean.
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class, SecurityConfigTest.ProbeController.class})
class SecurityConfigTest {

    @RestController
    static class ProbeController {

        @GetMapping("/nutrition/me/probe")
        ApiResponse<Long> probe(@AuthenticationPrincipal Jwt jwt) {
            return ApiResponse.<Long>builder().result(CurrentUser.id(jwt)).build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void withoutToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void malformedToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void validToken_exposesUserIdClaim() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe").with(jwt().jwt(token -> token.claim("userId", 7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(7));
    }

    @Test
    void tokenWithoutUserId_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe").with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }
}
