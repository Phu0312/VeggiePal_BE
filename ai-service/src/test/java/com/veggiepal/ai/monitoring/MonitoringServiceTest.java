package com.veggiepal.ai.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class MonitoringServiceTest {

    private UsageLogRepository repository;
    private MonitoringService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsageLogRepository.class);
        service = new MonitoringService(repository);
    }

    @Test
    void searchClampsNegativePageAndExcessiveSize() {
        Page<UsageLog> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        MonitoringService.UsageLogPage result = service.search(null, null, null, null, null, -5, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
        assertNotNull(result);
        assertEquals(0, result.totalElements());
    }

    @Test
    void searchPassesFiltersAndReturnsPopulatedPage() {
        UsageLog log = new UsageLog(10L, "user:10", "CHAT", "llama-3", "SUCCESS", 100, 50, 120L, null);
        Page<UsageLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        LocalDateTime now = LocalDateTime.now();
        MonitoringService.UsageLogPage result = service.search("CHAT", "SUCCESS", 10L, now.minusDays(1), now, 0, 10);

        assertEquals(1, result.items().size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals("CHAT", result.items().get(0).getFeature());
        assertEquals("SUCCESS", result.items().get(0).getStatus());
    }
}
