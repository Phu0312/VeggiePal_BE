package com.veggiepal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.dto.response.PageResponse;
import com.veggiepal.service.AdminUserService;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class AdminUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminUserService adminUserService;

    @Test
    void withoutToken_is401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    // Task sheet US17: any role but ADMIN is stopped by Spring Security with 403
    @Test
    void asUser_is403() throws Exception {
        mockMvc.perform(get("/admin/users").with(jwt().jwt(t -> t.claim("userId", 7L))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1009));
    }

    @Test
    void asAdmin_isAllowed() throws Exception {
        when(adminUserService.search(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(java.util.List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/admin/users").with(jwt().jwt(t -> t.claim("userId", 1L))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void lock_withoutReason_is400() throws Exception {
        mockMvc.perform(put("/admin/users/7/lock").with(jwt().jwt(t -> t.claim("userId", 1L))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1026));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"lock", "unlock", "role"})
    void userCannotMutateAccounts(String action) throws Exception {
        mockMvc.perform(put("/admin/users/7/" + action)
                        .with(jwt().jwt(t -> t.claim("userId", 7L))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(1009));
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
    }

    @Test
    void lock_validRequestUsesAuthenticatedAdminAndOmitsPasswordHash() throws Exception {
        when(adminUserService.lock(1L, 7L, "spam"))
                .thenReturn(com.veggiepal.dto.response.AdminUserResponse.builder()
                        .id(7L).email("u7@example.com").lockedReason("spam").build());
        mockMvc.perform(put("/admin/users/7/lock")
                        .with(jwt().jwt(t -> t.claim("userId", 1L))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"spam\",\"adminId\":99}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.id").value(7))
                .andExpect(jsonPath("$.result.lockedReason").value("spam"))
                .andExpect(jsonPath("$.result.passwordHash").doesNotExist());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"{}", "{\"role\":null}", "{\"role\":\"SUPER_ADMIN\"}"})
    void role_missingOrUnsupported_is400(String body) throws Exception {
        mockMvc.perform(put("/admin/users/7/role")
                        .with(jwt().jwt(t -> t.claim("userId", 1L))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
    }}
