package com.veggiepal.ai.quota;

import static org.assertj.core.api.Assertions.assertThat;

import com.veggiepal.ai.common.AppException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QuotaConcurrencyIntegrationTest {
    @Autowired QuotaService service;
    @Autowired DailyQuotaRepository repository;

    @BeforeEach void clean() { repository.deleteAll(); }

    @Test
    void concurrentReservationsCannotExceedLimit() throws Exception {
        int attempts = 12;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CyclicBarrier barrier = new CyclicBarrier(attempts);
        AtomicInteger accepted = new AtomicInteger();
        var futures = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    try {
                        service.reserveFeature("user:concurrent", "CHAT", QuotaPeriod.DAILY, 5);
                        accepted.incrementAndGet();
                    } catch (AppException ignored) {
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        DailyQuota row = repository.findAll().stream()
                .filter(item -> item.getPrincipalKey().equals("user:concurrent")
                        && item.getFeature().equals("CHAT") && item.getUsageDate().equals(LocalDate.now()))
                .findFirst().orElseThrow();
        assertThat(accepted.get()).isEqualTo(5);
        assertThat(row.getRequestCount()).isEqualTo(5);
    }
}
