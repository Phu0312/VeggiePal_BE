package com.veggiepal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.dto.request.ChangePasswordRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.service.ProfileService;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class ProfileControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProfileService profileService;

    static RequestPostProcessor currentUser() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    @Test
    void getProfile_usesUserIdFromToken() throws Exception {
        when(profileService.getProfile(USER_ID))
                .thenReturn(UserProfileResponse.builder().id(USER_ID).email("an@example.com").build());

        mockMvc.perform(get("/users/me").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.email").value("an@example.com"));
    }

    @Test
    void updateProfile_passesRequestForCurrentUser() throws Exception {
        when(profileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(UserProfileResponse.builder().id(USER_ID).fullName("Tran Thi Binh").build());

        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Tran Thi Binh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fullName").value("Tran Thi Binh"));

        verify(profileService).updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
    }

    @Test
    void updateProfile_blankFullName_returnsFullNameRequired() throws Exception {
        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void updateProfile_futureDateOfBirth_returnsInvalidDateOfBirth() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateOfBirth\": \"" + tomorrow + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1013));
    }

    @Test
    void updateProfile_invalidDateFormat_returnsInvalidRequest() throws Exception {
        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateOfBirth": "2000-13-40"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void changePassword_success_returnsCode1000() throws Exception {
        mockMvc.perform(put("/users/me/password").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "old-secret", "newPassword": "new-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(profileService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_shortNewPassword_returnsInvalidPassword() throws Exception {
        mockMvc.perform(put("/users/me/password").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "old-secret", "newPassword": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("Password must be at least 6 characters"));
    }

    @Test
    void changePassword_blankCurrentPassword_returnsPasswordRequired() throws Exception {
        mockMvc.perform(put("/users/me/password").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "", "newPassword": "new-secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    void uploadAvatar_passesFileForCurrentUser() throws Exception {
        String avatarUrl = "http://localhost:9000/veggiepal-avatars/avatars/7/a.png";
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{(byte) 0x89, 0x50});
        when(profileService.uploadAvatar(eq(USER_ID), any(MultipartFile.class)))
                .thenReturn(UserProfileResponse.builder().avatarUrl(avatarUrl).build());

        mockMvc.perform(multipart("/users/me/avatar").file(file).with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.avatarUrl").value(avatarUrl));
    }

    @Test
    void uploadAvatar_missingFile_returnsAvatarRequired() throws Exception {
        mockMvc.perform(multipart("/users/me/avatar").with(currentUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1014));
    }
}
