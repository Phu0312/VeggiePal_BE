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
        checkTokenBudgetOnDate(principalKey, QuotaCalendar.today());
    }

    private void checkTokenBudgetOnDate(String principalKey, LocalDate date) {
        if (repository.totalTokens(principalKey, date) >= tokenProperties.dailyLimit()) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reserveChat(String principalKey, boolean guest) {
        LocalDate today = QuotaCalendar.today();
        checkTokenBudgetOnDate(principalKey, today);
        repository.ensureExists(principalKey, "CHAT", today);
        DailyQuota quota = repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, "CHAT", today)
                .orElseThrow();
        int limit = guest ? properties.guestDailyLimit() : properties.memberDailyLimit();
        if (quota.getRequestCount() >= limit) throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        quota.reserve();
        return limit - quota.getRequestCount();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reserveFeature(String principalKey, String feature, QuotaPeriod period, int limit) {
        return reserveFeatureOnDate(principalKey, feature, period, limit, QuotaCalendar.today());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reserveFeatureOnDate(String principalKey, String feature, QuotaPeriod period, int limit, LocalDate today) {
        checkTokenBudgetOnDate(principalKey, today);
        LocalDate periodDate = period == QuotaPeriod.MONTHLY ? today.withDayOfMonth(1) : today;
        repository.ensureExists(principalKey, feature, periodDate);
        DailyQuota quota = repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, feature, periodDate)
                .orElseThrow();
        if (limit <= 0 || quota.getRequestCount() >= limit) throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        quota.reserve();
        return limit - quota.getRequestCount();
    }

    /** Read-only twin of reserveFeature: the same row, never created here. */
    @Transactional(readOnly = true)
    public int used(String principalKey, String feature, QuotaPeriod period) {
        return usedOnDate(principalKey, feature, period, QuotaCalendar.today());
    }

    @Transactional(readOnly = true)
    public int usedOnDate(String principalKey, String feature, QuotaPeriod period, LocalDate today) {
        LocalDate periodDate = period == QuotaPeriod.MONTHLY ? today.withDayOfMonth(1) : today;
        return repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, feature, periodDate)
                .map(DailyQuota::getRequestCount)
                .orElse(0);
    }

    @Transactional
    public void refundFeature(String principalKey, String feature, QuotaPeriod period) {
        LocalDate today = QuotaCalendar.today();
        LocalDate periodDate = period == QuotaPeriod.MONTHLY ? today.withDayOfMonth(1) : today;
        repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, feature, periodDate)
                .ifPresent(DailyQuota::refund);
    }

    @Transactional
    public void refundFeature(String principalKey, String feature, LocalDate usageDate) {
        repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, feature, usageDate)
                .ifPresent(DailyQuota::refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addTokens(String principalKey, String feature, int input, int output) {
        LocalDate today = QuotaCalendar.today();
        repository.ensureExists(principalKey, feature, today);
        DailyQuota quota = repository.findByPrincipalKeyAndFeatureAndUsageDate(principalKey, feature, today)
                .orElseThrow();
        quota.addTokens(input, output);
    }
}
