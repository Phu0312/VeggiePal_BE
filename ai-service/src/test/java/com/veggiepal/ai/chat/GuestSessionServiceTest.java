package com.veggiepal.ai.chat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.common.AppException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GuestSessionServiceTest {
    @Test
    void rejectsUnknownOrStolenSession() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.findByIdAndIpHashAndExpiresAtAfter(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        GuestSessionService service = new GuestSessionService(repository, "test-pepper", Duration.ofHours(24), 3);
        assertThrows(AppException.class, () -> service.requireValid("not-issued", "127.0.0.1"));
    }

    @Test
    void limitsSessionCreationPerAddress() {
        GuestSessionRepository repository = mock(GuestSessionRepository.class);
        when(repository.countByIpHashAndCreatedAtAfter(anyString(), any())).thenReturn(3L);
        GuestSessionService service = new GuestSessionService(repository, "test-pepper", Duration.ofHours(24), 3);
        assertThrows(AppException.class, () -> service.create("127.0.0.1"));
    }
}
