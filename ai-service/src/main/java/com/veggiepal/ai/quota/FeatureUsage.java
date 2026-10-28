package com.veggiepal.ai.quota;

import java.time.LocalDate;

/** One feature's quota for the current period, as the FE shows it (task_be C14). */
public record FeatureUsage(String planCode, int limit, int used, int remaining,
                           LocalDate periodStart, LocalDate resetsOn) {
}
