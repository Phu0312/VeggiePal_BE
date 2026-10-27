package com.veggiepal.nutrition.diary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
import com.veggiepal.nutrition.diary.DiaryDtos.MealType;
public interface MealDiaryRepository extends JpaRepository<MealDiary,Long> {
    Optional<MealDiary> findByUserIdAndMealPlanIdAndDateAndMealType(Long userId, Long planId, LocalDate date, MealType type);
    List<MealDiary> findByUserIdAndDateOrderByIdAsc(Long userId, LocalDate date);
    long deleteByUserIdAndMealPlanIdAndDateAndMealType(Long userId, Long planId, LocalDate date, MealType type);
}
