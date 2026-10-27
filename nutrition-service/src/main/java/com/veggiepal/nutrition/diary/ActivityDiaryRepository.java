package com.veggiepal.nutrition.diary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface ActivityDiaryRepository extends JpaRepository<ActivityDiary,Long> {
    Optional<ActivityDiary> findByIdAndUserId(Long id, Long userId);
    List<ActivityDiary> findByUserIdAndDateOrderByIdAsc(Long userId, LocalDate date);
}
