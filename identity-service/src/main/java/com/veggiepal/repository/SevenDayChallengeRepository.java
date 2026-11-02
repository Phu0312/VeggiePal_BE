package com.veggiepal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.veggiepal.entity.SevenDayChallenge;

public interface SevenDayChallengeRepository extends JpaRepository<SevenDayChallenge, Long> {
    Optional<SevenDayChallenge> findFirstByUserIdOrderByIdDesc(Long userId);
}
