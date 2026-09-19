package com.veggiepal.nutrition.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.dto.response.PageResponse;
import com.veggiepal.nutrition.entity.HealthRecord;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.HealthRecordMapper;
import com.veggiepal.nutrition.repository.HealthRecordRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HealthRecordService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id"));

    HealthRecordRepository healthRecordRepository;
    HealthRecordMapper healthRecordMapper;

    public HealthRecordResponse createRecord(Long userId, HealthRecordRequest request) {

        HealthRecord healthRecord = HealthRecord.builder()
                .userId(userId)
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .bmi(calculateBmi(request.getHeightCm(), request.getWeightKg()))
                .recordedAt(LocalDateTime.now())
                .build();

        healthRecordRepository.save(healthRecord);
        return healthRecordMapper.toHealthRecordResponse(healthRecord);
    }

    public PageResponse<HealthRecordResponse> getRecords(Long userId, int page, int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize);

        Page<HealthRecord> records = healthRecordRepository.findByUserId(
                userId,
                PageRequest.of(safePage, safeSize, NEWEST_FIRST)
        );

        return PageResponse.<HealthRecordResponse>builder()
                .items(records.getContent().stream()
                        .map(healthRecordMapper::toHealthRecordResponse)
                        .toList())
                .page(records.getNumber())
                .size(records.getSize())
                .totalElements(records.getTotalElements())
                .totalPages(records.getTotalPages())
                .build();
    }

    public HealthRecordResponse getLatestRecord(Long userId) {

        return healthRecordRepository
                .findFirstByUserIdOrderByRecordedAtDescIdDesc(userId)
                .map(healthRecordMapper::toHealthRecordResponse)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.HEALTH_RECORD_NOT_EXISTED
                        )
                );
    }

    public HealthRecordResponse updateRecord(Long userId, Long recordId, HealthRecordRequest request) {

        // Filtering by userId too: someone else's record looks exactly like a missing one
        HealthRecord healthRecord = healthRecordRepository
                .findByIdAndUserId(recordId, userId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.HEALTH_RECORD_NOT_EXISTED
                        )
                );

        healthRecord.setHeightCm(request.getHeightCm());
        healthRecord.setWeightKg(request.getWeightKg());
        healthRecord.setBmi(calculateBmi(request.getHeightCm(), request.getWeightKg()));

        healthRecordRepository.save(healthRecord);
        return healthRecordMapper.toHealthRecordResponse(healthRecord);
    }

    // BR-04: BMI = weight(kg) / height(m)^2, rounded HALF_UP to 1 decimal
    static BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {

        BigDecimal heightM = heightCm.movePointLeft(2);

        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
