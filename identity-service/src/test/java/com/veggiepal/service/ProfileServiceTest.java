package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.veggiepal.dto.request.ChangePasswordRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    static final Long USER_ID = 7L;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    FileStorageService fileStorageService;

    @Spy
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    ProfileService profileService;

    @Test
    void getProfile_returnsCurrentUserProfile() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

        UserProfileResponse profile = profileService.getProfile(USER_ID);

        assertThat(profile.getId()).isEqualTo(USER_ID);
        assertThat(profile.getEmail()).isEqualTo("an@example.com");
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 31));
    }

    @Test
    void getProfile_unknownUser_throwsUserNotExisted() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(USER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void updateProfile_nullFields_keepExistingValues() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        profileService.updateProfile(USER_ID, new UpdateProfileRequest());

        assertThat(user.getFullName()).isEqualTo("Nguyen Van An");
        assertThat(user.getPhone()).isEqualTo("0901234567");
        assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 31));
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_trimsValuesAndClearsBlankPhone() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("  Tran Thi Binh  ")
                .phone("   ")
                .dateOfBirth(LocalDate.of(1999, 12, 1))
                .build();

        UserProfileResponse profile = profileService.updateProfile(USER_ID, request);

        assertThat(user.getFullName()).isEqualTo("Tran Thi Binh");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 12, 1));
        assertThat(profile.getFullName()).isEqualTo("Tran Thi Binh");
    }

    @Test
    void updateProfile_trimsNewPhone() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        profileService.updateProfile(USER_ID, UpdateProfileRequest.builder().phone(" 0987654321 ").build());

        assertThat(user.getPhone()).isEqualTo("0987654321");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsWrongPassword() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong-pass", "hashed-old-password")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(
                USER_ID, new ChangePasswordRequest("wrong-pass", "new-secret")))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WRONG_PASSWORD));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_sameAsCurrent_throwsPasswordUnchanged() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("old-secret", "hashed-old-password")).thenReturn(true);

        assertThatThrownBy(() -> profileService.changePassword(
                USER_ID, new ChangePasswordRequest("old-secret", "old-secret")))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_UNCHANGED));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_success_storesNewHash() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-secret", "hashed-old-password")).thenReturn(true);
        when(passwordEncoder.matches("new-secret", "hashed-old-password")).thenReturn(false);
        when(passwordEncoder.encode("new-secret")).thenReturn("hashed-new-password");

        profileService.changePassword(USER_ID, new ChangePasswordRequest("old-secret", "new-secret"));

        assertThat(user.getPasswordHash()).isEqualTo("hashed-new-password");
        verify(userRepository).save(user);
    }

    static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    static final String OLD_AVATAR_URL = "http://localhost:9000/veggiepal-avatars/avatars/7/old.png";

    static final String NEW_AVATAR_URL = "http://localhost:9000/veggiepal-avatars/avatars/7/new.png";

    @Test
    void uploadAvatar_emptyFile_throwsAvatarRequired() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AVATAR_REQUIRED));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_tooLarge_throwsAvatarTooLarge() {
        byte[] content = Arrays.copyOf(PNG_BYTES, 2 * 1024 * 1024 + 1);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", content);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AVATAR_TOO_LARGE));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_unsupportedContentType_throwsInvalidAvatarType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif", PNG_BYTES);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AVATAR_TYPE));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_bytesDoNotMatchContentType_throwsInvalidAvatarType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", PNG_BYTES);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AVATAR_TYPE));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_success_uploadsSavesAndDeletesPreviousAvatar() {
        User user = existingUser();
        user.setAvatarUrl(OLD_AVATAR_URL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(anyString(), aryEq(PNG_BYTES), eq("image/png"))).thenReturn(NEW_AVATAR_URL);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        UserProfileResponse profile = profileService.uploadAvatar(USER_ID, file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).upload(keyCaptor.capture(), aryEq(PNG_BYTES), eq("image/png"));
        assertThat(keyCaptor.getValue()).matches("avatars/7/[0-9a-f-]{36}\\.png");
        assertThat(user.getAvatarUrl()).isEqualTo(NEW_AVATAR_URL);
        assertThat(profile.getAvatarUrl()).isEqualTo(NEW_AVATAR_URL);
        verify(userRepository).save(user);
        verify(fileStorageService).delete(OLD_AVATAR_URL);
    }

    @Test
    void uploadAvatar_deletingPreviousAvatarFails_stillSucceeds() {
        User user = existingUser();
        user.setAvatarUrl(OLD_AVATAR_URL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(anyString(), aryEq(PNG_BYTES), eq("image/png"))).thenReturn(NEW_AVATAR_URL);
        doThrow(new AppException(ErrorCode.FILE_UPLOAD_FAILED)).when(fileStorageService).delete(OLD_AVATAR_URL);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        UserProfileResponse profile = profileService.uploadAvatar(USER_ID, file);

        assertThat(profile.getAvatarUrl()).isEqualTo(NEW_AVATAR_URL);
    }

    @Test
    void uploadAvatar_saveFails_deletesNewFileAndRethrows() {
        User user = existingUser();
        user.setAvatarUrl(OLD_AVATAR_URL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(anyString(), aryEq(PNG_BYTES), eq("image/png"))).thenReturn(NEW_AVATAR_URL);
        when(userRepository.save(user)).thenThrow(new IllegalStateException("db down"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
        verify(fileStorageService).delete(NEW_AVATAR_URL);
        verify(fileStorageService, never()).delete(OLD_AVATAR_URL);
    }

    static User existingUser() {
        return User.builder()
                .id(USER_ID)
                .email("an@example.com")
                .passwordHash("hashed-old-password")
                .fullName("Nguyen Van An")
                .phone("0901234567")
                .dateOfBirth(LocalDate.of(2000, 1, 31))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
    }
}
