package com.veggiepal.nutrition.diary;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;

@Entity @Table(name="meal_diary", uniqueConstraints=@UniqueConstraint(name="uk_meal_diary_plan_slot",
        columnNames={"user_id","meal_plan_id","entry_date","meal_type"}),
        indexes=@Index(name="idx_meal_diary_user_date",columnList="user_id,entry_date"))
@Getter @Setter @NoArgsConstructor
public class MealDiary {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="entry_date",nullable=false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Source source;
    @Column(name="meal_plan_id") private Long mealPlanId;
    @Enumerated(EnumType.STRING) @Column(name="meal_type",nullable=false,length=20) private MealType mealType;
    @Column(name="recipe_id") private Long recipeId;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false,precision=6,scale=2) private BigDecimal servings;
    @Column(nullable=false,precision=16,scale=4) private BigDecimal calories;
    @Column(nullable=false,precision=16,scale=4) private BigDecimal protein;
    @Column(nullable=false,precision=16,scale=4) private BigDecimal carbohydrate;
    @Column(nullable=false,precision=16,scale=4) private BigDecimal fat;
    @Column(name="recorded_at",nullable=false,updatable=false) private Instant recordedAt;

    public MealEntry view() {
        return new MealEntry(id,date,source,mealPlanId,mealType,recipeId,title,servings,
                new Nutrition(calories,protein,carbohydrate,fat),recordedAt);
    }
}
