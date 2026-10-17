package com.veggiepal.ai.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Runs the real aggregate @Query: how Spring Data shapes a multi-column single-row result
// only shows up against a database, never with a mocked repository.
@SpringBootTest
class MonitoringSummaryIntegrationTest {
    @Autowired MonitoringController controller;
    @Autowired UsageLogRepository logs;

    @BeforeEach
    void clean() {
        logs.deleteAll();
    }

    @Test
    void summaryAggregatesUsageLogs() {
        logs.save(new UsageLog(1L, "user:1", "CHAT", "m", "SUCCESS", 10, 5, 100, null));
        logs.save(new UsageLog(1L, "user:1", "CHAT", "m", "FAILED", 0, 0, 300, "boom"));

        Map<String, Object> summary = controller.summary(7).result();

        assertEquals(2L, ((Number) summary.get("requests")).longValue());
        assertEquals(10L, ((Number) summary.get("promptTokens")).longValue());
        assertEquals(5L, ((Number) summary.get("completionTokens")).longValue());
        assertEquals(200.0, ((Number) summary.get("averageLatencyMs")).doubleValue());
        assertEquals(1L, ((Number) summary.get("failedRequests")).longValue());
    }
}
