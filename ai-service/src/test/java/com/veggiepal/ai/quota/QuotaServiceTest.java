package com.veggiepal.ai.quota;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.veggiepal.ai.common.AppException;
import org.junit.jupiter.api.Test;

class QuotaServiceTest {
    @Test
    void guestQuotaRejectsFourthRequest() {
        DailyQuotaRepository repository=mock(DailyQuotaRepository.class);
        DailyQuota row=new DailyQuota("guest:abc","CHAT",java.time.LocalDate.now());
        when(repository.totalTokens(anyString(),any())).thenReturn(0L);
        when(repository.findByPrincipalKeyAndFeatureAndUsageDate(anyString(),anyString(),any())).thenReturn(Optional.of(row));
        QuotaService service=new QuotaService(repository,new QuotaProperties(3,50,12,4000),new TokenProperties(50000));
        assertEquals(2,service.reserveChat("guest:abc",true));
        assertEquals(1,service.reserveChat("guest:abc",true));
        assertEquals(0,service.reserveChat("guest:abc",true));
        assertThrows(AppException.class,()->service.reserveChat("guest:abc",true));
    }

    @Test
    void tokenLimitRejectsBeforeProviderCall() {
        DailyQuotaRepository repository=mock(DailyQuotaRepository.class);
        when(repository.totalTokens(anyString(),any())).thenReturn(50000L);
        QuotaService service=new QuotaService(repository,new QuotaProperties(3,50,12,4000),new TokenProperties(50000));
        assertThrows(AppException.class,()->service.reserveChat("user:1",false));
        verify(repository,never()).findByPrincipalKeyAndFeatureAndUsageDate(anyString(),anyString(),any());
    }

    @Test
    void monthlyFeatureQuotaRejectsAfterConfiguredLimit() {
        DailyQuotaRepository repository = mock(DailyQuotaRepository.class);
        DailyQuota row = new DailyQuota("user:1", "MEAL_PLAN", java.time.LocalDate.now().withDayOfMonth(1));
        when(repository.totalTokens(anyString(), any())).thenReturn(0L);
        when(repository.findByPrincipalKeyAndFeatureAndUsageDate(anyString(), anyString(), any())).thenReturn(Optional.of(row));
        QuotaService service = new QuotaService(repository, new QuotaProperties(3, 50, 12, 4000), new TokenProperties(50000));

        service.reserveFeature("user:1", "MEAL_PLAN", QuotaPeriod.MONTHLY, 2);
        service.reserveFeature("user:1", "MEAL_PLAN", QuotaPeriod.MONTHLY, 2);
        assertThrows(AppException.class,
                () -> service.reserveFeature("user:1", "MEAL_PLAN", QuotaPeriod.MONTHLY, 2));
    }

    @Test
    void usedReadsThisMonthsRowAndIsZeroWithoutOne() {
        DailyQuotaRepository repository = mock(DailyQuotaRepository.class);
        java.time.LocalDate firstOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
        DailyQuota row = new DailyQuota("user:1", "MEAL_PLAN", firstOfMonth);
        row.reserve();
        when(repository.findByPrincipalKeyAndFeatureAndUsageDate("user:1", "MEAL_PLAN", firstOfMonth))
                .thenReturn(Optional.of(row));
        QuotaService service = new QuotaService(repository, new QuotaProperties(3, 50, 12, 4000), new TokenProperties(50000));

        assertEquals(1, service.used("user:1", "MEAL_PLAN", QuotaPeriod.MONTHLY));
        assertEquals(0, service.used("user:2", "MEAL_PLAN", QuotaPeriod.MONTHLY));
        verify(repository, never()).ensureExists(anyString(), anyString(), any());
    }
}
