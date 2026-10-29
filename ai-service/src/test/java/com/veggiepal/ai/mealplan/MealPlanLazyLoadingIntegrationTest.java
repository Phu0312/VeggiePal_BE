package com.veggiepal.ai.mealplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
class MealPlanLazyLoadingIntegrationTest {
    private static final Long USER_ID = 42L;
    private static final String PLAN_JSON = "{\"days\":[]}";

    @Autowired MealPlanService service;
    @Autowired MealPlanRepository mealPlans;
    @Autowired MealPlanFavoriteRepository favorites;
    @Autowired MealPlanVersionRepository versions;

    @BeforeEach
    void clean() {
        versions.deleteAll();
        favorites.deleteAll();
        mealPlans.deleteAll();
    }

    @Test
    void favoriteAndVersionReadsResolveLazyMealPlanAssociations() {
        MealPlan plan = mealPlans.saveAndFlush(new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", PLAN_JSON));
        favorites.saveAndFlush(new MealPlanFavorite(USER_ID, plan));
        versions.saveAndFlush(new MealPlanVersion(
                plan, 1, plan.getName(), plan.getGoal(), plan.getPlanJson(), "Bản khởi tạo"));

        var favoritePage = service.listFavorites(USER_ID, 0, 10);
        var favoriteDetail = service.getFavoriteDetail(USER_ID, plan.getId());
        var versionList = service.listVersions(USER_ID, plan.getId());
        var versionDetail = service.getVersion(USER_ID, plan.getId(), 1);

        assertThat(favoritePage.getItems()).singleElement().extracting(MealPlanDtos.MealPlanResponse::id)
                .isEqualTo(plan.getId());
        assertThat(favoriteDetail.id()).isEqualTo(plan.getId());
        assertThat(versionList).singleElement().extracting(MealPlanDtos.MealPlanVersionResponse::mealPlanId)
                .isEqualTo(plan.getId());
        assertThat(versionDetail.mealPlanId()).isEqualTo(plan.getId());
    }
}
