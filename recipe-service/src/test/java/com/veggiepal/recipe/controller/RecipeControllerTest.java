package com.veggiepal.recipe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.recipe.configuration.JwtConfig;
import com.veggiepal.recipe.configuration.SecurityConfig;
import com.veggiepal.recipe.configuration.SecurityExceptionHandler;
import com.veggiepal.recipe.dto.response.RecipeResponse;
import com.veggiepal.recipe.enums.RecipeStatus;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.service.RecipeFavoriteService;
import com.veggiepal.recipe.service.RecipeImageService;
import com.veggiepal.recipe.service.RecipeService;

@WebMvcTest(RecipeController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class RecipeControllerTest {

    static final Long USER_ID = 100L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecipeService recipeService;

    @MockitoBean
    RecipeImageService recipeImageService;

    @MockitoBean
    RecipeFavoriteService recipeFavoriteService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"));
    }

    @Test
    void getRecipe_ownerCanReadOwnDraft() throws Exception {
        RecipeResponse response = RecipeResponse.builder().id(10L).status(RecipeStatus.DRAFT).build();
        when(recipeService.getRecipe(10L, USER_ID, false)).thenReturn(response);

        mockMvc.perform(get("/recipes/10").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.status").value("DRAFT"));
    }

    @Test
    void getRecipe_adminCanReadAnyDraft() throws Exception {
        RecipeResponse response = RecipeResponse.builder().id(10L).status(RecipeStatus.DRAFT).build();
        when(recipeService.getRecipe(10L, 1L, true)).thenReturn(response);

        mockMvc.perform(get("/recipes/10").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.status").value("DRAFT"));
    }

    @Test
    void getRecipe_guestIsHiddenFromDraft() throws Exception {
        when(recipeService.getRecipe(10L, null, false))
                .thenThrow(new AppException(ErrorCode.RECIPE_NOT_EXISTED));

        mockMvc.perform(get("/recipes/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4100));
    }

    @Test
    void getRecipe_invalidTokenDoesNotCause401OnPublicEndpoint() throws Exception {
        RecipeResponse response = RecipeResponse.builder().id(10L).status(RecipeStatus.PUBLISHED).build();
        // Since token is invalid, resolver strips it so requesterId is null, but request succeeds as guest
        when(recipeService.getRecipe(10L, null, false)).thenReturn(response);

        mockMvc.perform(get("/recipes/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.malformed.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10));
    }

    @Test
    void getMyRecipes_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/recipes/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test void unexpectedDatabaseFailureRemains500() throws Exception {
        when(recipeService.createRecipe(eq(USER_ID),any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("database failure"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/recipes").with(member())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("""
                {"title":"Tofu","servings":1,"ingredients":[{"ingredientId":1,"quantity":100,"unit":"g"}],
                "steps":[{"stepNumber":1,"instruction":"Cook"}]}
                """))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(9999));
    }

    @Test void listEmitsSharedPaginationWithoutContentAlias() throws Exception {
        when(recipeService.getPublishedRecipes(null,null,0,20)).thenReturn(
            com.veggiepal.recipe.dto.response.PageResponse.<RecipeResponse>builder()
                .items(java.util.List.of(RecipeResponse.builder().id(10L).build()))
                .page(0).size(20).totalElements(1).totalPages(1).build());
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.items[0].id").value(10))
                .andExpect(jsonPath("$.result.content").doesNotExist())
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.first").doesNotExist()).andExpect(jsonPath("$.result.last").doesNotExist());
    }

}
