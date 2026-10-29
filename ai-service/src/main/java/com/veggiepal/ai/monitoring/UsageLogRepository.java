package com.veggiepal.ai.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long>, JpaSpecificationExecutor<UsageLog> {
    List<UsageLog> findTop100ByOrderByCreatedAtDesc();
    @Query("select count(u), coalesce(sum(u.promptTokens),0), coalesce(sum(u.completionTokens),0), "
            + "coalesce(avg(u.latencyMs),0), coalesce(sum(case when u.status='FAILED' then 1 else 0 end),0) "
            + "from UsageLog u where u.createdAt >= :since and (:excluded is null or u.feature <> :excluded)")
    // List<Object[]>, not Object[]: Spring Data wraps a single multi-column row as Object[]{Object[]{...}}.
    List<Object[]> aggregateSince(@Param("since") LocalDateTime since, @Param("excluded") String excludedFeature);
}
