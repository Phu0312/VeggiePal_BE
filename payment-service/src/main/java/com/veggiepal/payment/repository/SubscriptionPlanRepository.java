package com.veggiepal.payment.repository;

import com.veggiepal.payment.entity.PlanCode;
import com.veggiepal.payment.entity.SubscriptionPlan;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByCodeAndActiveTrue(PlanCode code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SubscriptionPlan p where p.code = :code and p.active = true")
    Optional<SubscriptionPlan> findByCodeAndActiveTrueForUpdate(@Param("code") PlanCode code);
    Optional<SubscriptionPlan> findByCode(PlanCode code);
    List<SubscriptionPlan> findAllByActiveTrueOrderByPriceAsc();
}
