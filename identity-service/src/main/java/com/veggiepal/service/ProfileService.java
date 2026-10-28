package com.veggiepal.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.dto.request.ChangePasswordRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.ImageType;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    FileStorageService fileStorageService;
    RefreshTokenService refreshTokenService;
    PlatformTransactionManager transactionManager;

    public UserProfileResponse getProfile(Long userId) {

        return userMapper.toUserProfileResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateProfile(user, request);
        user.setFullName(user.getFullName().trim());
        user.setPhone(
                StringUtils.hasText(user.getPhone())
                        ? user.getPhone().trim()
                        : null
        );

        userRepository.save(user);
        return userMapper.toUserProfileResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {

        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        // Every session, this device's included, must sign in again with the new password
        refreshTokenService.revokeAll(userId);
    }

    @Transactional(propagation = Propagation.NEVER)
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.AVATAR_REQUIRED);
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new AppException(ErrorCode.AVATAR_TOO_LARGE);
        }

        byte[] content = readContent(file);

        // The declared content type must match the real file signature
        ImageType imageType = ImageTypeDetector
                .detect(content)
                .filter(type -> type.getContentType().equals(file.getContentType()))
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.INVALID_AVATAR_TYPE
                        )
                );

        findUser(userId);

        String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + imageType.getExtension();
        String avatarUrl = fileStorageService.upload(key, content, imageType.getContentType());

        AvatarUpdate update;
        try {
            update = new TransactionTemplate(transactionManager).execute(status -> {
                User current = userRepository.lockById(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                String previous = current.getAvatarUrl();
                current.setAvatarUrl(avatarUrl);
                userRepository.save(current);
                return new AvatarUpdate(previous, userMapper.toUserProfileResponse(current));
            });
        } catch (RuntimeException exception) {
            deleteQuietly(avatarUrl);
            throw exception;
        }

        // execute returns only after commit, including the flush that may still fail.
        deleteQuietly(update.previousUrl());
        return update.response();
    }

    private record AvatarUpdate(String previousUrl, UserProfileResponse response) {}

    private byte[] readContent(MultipartFile file) {

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void deleteQuietly(String url) {

        if (url == null) {
            return;
        }

        try {
            fileStorageService.delete(url);
        } catch (RuntimeException exception) {
            log.warn("Could not delete avatar object from storage", exception);
        }
    }

    private User findUser(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.USER_NOT_EXISTED
                        )
                );
    }
}
