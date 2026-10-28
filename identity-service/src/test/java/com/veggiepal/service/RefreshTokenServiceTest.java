package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.veggiepal.entity.RefreshToken;
import com.veggiepal.entity.User;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.RefreshTokenRepository;
import com.veggiepal.repository.UserRepository;

class RefreshTokenServiceTest {

    RefreshTokenRepository repository;
    UserRepository users;
    RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        users = mock(UserRepository.class);
        when(users.lockById(7L)).thenReturn(Optional.of(User.builder().id(7L).build()));
        service = new RefreshTokenService(repository, users, Duration.ofDays(7));
    }

    @Test
    void issue_storesOnlyTheHash() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(call -> call.getArgument(0));

        String raw = service.issue(7L);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.sha256(raw)).isNotEqualTo(raw).hasSize(64);
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void rotate_deletesTheUsedTokenAndReturnsItsUser() {
        RefreshToken row = RefreshToken.builder().id(1L).userId(7L)
                .tokenHash(RefreshTokenService.sha256("raw")).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(repository.findUserIdByTokenHash(RefreshTokenService.sha256("raw"))).thenReturn(Optional.of(7L));
        when(repository.findForRotation(RefreshTokenService.sha256("raw"))).thenReturn(Optional.of(row));

        assertThat(service.rotate("raw")).isEqualTo(7L);
        verify(repository).delete(row);
    }

    @Test
    void rotate_expiredToken_isInvalidAndRemoved() {
        RefreshToken row = RefreshToken.builder().id(1L).userId(7L)
                .tokenHash(RefreshTokenService.sha256("raw")).expiresAt(LocalDateTime.now().minusSeconds(1)).build();
        when(repository.findUserIdByTokenHash(RefreshTokenService.sha256("raw"))).thenReturn(Optional.of(7L));
        when(repository.findForRotation(RefreshTokenService.sha256("raw"))).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.rotate("raw"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
        verify(repository).delete(row);
    }

    @Test
    void rotate_unknownOrBlankToken_isInvalid() {
        when(repository.findUserIdByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nope")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.rotate(" ")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.rotate(null)).isInstanceOf(AppException.class);
    }

    @Test
    void revokeAll_deletesEveryTokenOfTheUser() {
        service.revokeAll(7L);

        verify(repository).deleteByUserId(7L);
    }
}
