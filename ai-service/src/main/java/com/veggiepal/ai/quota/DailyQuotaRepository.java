package com.veggiepal.ai.quota;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface DailyQuotaRepository extends JpaRepository<DailyQuota, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO ai_daily_quotas
                (principal_key, feature, usage_date, request_count, input_tokens, output_tokens, version)
            VALUES (:principal, :feature, :date, 0, 0, 0, 0)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int ensureExists(@Param("principal") String principal, @Param("feature") String feature,
                     @Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DailyQuota> findByPrincipalKeyAndFeatureAndUsageDate(String principalKey, String feature, LocalDate usageDate);

    @Query("select coalesce(sum(q.inputTokens + q.outputTokens),0) from DailyQuota q where q.principalKey=:principal and q.usageDate=:date")
    long totalTokens(@Param("principal") String principal,@Param("date") LocalDate date);
}
