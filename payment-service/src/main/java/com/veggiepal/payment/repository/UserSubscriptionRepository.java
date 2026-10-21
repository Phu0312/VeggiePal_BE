package com.veggiepal.payment.repository;

import com.veggiepal.payment.entity.UserSubscription;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSubscription s join fetch s.plan where s.userId = :userId")
    Optional<UserSubscription> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select s from UserSubscription s join fetch s.plan where s.expiresAt is not null and s.expiresAt <= :now")
    List<UserSubscription> findExpired(@Param("now") Instant now);
}
