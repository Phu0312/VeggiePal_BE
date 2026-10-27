package com.veggiepal.nutrition.diary;

import java.math.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veggiepal.nutrition.exception.*;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;

@Service
public class DiaryService {
    private final MealDiaryRepository meals;
    private final WaterDiaryRepository water;
    private final ActivityDiaryRepository activities;
    private final MealDiaryWriter writer;
    private final MealPlanSnapshotClient plans;
    private final Clock clock;
    public DiaryService(MealDiaryRepository meals, WaterDiaryRepository water, ActivityDiaryRepository activities,
            MealDiaryWriter writer, MealPlanSnapshotClient plans, @Qualifier("diaryClock") Clock clock) {
        this.meals=meals;this.water=water;this.activities=activities;this.writer=writer;this.plans=plans;this.clock=clock;
    }
    LocalDate date(LocalDate requested) { return requested==null?LocalDate.now(clock):requested; }
    public MealEntry confirm(Long userId,String token,Long planId,MealType mealType,ConfirmMealRequest request) {
        LocalDate day=date(request.date());
        var existing=meals.findByUserIdAndMealPlanIdAndDateAndMealType(userId,planId,day,mealType);
        if(existing.isPresent()) return existing.get().view();
        MealSnapshot snapshot=plans.fetch(token,planId,day,mealType);
        BigDecimal servings=request.servings()==null?snapshot.plannedServings():request.servings();
        if(servings.signum()<=0 || servings.compareTo(new BigDecimal("100"))>0)
            throw new AppException(ErrorCode.INVALID_DIARY_SERVINGS);
        var entry=new MealDiary(); entry.setUserId(userId);entry.setDate(day);entry.setSource(Source.MEAL_PLAN);
        entry.setMealPlanId(planId);entry.setMealType(mealType);entry.setRecipeId(snapshot.recipeId());
        entry.setTitle(snapshot.title());entry.setServings(servings);entry.setRecordedAt(clock.instant());
        entry.setCalories(scale(snapshot.nutrition().calories(),servings,snapshot.plannedServings()));
        entry.setProtein(scale(snapshot.nutrition().protein(),servings,snapshot.plannedServings()));
        entry.setCarbohydrate(scale(snapshot.nutrition().carbohydrate(),servings,snapshot.plannedServings()));
        entry.setFat(scale(snapshot.nutrition().fat(),servings,snapshot.plannedServings()));
        try { return writer.insert(entry).view(); }
        catch(DataIntegrityViolationException e) {
            // Only the duplicate slot is idempotent. Other database failures still propagate as 500.
            return meals.findByUserIdAndMealPlanIdAndDateAndMealType(userId,planId,day,mealType)
                    .map(MealDiary::view).orElseThrow(() -> e);
        }
    }
    private BigDecimal scale(BigDecimal n,BigDecimal servings,BigDecimal planned) {
        return n.multiply(servings).divide(planned,4,RoundingMode.HALF_UP);
    }
    @Transactional public void cancel(Long userId,Long planId,MealType mealType,LocalDate requested) {
        meals.deleteByUserIdAndMealPlanIdAndDateAndMealType(userId,planId,date(requested),mealType);
    }
    @Transactional(readOnly=true) public DailySummary summary(Long userId,LocalDate requested) {
        LocalDate day=date(requested);
        var mealEntries=meals.findByUserIdAndDateOrderByIdAsc(userId,day).stream().map(MealDiary::view).toList();
        var waterEntries=water.findByUserIdAndDateOrderByIdAsc(userId,day).stream().map(WaterDiary::view).toList();
        var activityEntries=activities.findByUserIdAndDateOrderByIdAsc(userId,day).stream().map(ActivityDiary::view).toList();
        Nutrition total=mealEntries.stream().map(MealEntry::nutrition).reduce(Nutrition.zero(),Nutrition::plus);
        return new DailySummary(day,DiaryConfig.TIMEZONE,mealEntries,total,waterEntries,
                waterEntries.stream().mapToLong(WaterEntry::milliliters).sum(),activityEntries,
                activityEntries.stream().mapToLong(ActivityEntry::minutes).sum());
    }
    @Transactional public WaterEntry saveWater(Long userId,Long id,WaterRequest request) {
        WaterDiary entry=id==null?new WaterDiary():water.findByIdAndUserId(id,userId)
                .orElseThrow(() -> new AppException(ErrorCode.DIARY_ENTRY_NOT_FOUND));
        entry.setUserId(userId);entry.setDate(date(request.date()));entry.setMilliliters(request.milliliters());
        return water.save(entry).view();
    }
    @Transactional public void deleteWater(Long userId,Long id) {
        WaterDiary entry=water.findByIdAndUserId(id,userId)
                .orElseThrow(() -> new AppException(ErrorCode.DIARY_ENTRY_NOT_FOUND));
        water.delete(entry);
    }
    @Transactional public ActivityEntry saveActivity(Long userId,Long id,ActivityRequest request) {
        ActivityDiary entry=id==null?new ActivityDiary():activities.findByIdAndUserId(id,userId)
                .orElseThrow(() -> new AppException(ErrorCode.DIARY_ENTRY_NOT_FOUND));
        entry.setUserId(userId);entry.setDate(date(request.date()));
        entry.setActivityType(request.activityType().trim());entry.setMinutes(request.minutes());
        return activities.save(entry).view();
    }
    @Transactional public void deleteActivity(Long userId,Long id) {
        ActivityDiary entry=activities.findByIdAndUserId(id,userId)
                .orElseThrow(() -> new AppException(ErrorCode.DIARY_ENTRY_NOT_FOUND));
        activities.delete(entry);
    }
}
