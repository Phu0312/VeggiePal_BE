package com.veggiepal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.veggiepal.dto.response.AdminUserResponse;
import com.veggiepal.dto.response.PageResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * task_be C12, task sheet US16/US17. There is no SUPER_ADMIN role: an admin may not act on
 * themselves or on any other ADMIN; administrators are managed in the database.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserService {

    static final int MAX_PAGE_SIZE = 100;

    UserRepository userRepository;
    RefreshTokenService refreshTokenService;
    UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String keyword, UserStatus status, Role role, int page, int size) {

        Page<User> users = userRepository.searchForAdmin(
                StringUtils.hasText(keyword) ? keyword.trim() : null, status, role,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        return toPage(users.map(userMapper::toAdminUserResponse));
    }

    @Transactional
    public AdminUserResponse lock(Long adminId, Long userId, String reason) {

        if (!StringUtils.hasText(reason) || reason.trim().length() > 500) {
            throw new AppException(ErrorCode.LOCK_REASON_REQUIRED);
        }

        User user = manageable(adminId, userId);

        user.setStatus(UserStatus.BLOCKED);
        user.setLockedReason(reason.trim());
        user.setLockedAt(LocalDateTime.now());
        user.setLockedBy(adminId);
        userRepository.save(user);

        // Signed out everywhere within one access-token lifetime (15 min)
        refreshTokenService.revokeAll(userId);

        return userMapper.toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse unlock(Long adminId, Long userId) {

        User user = manageable(adminId, userId);

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setLockedReason(null);
        user.setLockedAt(null);
        user.setLockedBy(null);
        userRepository.save(user);

        return userMapper.toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse changeRole(Long adminId, Long userId, Role role) {

        User user = manageable(adminId, userId);

        user.setRole(role);
        userRepository.save(user);

        // Revoked sessions must sign in again; existing access tokens expire within 15 minutes.
        refreshTokenService.revokeAll(userId);

        return userMapper.toAdminUserResponse(user);
    }

    private User manageable(Long adminId, Long userId) {

        if (adminId.equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_MANAGE_SELF);
        }

        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getRole() == Role.ADMIN) {
            throw new AppException(ErrorCode.CANNOT_MANAGE_ADMIN);
        }

        return user;
    }

    // PageResponse here is a record (items, page, size, totalElements, totalPages), as FollowService uses it
    private static PageResponse<AdminUserResponse> toPage(Page<AdminUserResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
