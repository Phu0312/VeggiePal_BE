package com.veggiepal.ai.chat;

import static org.mockito.Mockito.mockStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.common.AppException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.veggiepal.ai.quota.QuotaCalendar;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GuestSessionServiceTest {

    @Test
    void createGeneratesUuidSessionWithConfiguredTtl() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.save(any(GuestSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(repository.countByIpHashAndCreatedAtGreaterThanEqual(any(), any())).thenReturn(0L);
        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24), "pepper", 3);
        LocalDateTime before = QuotaCalendar.localNow();
        GuestSession created = service.create("203.0.113.10");
        LocalDateTime after = QuotaCalendar.localNow();

        assertNotNull(created);
        assertNotNull(created.getId());
        assertDoesNotThrow(() -> UUID.fromString(created.getId()));
        assertNotNull(created.getIpHash());
        assertTrue(created.getExpiresAt().isAfter(before.plusHours(23).plusMinutes(59)));
        assertTrue(created.getExpiresAt().isBefore(after.plusHours(24).plusMinutes(1)));

        ArgumentCaptor<GuestSession> captor = ArgumentCaptor.forClass(GuestSession.class);
        verify(repository).save(captor.capture());
        assertEquals(created.getId(), captor.getValue().getId());
    }

    @Test
    void requireValidPassesForActiveSession() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.save(any(GuestSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.countByIpHashAndCreatedAtGreaterThanEqual(any(), any())).thenReturn(0L);
        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24), "pepper", 3);
        GuestSession activeSession = service.create("203.0.113.10");
        when(repository.findByIdAndExpiresAtAfter(eq(activeSession.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSession));

        assertDoesNotThrow(() -> service.requireValid(activeSession.getId(), "203.0.113.10"));
        assertThrows(AppException.class, () -> service.requireValid(activeSession.getId(), "203.0.113.11"));
    }

    @Test
    void requireValidRejectsUnknownSession() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.findByIdAndExpiresAtAfter(eq("unknown-id"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24), "pepper", 3);
        assertThrows(AppException.class, () -> service.requireValid("unknown-id", "203.0.113.10"));
    }

    @Test
    void requireValidRejectsExpiredSession() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.findByIdAndExpiresAtAfter(eq("expired-id"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24), "pepper", 3);
        assertThrows(AppException.class, () -> service.requireValid("expired-id", "203.0.113.10"));
    }

    @Test
    void createRejectsSessionRotationAfterConfiguredDailyLimit() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.countByIpHashAndCreatedAtGreaterThanEqual(any(), any())).thenReturn(3L);
        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24), "pepper", 3);

        assertThrows(AppException.class, () -> service.create("203.0.113.10"));
    }

    @Test
    void guestSessionLimitUsesVietnamCalendarDay() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.save(any(GuestSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24), "pepper", 3);
        try (var calendar = mockStatic(QuotaCalendar.class)) {
            calendar.when(QuotaCalendar::localNow).thenReturn(LocalDateTime.of(2026, 11, 1, 0, 0));
            service.create("203.0.113.10");
        }
        verify(repository).countByIpHashAndCreatedAtGreaterThanEqual(any(),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)));
    }
}
