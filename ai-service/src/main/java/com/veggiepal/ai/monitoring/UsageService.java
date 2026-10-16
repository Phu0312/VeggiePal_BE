package com.veggiepal.ai.monitoring;

import com.veggiepal.ai.groq.GroqClient.GroqResult;
import com.veggiepal.ai.quota.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class UsageService {
    private final UsageLogRepository repository;
    private final QuotaService quotaService;
    public void checkBudget(String principal) { quotaService.checkTokenBudget(principal); }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(Long userId, String principal, String feature, String model, GroqResult result, long latency) {
        repository.save(new UsageLog(userId, principal, feature, model, "SUCCESS", result.promptTokens(), result.completionTokens(), latency, null));
        quotaService.addTokens(principal, feature, result.promptTokens(), result.completionTokens());
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(Long userId, String principal, String feature, String model, long latency, Exception error) {
        repository.save(new UsageLog(userId, principal, feature, model, "FAILED", 0, 0, latency, error.getMessage()));
    }
}
