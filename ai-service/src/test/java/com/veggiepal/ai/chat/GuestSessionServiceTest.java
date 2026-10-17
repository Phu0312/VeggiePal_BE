package com.veggiepal.ai.chat;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GuestSessionServiceTest {

    @Test
    void createGeneratesUuidSessionWithConfiguredTtl() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.save(any(GuestSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24));
        LocalDateTime before = LocalDateTime.now();
        GuestSession created = service.create();
        LocalDateTime after = LocalDateTime.now();

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
        GuestSession activeSession = new GuestSession(UUID.randomUUID().toString(),
                LocalDateTime.now(), LocalDateTime.now().plusHours(24));
        when(repository.findByIdAndExpiresAtAfter(eq(activeSession.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSession));

        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24));
        assertDoesNotThrow(() -> service.requireValid(activeSession.getId()));
    }

    @Test
    void requireValidRejectsUnknownSession() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.findByIdAndExpiresAtAfter(eq("unknown-id"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24));
        assertThrows(AppException.class, () -> service.requireValid("unknown-id"));
    }

    @Test
    void requireValidRejectsExpiredSession() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.findByIdAndExpiresAtAfter(eq("expired-id"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        GuestSessionService service = new GuestSessionService(repository, Duration.ofHours(24));
        assertThrows(AppException.class, () -> service.requireValid("expired-id"));
    }
}
