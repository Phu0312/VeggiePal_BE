package com.veggiepal.nutrition.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.nutrition.configuration.JwtConfig;
import com.veggiepal.nutrition.configuration.SecurityConfig;
import com.veggiepal.nutrition.configuration.SecurityExceptionHandler;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.enums.AllergenCategory;
import com.veggiepal.nutrition.service.AllergyService;

@WebMvcTest(AllergyController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class AllergyControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AllergyService allergyService;

    static RequestPostProcessor currentUser() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    @Test
    void getAllergens_returnsCatalog() throws Exception {
        when(allergyService.getAllAllergens()).thenReturn(List.of(
                AllergenResponse.builder().id(1L).code("GLUTEN").name("Gluten").category(AllergenCategory.GRAIN).build()));

        mockMvc.perform(get("/nutrition/allergens").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].code").value("GLUTEN"))
                .andExpect(jsonPath("$.result[0].category").value("GRAIN"));
    }

    @Test
    void getMyAllergies_usesCurrentUser() throws Exception {
        when(allergyService.getMyAllergies(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/nutrition/me/allergies").with(currentUser()))
                .andExpect(status().isOk());

        verify(allergyService).getMyAllergies(USER_ID);
    }

    @Test
    void replaceMyAllergies_passesIdsForCurrentUser() throws Exception {
        mockMvc.perform(put("/nutrition/me/allergies").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allergenIds": [1, 2]}
                                """))
                .andExpect(status().isOk());

        verify(allergyService).replaceAllergies(
                eq(USER_ID), argThat(request -> request.getAllergenIds().equals(List.of(1L, 2L))));
    }

    @Test
    void replaceMyAllergies_missingList_returnsAllergenIdsRequired() throws Exception {
        mockMvc.perform(put("/nutrition/me/allergies").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    void replaceMyAllergies_nullId_returnsAllergenNotExisted() throws Exception {
        mockMvc.perform(put("/nutrition/me/allergies").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allergenIds": [null]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2007));
    }
}
