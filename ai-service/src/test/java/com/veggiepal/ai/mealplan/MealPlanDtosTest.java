package com.veggiepal.ai.mealplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class MealPlanDtosTest {
    @Test
    void createdOnIsTheCreationDayInVietnam() {
        // 23:30 UTC on Sunday is already Monday in Vietnam.
        LocalDateTime createdAt = ZonedDateTime.of(2026, 10, 25, 23, 30, 0, 0, ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        var response = new MealPlanDtos.MealPlanResponse(1L, "n", "g", null, createdAt, createdAt);

        assertThat(response.createdOn()).hasToString("2026-10-26");
    }

    @Test
    void createdOnIsSerialized() throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var response = new MealPlanDtos.MealPlanResponse(1L, "n", "g", null,
                LocalDateTime.of(2026, 10, 26, 12, 0), LocalDateTime.of(2026, 10, 26, 12, 0));

        assertThat(mapper.readTree(mapper.writeValueAsString(response)).has("createdOn")).isTrue();
    }
}
