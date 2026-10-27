package com.veggiepal.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.controller.UserController;
import com.veggiepal.service.UserService;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    void malformedJson_returnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018))
                .andExpect(jsonPath("$.message").value("Invalid request data"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {" ", "      ", "abc", "12345"})
    void invalidRegistrationPasswordIsRejected(String password) throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var body = new java.util.HashMap<String, Object>();
        body.put("email", "member@example.com"); body.put("fullName", "Member"); body.put("password", password);
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.isOneOf(1010, 1003)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("Invalid validation key")));
        org.mockito.Mockito.verifyNoInteractions(userService);
    }
}
