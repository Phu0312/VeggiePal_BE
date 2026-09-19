package com.veggiepal.nutrition.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.nutrition.entity.HealthRecord;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Page<HealthRecord> findByUserId(Long userId, Pageable pageable);

    Optional<HealthRecord> findFirstByUserIdOrderByRecordedAtDescIdDesc(Long userId);

    Optional<HealthRecord> findByIdAndUserId(Long id, Long userId);
}
