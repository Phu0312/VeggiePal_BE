package com.veggiepal.ai.mealplan;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name="ai_meal_plans") @Getter @NoArgsConstructor
public class MealPlan {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=160) private String name;
    @Column(nullable=false,length=500) private String goal;
    @Column(name="plan_json",nullable=false,columnDefinition="LONGTEXT") private String planJson;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public MealPlan(Long userId,String name,String goal,String json) { this.userId=userId;this.name=name;this.goal=goal;this.planJson=json;this.createdAt=LocalDateTime.now();this.updatedAt=createdAt; }
    public void replaceJson(String json) { this.planJson=json;this.updatedAt=LocalDateTime.now(); }
}
