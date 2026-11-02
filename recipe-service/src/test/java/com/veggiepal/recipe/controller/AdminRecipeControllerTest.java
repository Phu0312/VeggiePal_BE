package com.veggiepal.recipe.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veggiepal.recipe.configuration.JwtConfig;
import com.veggiepal.recipe.configuration.SecurityConfig;
import com.veggiepal.recipe.configuration.SecurityExceptionHandler;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.service.RecipeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminRecipeController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class AdminRecipeControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RecipeService service;

    @Test void adminCanFilterArchivedRecipes() throws Exception {
        when(service.getAdminRecipes(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.<com.veggiepal.recipe.dto.response.RecipeResponse>builder().items(List.of()).build());
        mvc.perform(get("/admin/recipes")
                .with(jwt().jwt(t -> t.claim("userId", 1L).claim("role", "ADMIN"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .param("status", "ARCHIVED").param("authorId", "7").param("keyword", "pho"))
                .andExpect(status().isOk());
        verify(service).getAdminRecipes(RecipeStatus.ARCHIVED, 7L, "pho", 0, 20);
    }

    @Test void memberCannotListRecipes() throws Exception {
        mvc.perform(get("/admin/recipes")
                .with(jwt().jwt(t -> t.claim("userId", 7L).claim("role", "USER"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
