package com.veggiepal.payment.repository;

import com.veggiepal.payment.entity.PaymentOrder;
import com.veggiepal.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long>, JpaSpecificationExecutor<PaymentOrder> {
    Optional<PaymentOrder> findByIdAndUserId(Long id, Long userId);
    List<PaymentOrder> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentOrder> findAllByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(
            Long userId, Long planId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PaymentOrder o join fetch o.plan where o.id = :id and o.userId = :userId")
    Optional<PaymentOrder> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PaymentOrder o join fetch o.plan where o.orderCode = :orderCode")
    Optional<PaymentOrder> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);
}
