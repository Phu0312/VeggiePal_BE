package com.veggiepal.recipe.controller;

import com.veggiepal.recipe.configuration.*;
import com.veggiepal.recipe.dto.request.*;
import com.veggiepal.recipe.service.RecipeFavoriteService;
import com.veggiepal.recipe.service.RecipeImageService;
import com.veggiepal.recipe.service.RecipeService;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(RecipeController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class RecipeValidationTest {
    @Autowired MockMvc mvc;
    @MockitoBean RecipeService service;
    @MockitoBean RecipeImageService recipeImageService;
    @MockitoBean RecipeFavoriteService recipeFavoriteService;

    @ParameterizedTest
    @CsvSource({"title,200,4110", "description,2000,4111", "imageUrl,500,4112",
        "unit,30,4113", "note,300,4114", "instruction,2000,4115"})
    void rejectsOversizedFieldsBeforeCreateOrUpdate(String field, int max, int code) throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        String payload = """
            {"title":"Healthy bowl","description":"Good","imageUrl":"https://example.com/a.jpg",
             "servings":1,"ingredients":[{"ingredientId":1,"quantity":100,"unit":"g","note":"fresh"}],
             "steps":[{"stepNumber":1,"instruction":"Cook well"}]}
            """;
        var body = mapper.readTree(payload).deepCopy();
        if (field.equals("unit") || field.equals("note")) {
            ((tools.jackson.databind.node.ObjectNode) body.get("ingredients").get(0)).put(field, "x".repeat(max+1));
        } else if (field.equals("instruction")) {
            ((tools.jackson.databind.node.ObjectNode) body.get("steps").get(0)).put(field, "x".repeat(max+1));
        } else { ((tools.jackson.databind.node.ObjectNode) body).put(field, "x".repeat(max+1)); }
        for (var builder : java.util.List.of(post("/recipes"), put("/recipes/1"))) {
            mvc.perform(builder.with(jwt().jwt(j -> j.claim("userId", 7L).claim("role", "USER")))
                    .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(String.valueOf(max))));
        }
        verifyNoInteractions(service);
    }
}
