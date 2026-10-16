package com.veggiepal.ai.quota;

import java.time.LocalDate;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class QuotaService {
    private final DailyQuotaRepository repository;
    private final QuotaProperties properties;
    private final TokenProperties tokenProperties;

    @Transactional(readOnly = true)
    public void checkTokenBudget(String principalKey) {
        if (repository.totalTokens(principalKey, LocalDate.now()) >= tokenProperties.dailyLimit()) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reserveChat(String principalKey, boolean guest) {
        checkTokenBudget(principalKey);
        repository.ensureExists(principalKey, "CHAT", LocalDate.now());
        DailyQuota quota = repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, "CHAT", LocalDate.now())
                .orElseThrow();
        int limit = guest ? properties.guestDailyLimit() : properties.memberDailyLimit();
        if (quota.getRequestCount() >= limit) throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        quota.reserve();
        return limit - quota.getRequestCount();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addTokens(String principalKey, String feature, int input, int output) {
        repository.ensureExists(principalKey, feature, LocalDate.now());
        DailyQuota quota = repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, feature, LocalDate.now())
                .orElseThrow();
        quota.addTokens(input, output);
    }
}
