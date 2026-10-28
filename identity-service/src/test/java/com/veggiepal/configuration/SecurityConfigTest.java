package com.veggiepal.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.controller.UserController;
import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.service.UserService;
import com.veggiepal.service.RefreshTokenService;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class SecurityConfigTest {

    private static final String LOGIN_BODY = """
            {"email": "an@example.com", "password": "secret123"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    RefreshTokenService refreshTokenService;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void verifyEmail_isPublicEvenWithStaleBearer(boolean staleBearer) throws Exception {
        when(userService.verifyEmail("an@example.com", "123456"))
                .thenReturn(LoginResponse.builder().accessToken("verified-access").refreshToken("verified-refresh")
                        .expiresIn(900L).build());
        var request = post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"an@example.com\",\"otp\":\"123456\"}");
        if (staleBearer) request.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage");
        mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("verified-access"))
                .andExpect(jsonPath("$.result.refreshToken").value("verified-refresh"))
                .andExpect(jsonPath("$.result.expiresIn").value(900));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void resendVerification_isPublicEvenWithStaleBearer(boolean staleBearer) throws Exception {
        var request = post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"an@example.com\"}");
        if (staleBearer) request.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage");
        mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value(
                        "If the account is waiting for verification, a new code has been sent"))
                .andExpect(jsonPath("$.result").doesNotExist());
        org.mockito.Mockito.verify(userService).resendVerification("an@example.com");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "/auth/verify-email,'{\"email\":\"\",\"otp\":\"123456\"}',1005",
            "/auth/verify-email,'{\"email\":\"bad\",\"otp\":\"123456\"}',1004",
            "/auth/verify-email,'{\"email\":\"an@example.com\",\"otp\":\" \"}',1028",
            "/auth/resend-verification,'{\"email\":\"\"}',1005",
            "/auth/resend-verification,'{\"email\":\"bad\"}',1004"
    })
    void verification_invalidInputIsRejectedBeforeService(String path, String body, int error) throws Exception {
        mockMvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(error));
        org.mockito.Mockito.verifyNoInteractions(userService);
    }

    @Test
    void verifyEmail_expiredOtpReturnsItsApiError() throws Exception {
        when(userService.verifyEmail("an@example.com", "123456"))
                .thenThrow(new com.veggiepal.exception.AppException(com.veggiepal.exception.ErrorCode.OTP_EXPIRED));
        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"an@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1029));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void resetEndpoints_arePublicAndReturnTheirContract(boolean staleBearer) throws Exception {
        when(userService.verifyResetOtp("an@example.com", "123456")).thenReturn(
                com.veggiepal.dto.response.ResetTokenResponse.builder().resetToken("reset-proof").expiresIn(600L).build());
        String[][] requests = {
                {"/auth/forgot-password", "{\"email\":\"an@example.com\"}"},
                {"/auth/verify-otp", "{\"email\":\"an@example.com\",\"otp\":\"123456\"}"},
                {"/auth/reset-password", "{\"resetToken\":\"reset-proof\",\"newPassword\":\"123456\"}"}
        };
        for (String[] item : requests) {
            var request = post(item[0]).contentType(MediaType.APPLICATION_JSON).content(item[1]);
            if (staleBearer) request.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage");
            var response = mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000));
            if (item[0].endsWith("verify-otp")) {
                response.andExpect(jsonPath("$.result.resetToken").value("reset-proof"))
                        .andExpect(jsonPath("$.result.expiresIn").value(600));
            } else {
                response.andExpect(jsonPath("$.result").doesNotExist());
            }
        }
        org.mockito.Mockito.verify(userService).resetPassword("reset-proof", "123456");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "/auth/forgot-password,'{\"email\":\"bad\"}',1004",
            "/auth/forgot-password,'{\"email\":\"\"}',1005",
            "/auth/verify-otp,'{\"email\":\"bad\",\"otp\":\"123456\"}',1004",
            "/auth/verify-otp,'{\"email\":\"an@example.com\",\"otp\":\" \"}',1028",
            "/auth/reset-password,'{\"resetToken\":\" \" ,\"newPassword\":\"secret123\"}',1030",
            "/auth/reset-password,'{\"resetToken\":\"reset\",\"newPassword\":\"      \"}',1010",
            "/auth/reset-password,'{\"resetToken\":\"reset\",\"newPassword\":\"12345\"}',1003"
    })
    void resetEndpoints_invalidInputIsRejected(String path, String body, int code) throws Exception {
        mockMvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, "Bearer stale")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code));
        org.mockito.Mockito.verifyNoInteractions(userService);
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void login_withStaleBearerHeader_isNotRejected() throws Exception {
        when(userService.login(any()))
                .thenReturn(LoginResponse.builder().accessToken("token").build());

        mockMvc.perform(post("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void googleLogin_withStaleBearerHeader_isNotRejected() throws Exception {
        when(userService.loginWithGoogle("google-id-token"))
                .thenReturn(LoginResponse.builder().accessToken("token").build());

        mockMvc.perform(post("/auth/google")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "google-id-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void googleLogin_blankIdToken_returns400InvalidRequest() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }
    @Test
    void logout_withoutAnyToken_isPublic() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "r"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_withStaleBearerHeader_isPublic() throws Exception {
        when(userService.refresh("r")).thenReturn(LoginResponse.builder().accessToken("token").build());

        mockMvc.perform(post("/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "r"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }



    @Test
    void refresh_blankToken_returns1027WithoutCallingService() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \" \"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1027));
        org.mockito.Mockito.verifyNoInteractions(userService);
    }

    @Test
    void logout_withStaleBearerHeader_revokesSuppliedToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"device-session\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
        org.mockito.Mockito.verify(refreshTokenService).revoke("device-session");
    }

    @Test
    void logout_emptyBodyObject_stillSucceeds() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(refreshTokenService).revoke(null);
    }
}
