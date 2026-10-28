package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    static final Long ADMIN_ID = 1L;

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenService refreshTokenService;

    @Spy
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    AdminUserService adminUserService;

    static User user(Long id, Role role, UserStatus status) {
        return User.builder().id(id).email("u" + id + "@example.com").fullName("U" + id)
                .passwordHash("hash").role(role).status(status).emailVerified(true).build();
    }

    @Test
    void lock_recordsWhoWhyWhenAndSignsTheUserOut() {
        User target = user(7L, Role.USER, UserStatus.ACTIVE);
        when(userRepository.lockById(7L)).thenReturn(Optional.of(target));

        var response = adminUserService.lock(ADMIN_ID, 7L, "  spam  ");

        assertThat(target.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(target.getLockedReason()).isEqualTo("spam");
        assertThat(target.getLockedBy()).isEqualTo(ADMIN_ID);
        assertThat(target.getLockedAt()).isNotNull();
        assertThat(response.getLockedReason()).isEqualTo("spam");
        verify(userRepository).save(target);
        verify(refreshTokenService).revokeAll(7L);
    }

    @Test
    void lock_blankReason_isRejected() {
        assertCode(() -> adminUserService.lock(ADMIN_ID, 7L, "  "), ErrorCode.LOCK_REASON_REQUIRED);
    }

    @Test
    void lock_self_isRejected() {
        assertCode(() -> adminUserService.lock(ADMIN_ID, ADMIN_ID, "x"), ErrorCode.CANNOT_MANAGE_SELF);
    }

    // Task sheet US16 without a SUPER_ADMIN role: no admin can act on another admin
    @Test
    void lock_anotherAdmin_isRejected() {
        when(userRepository.lockById(2L)).thenReturn(Optional.of(user(2L, Role.ADMIN, UserStatus.ACTIVE)));

        assertCode(() -> adminUserService.lock(ADMIN_ID, 2L, "x"), ErrorCode.CANNOT_MANAGE_ADMIN);
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    void unlock_blockedUser_becomesActiveAndForgetsTheReason() {
        User target = user(7L, Role.USER, UserStatus.BLOCKED);
        target.setLockedReason("spam");
        when(userRepository.lockById(7L)).thenReturn(Optional.of(target));

        adminUserService.unlock(ADMIN_ID, 7L);

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(target.getLockedReason()).isNull();
        assertThat(target.getLockedAt()).isNull();
        assertThat(target.getLockedBy()).isNull();
    }

    @Test
    void unlock_userWhoIsNotBlocked_isInvalid() {
        when(userRepository.lockById(7L)).thenReturn(Optional.of(user(7L, Role.USER, UserStatus.PENDING)));

        assertCode(() -> adminUserService.unlock(ADMIN_ID, 7L), ErrorCode.INVALID_REQUEST);
    }

    @Test
    void changeRole_promotesAndForcesANewToken() {
        User target = user(7L, Role.USER, UserStatus.ACTIVE);
        when(userRepository.lockById(7L)).thenReturn(Optional.of(target));

        adminUserService.changeRole(ADMIN_ID, 7L, Role.ADMIN);

        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        verify(refreshTokenService).revokeAll(7L);
    }

    @Test
    void anyAction_onAMissingUser_isNotFound() {
        when(userRepository.lockById(9L)).thenReturn(Optional.empty());

        assertCode(() -> adminUserService.unlock(ADMIN_ID, 9L), ErrorCode.USER_NOT_EXISTED);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"lock", "unlock", "role"})
    void everyMutation_rejectsSelfAndOtherAdmins(String action) {
        assertCode(() -> mutate(action, ADMIN_ID), ErrorCode.CANNOT_MANAGE_SELF);
        when(userRepository.lockById(2L)).thenReturn(Optional.of(user(2L, Role.ADMIN, UserStatus.BLOCKED)));
        assertCode(() -> mutate(action, 2L), ErrorCode.CANNOT_MANAGE_ADMIN);
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    private void mutate(String action, Long target) {
        switch (action) {
            case "lock" -> adminUserService.lock(ADMIN_ID, target, "spam");
            case "unlock" -> adminUserService.unlock(ADMIN_ID, target);
            case "role" -> adminUserService.changeRole(ADMIN_ID, target, Role.ADMIN);
            default -> throw new AssertionError(action);
        }
    }

    @Test
    void lock_nullOrTooLongReason_doesNotWriteAnything() {
        assertCode(() -> adminUserService.lock(ADMIN_ID, 7L, null), ErrorCode.LOCK_REASON_REQUIRED);
        assertCode(() -> adminUserService.lock(ADMIN_ID, 7L, "x".repeat(501)), ErrorCode.LOCK_REASON_REQUIRED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void relock_updatesAuditAndAccepts500CharacterReason() {
        User target = user(7L, Role.USER, UserStatus.BLOCKED);
        target.setLockedReason("old");
        target.setLockedBy(3L);
        when(userRepository.lockById(7L)).thenReturn(Optional.of(target));
        var response = adminUserService.lock(ADMIN_ID, 7L, "x".repeat(500));
        assertThat(response.getLockedReason()).isEqualTo("x".repeat(500));
        assertThat(target.getLockedBy()).isEqualTo(ADMIN_ID);
        var order = org.mockito.Mockito.inOrder(userRepository, refreshTokenService);
        order.verify(userRepository).lockById(7L);
        order.verify(userRepository).save(target);
        order.verify(refreshTokenService).revokeAll(7L);
    }

    @Test
    void search_normalizesKeywordClampsPaginationAndMapsMetadata() {
        User target = user(7L, Role.USER, UserStatus.BLOCKED);
        target.setLockedReason("spam");
        var pageable = org.springframework.data.domain.PageRequest.of(0, 100,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
        when(userRepository.searchForAdmin("An", UserStatus.BLOCKED, Role.USER, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(target), pageable, 101));
        var result = adminUserService.search(" An ", UserStatus.BLOCKED, Role.USER, -1, 1000);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(100);
        assertThat(result.totalElements()).isEqualTo(101);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).singleElement().satisfies(u -> {
            assertThat(u.getEmail()).isEqualTo("u7@example.com");
            assertThat(u.getLockedReason()).isEqualTo("spam");
        });
    }

    @Test
    void search_blankKeywordAndZeroSize_useUnfilteredMinimumPage() {
        var pageable = org.springframework.data.domain.PageRequest.of(2, 1,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
        when(userRepository.searchForAdmin(null, null, null, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(), pageable, 0));
        assertThat(adminUserService.search("  ", null, null, 2, 0).size()).isEqualTo(1);
    }
    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
