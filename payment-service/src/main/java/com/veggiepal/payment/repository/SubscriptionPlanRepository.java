package com.veggiepal.payment.repository;

import com.veggiepal.payment.entity.PlanCode;
import com.veggiepal.payment.entity.SubscriptionPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByCodeAndActiveTrue(PlanCode code);
    Optional<SubscriptionPlan> findByCode(PlanCode code);
    List<SubscriptionPlan> findAllByActiveTrueOrderByPriceAsc();
}
