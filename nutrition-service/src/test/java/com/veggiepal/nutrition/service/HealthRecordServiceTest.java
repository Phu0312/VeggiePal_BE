package com.veggiepal.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.dto.response.PageResponse;
import com.veggiepal.nutrition.entity.HealthRecord;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.HealthRecordMapper;
import com.veggiepal.nutrition.repository.HealthRecordRepository;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    static final Long USER_ID = 7L;

    @Mock
    HealthRecordRepository healthRecordRepository;

    @Spy
    HealthRecordMapper healthRecordMapper = Mappers.getMapper(HealthRecordMapper.class);

    @InjectMocks
    HealthRecordService healthRecordService;

    @Test
    void calculateBmi_roundsHalfUpToOneDecimal() {
        assertThat(HealthRecordService.calculateBmi(new BigDecimal("170"), new BigDecimal("65")))
                .isEqualByComparingTo("22.5");
        // 89.8 / 2.0² = 22.45 exactly: HALF_UP gives 22.5 (HALF_EVEN would give 22.4)
        assertThat(HealthRecordService.calculateBmi(new BigDecimal("200"), new BigDecimal("89.8")))
                .isEqualByComparingTo("22.5");
        assertThat(HealthRecordService.calculateBmi(new BigDecimal("50"), new BigDecimal("300")))
                .isEqualByComparingTo("1200.0");
    }

    @Test
    void createRecord_savesRecordForUserWithBmiAndRecordedAt() {
        HealthRecordResponse response = healthRecordService.createRecord(
                USER_ID, new HealthRecordRequest(new BigDecimal("170"), new BigDecimal("65")));

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getBmi()).isEqualByComparingTo("22.5");
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(response.getBmi()).isEqualByComparingTo("22.5");
    }

    @Test
    void getLatestRecord_returnsNewestRecord() {
        when(healthRecordRepository.findFirstByUserIdOrderByRecordedAtDescIdDesc(USER_ID))
                .thenReturn(Optional.of(record(3L)));

        assertThat(healthRecordService.getLatestRecord(USER_ID).getId()).isEqualTo(3L);
    }

    @Test
    void getLatestRecord_noRecord_throwsHealthRecordNotExisted() {
        when(healthRecordRepository.findFirstByUserIdOrderByRecordedAtDescIdDesc(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.getLatestRecord(USER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.HEALTH_RECORD_NOT_EXISTED));
    }

    @Test
    void getRecords_clampsPagingAndSortsNewestFirst() {
        when(healthRecordRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(record(3L)), invocation.getArgument(1), 1));

        PageResponse<HealthRecordResponse> page = healthRecordService.getRecords(USER_ID, -1, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(healthRecordRepository).findByUserId(eq(USER_ID), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id")));
        assertThat(page.getItems()).extracting(HealthRecordResponse::getId).containsExactly(3L);
        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getRecords_hugePage_doesNotOverflowOffset() {
        when(healthRecordRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        healthRecordService.getRecords(USER_ID, Integer.MAX_VALUE, 100);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(healthRecordRepository).findByUserId(eq(USER_ID), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getPageNumber()).isEqualTo(Integer.MAX_VALUE / 100);
        assertThat(pageable.getOffset()).isNotNegative();
    }

    @Test
    void getRecords_sizeBelowOne_usesOne() {
        when(healthRecordRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        healthRecordService.getRecords(USER_ID, 2, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(healthRecordRepository).findByUserId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void updateRecord_keepsRecordedAtAndRecalculatesBmi() {
        HealthRecord existing = record(5L);
        when(healthRecordRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));

        HealthRecordResponse response = healthRecordService.updateRecord(
                USER_ID, 5L, new HealthRecordRequest(new BigDecimal("200"), new BigDecimal("89.8")));

        assertThat(existing.getHeightCm()).isEqualByComparingTo("200");
        assertThat(existing.getWeightKg()).isEqualByComparingTo("89.8");
        assertThat(existing.getBmi()).isEqualByComparingTo("22.5");
        assertThat(existing.getRecordedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
        assertThat(response.getRecordedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
        verify(healthRecordRepository).save(existing);
    }

    @Test
    void updateRecord_notOwnedOrMissing_throwsHealthRecordNotExisted() {
        when(healthRecordRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.updateRecord(
                USER_ID, 5L, new HealthRecordRequest(new BigDecimal("170"), new BigDecimal("65"))))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.HEALTH_RECORD_NOT_EXISTED));
    }

    static HealthRecord record(Long id) {
        return HealthRecord.builder()
                .id(id)
                .userId(USER_ID)
                .heightCm(new BigDecimal("170.0"))
                .weightKg(new BigDecimal("65.0"))
                .bmi(new BigDecimal("22.5"))
                .recordedAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .build();
    }
}
