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
    @Column(name="goal_type",length=20) private String goalType;
    @Column(name="plan_json",nullable=false,columnDefinition="LONGTEXT") private String planJson;
    @Column(name="version",nullable=false) private Integer version = 1;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;

    public MealPlan(Long userId,String name,String goal,String json) {
        this(userId, name, goal, json, 1);
    }

    public MealPlan(Long userId,String name,String goal,String json, Integer version) {
        this(userId, name, goal, null, json, version);
    }

    public MealPlan(Long userId,String name,String goal,String goalType,String json) {
        this(userId, name, goal, goalType, json, 1);
    }

    private MealPlan(Long userId,String name,String goal,String goalType,String json, Integer version) {
        this.userId=userId;
        this.name=name;
        this.goal=goal;
        this.goalType=goalType;
        this.planJson=json;
        this.version=version != null ? version : 1;
        this.createdAt=LocalDateTime.now();
        this.updatedAt=createdAt;
    }

    public void replaceJson(String json) {
        this.planJson=json;
        this.updatedAt=LocalDateTime.now();
    }

    public void bumpVersion(String json) {
        this.planJson=json;
        this.version=(this.version == null ? 1 : this.version) + 1;
        this.updatedAt=LocalDateTime.now();
    }
}
