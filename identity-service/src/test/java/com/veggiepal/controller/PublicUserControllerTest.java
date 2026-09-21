package com.veggiepal.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.service.PublicUserService;

@WebMvcTest(PublicUserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class PublicUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicUserService publicUserService;

    @Test
    void getPublicUsers_withoutToken_isPublic() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L, 2L)))
                .thenReturn(List.of(PublicUserResponse.builder()
                        .id(1L).fullName("Long Nguyễn").avatarUrl("http://minio/a.png").build()));

        mockMvc.perform(get("/users/batch").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].fullName").value("Long Nguyễn"));
    }

    // A stale token in the header must not turn a public endpoint into a 401
    @Test
    void getPublicUsers_withGarbageToken_stillPublic() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L)))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/batch").param("ids", "1")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicUsers_missingIdsParam_returnsInvalidRequest() throws Exception {
        mockMvc.perform(get("/users/batch"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPublicUsers_tooManyIds_returnsInvalidRequest() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L)))
                .thenThrow(new AppException(ErrorCode.INVALID_REQUEST));

        mockMvc.perform(get("/users/batch").param("ids", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }
}
