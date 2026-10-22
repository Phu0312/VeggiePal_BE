package com.veggiepal.ai.quota;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_daily_quotas", uniqueConstraints = @UniqueConstraint(name = "uk_ai_quota_principal_feature_date", columnNames = {"principal_key", "feature", "usage_date"}))
@Getter
@NoArgsConstructor
public class DailyQuota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "principal_key", nullable = false, length = 100) private String principalKey;
    @Column(nullable = false, length = 40) private String feature;
    @Column(name = "usage_date", nullable = false) private LocalDate usageDate;
    @Column(name = "request_count", nullable = false) private int requestCount;
    @Column(name = "input_tokens", nullable = false) private long inputTokens;
    @Column(name = "output_tokens", nullable = false) private long outputTokens;
    @Version private long version;

    public DailyQuota(String principalKey, String feature, LocalDate date) {
        this.principalKey = principalKey; this.feature = feature; this.usageDate = date;
    }
    public void reserve() { requestCount++; }
    public void refund() { if (requestCount > 0) requestCount--; }
    public void addTokens(int input, int output) { inputTokens += input; outputTokens += output; }
}
