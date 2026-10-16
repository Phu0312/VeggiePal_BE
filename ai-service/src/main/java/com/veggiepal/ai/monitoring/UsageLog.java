package com.veggiepal.ai.monitoring;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_usage_logs")
@Getter
@NoArgsConstructor
public class UsageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "principal_key", nullable = false, length = 100) private String principalKey;
    @Column(nullable = false, length = 40) private String feature;
    @Column(nullable = false, length = 120) private String model;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "prompt_tokens", nullable = false) private int promptTokens;
    @Column(name = "completion_tokens", nullable = false) private int completionTokens;
    @Column(name = "latency_ms", nullable = false) private long latencyMs;
    @Column(name = "error_message", length = 500) private String errorMessage;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public UsageLog(Long userId, String principalKey, String feature, String model, String status,
                    int promptTokens, int completionTokens, long latencyMs, String errorMessage) {
        this.userId=userId; this.principalKey=principalKey; this.feature=feature; this.model=model; this.status=status;
        this.promptTokens=promptTokens; this.completionTokens=completionTokens; this.latencyMs=latencyMs;
        this.errorMessage=errorMessage == null ? null : errorMessage.substring(0, Math.min(500, errorMessage.length()));
        this.createdAt=LocalDateTime.now();
    }
}
