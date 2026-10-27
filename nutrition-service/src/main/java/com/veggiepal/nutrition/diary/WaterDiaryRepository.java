package com.veggiepal.nutrition.diary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface WaterDiaryRepository extends JpaRepository<WaterDiary,Long> {
    Optional<WaterDiary> findByIdAndUserId(Long id, Long userId);
    List<WaterDiary> findByUserIdAndDateOrderByIdAsc(Long userId, LocalDate date);
}
