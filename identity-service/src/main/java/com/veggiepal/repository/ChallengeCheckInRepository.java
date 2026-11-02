package com.veggiepal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.veggiepal.entity.ChallengeCheckIn;

public interface ChallengeCheckInRepository extends JpaRepository<ChallengeCheckIn, Long> {
    long countByChallengeId(Long challengeId);
    boolean existsByChallengeIdAndDayNumber(Long challengeId, int dayNumber);
}
