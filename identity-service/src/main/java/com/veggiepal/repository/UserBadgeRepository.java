package com.veggiepal.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.veggiepal.entity.UserBadge;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    Optional<UserBadge> findFirstByUserIdAndBadgeTypeAndRevokedAtIsNull(Long userId, String badgeType);
    List<UserBadge> findByUserIdAndRevokedAtIsNullOrderByGrantedAtDesc(Long userId);
    List<UserBadge> findByUserIdOrderByGrantedAtDesc(Long userId);
    Optional<UserBadge> findByIdAndUserId(Long id, Long userId);
}
