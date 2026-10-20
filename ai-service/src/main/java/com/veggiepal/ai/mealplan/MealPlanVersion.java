package com.veggiepal.ai.mealplan;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_meal_plan_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_meal_plan_version", columnNames = {"meal_plan_id", "version"})
})
@Getter
@NoArgsConstructor
public class MealPlanVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 500)
    private String goal;

    @Column(name = "plan_json", nullable = false, columnDefinition = "LONGTEXT")
    private String planJson;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MealPlanVersion(MealPlan mealPlan, Integer version, String name, String goal, String planJson, String changeSummary) {
        this.mealPlan = mealPlan;
        this.version = version;
        this.name = name;
        this.goal = goal;
        this.planJson = planJson;
        this.changeSummary = changeSummary;
        this.createdAt = LocalDateTime.now();
    }
}
