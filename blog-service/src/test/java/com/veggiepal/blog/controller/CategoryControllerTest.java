package com.veggiepal.blog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.service.CategoryService;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class CategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryService categoryService;

    // jwt() does not run the app's real JwtAuthenticationConverter (that only happens on the
    // real OAuth2 resource server filter, which this postprocessor bypasses), so the "role"
    // claim alone would not become a ROLE_* authority. Supplying it explicitly here mirrors
    // what SecurityConfig.jwtAuthenticationConverter() does for a real token, which is what
    // lets @PreAuthorize("hasRole(...)") be exercised meaningfully by this test.
    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", 7L).claim("role", "USER"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Matches the default jwt.secret in application.properties (JWT_SECRET is unset in tests).
    // Same constant JwtConfigTest uses to mint tokens for the same reason.
    private static final String JWT_SECRET = "veggiepal-secret-key-must-be-at-least-32-characters";

    private static String signedToken(String role) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin@example.com")
                .claim("userId", 1L)
                .claim("role", role)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    @Test
    void getTree_withoutToken_isPublic() throws Exception {
        when(categoryService.getTree(null, true)).thenReturn(List.of(
                CategoryResponse.builder().id(1L).name("Công thức").type(CategoryType.RECIPE_TYPE).build()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].name").value("Công thức"));
    }

    @Test
    void create_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        verify(categoryService, never()).create(any());
    }

    @Test
    void create_asMember_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/categories").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1009));

        verify(categoryService, never()).create(any());
    }

    @Test
    void create_asAdmin_succeeds() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenReturn(CategoryResponse.builder().id(1L).name("Công thức").build());

        mockMvc.perform(post("/categories").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1));
    }

    // Unlike admin()/member() above (which bypass the real converter via jwt()'s manufactured
    // Authentication), this sends a genuine signed token through the actual OAuth2
    // resource-server filter chain. It proves SecurityConfig really does decode the token and
    // map its "role" claim to a ROLE_* authority that satisfies hasRole('ADMIN') — not just
    // that @PreAuthorize enforces whatever authority a test hands it directly.
    @Test
    void create_withRealAdminToken_succeeds() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenReturn(CategoryResponse.builder().id(1L).name("Công thức").build());

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + signedToken("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1));
    }

    @Test
    void create_blankName_returnsCategoryNameRequired() throws Exception {
        mockMvc.perform(post("/categories").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3001));
    }

    @Test
    void delete_asAdmin_succeeds() throws Exception {
        mockMvc.perform(delete("/categories/3").with(admin()))
                .andExpect(status().isOk());

        verify(categoryService).delete(3L);
    }
}
