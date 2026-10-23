package com.veggiepal.ai.mealplan;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.quota.PremiumQuotaService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

// Goes through MockMvc on purpose: the plan is written by Spring MVC's Jackson 3 converter,
// which service-level tests never run.
class MealPlanControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listReturnsStoredPlanDaysAsJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MealPlanRepository repository = mock(MealPlanRepository.class);
        String planJson = "{\"days\":[" + String.join(",", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
                "FRIDAY", "SATURDAY", "SUNDAY").stream().map(day -> "{\"day\":\"" + day + "\",\"meals\":[]}").toList()) + "]}";
        when(repository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(new MealPlan(7L, "Tuần này", "Ăn lành mạnh", planJson)));
        MealPlanService service = new MealPlanService(repository, mock(RestClient.class), "http://recipe",
                "http://nutrition", mock(GroqClient.class), new ModelProperties("c", "m", "r", "p", "v", "w"),
                mapper, mock(UsageService.class), new MealPlanValidator(mapper), mock(PremiumQuotaService.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MealPlanController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").claim("userId", 7L).issuedAt(Instant.now()).build()));

        mockMvc.perform(get("/ai/meal-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].plan.days.length()").value(7))
                .andExpect(jsonPath("$.result[0].plan.days[0].day").value("MONDAY"));
    }

    private MockMvc mockMvcFor(MealPlanService service) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").claim("userId", 7L).issuedAt(Instant.now()).build()));
        return MockMvcBuilders.standaloneSetup(new MealPlanController(service))
                .setControllerAdvice(new com.veggiepal.ai.common.GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void nutritionTargetReturnsTheComputedTarget() throws Exception {
        MealPlanService service = mock(MealPlanService.class);
        when(service.nutritionTarget("token", GoalType.GAIN)).thenReturn(new MealPlanDtos.NutritionTargetResponse(
                GoalType.GAIN, 1805, 2482, 2854, new MealPlanDtos.NutritionTarget(
                        java.math.BigDecimal.valueOf(2569), java.math.BigDecimal.valueOf(3140),
                        null, null, null, null, null, null)));

        mockMvcFor(service).perform(get("/ai/meal-plans/nutrition-target").param("goalType", "GAIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.goalType").value("GAIN"))
                .andExpect(jsonPath("$.result.bmr").value(1805))
                .andExpect(jsonPath("$.result.targetCalories").value(2854))
                .andExpect(jsonPath("$.result.target.minCalories").value(2569));
    }

    @Test
    void nutritionTargetWithoutGoalTypeIs400() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(get("/ai/meal-plans/nutrition-target"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }

    @Test
    void nutritionTargetWithUnknownGoalTypeIs400() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(get("/ai/meal-plans/nutrition-target").param("goalType", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }

    @Test
    void generateWithoutGoalTypeIs400() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/ai/meal-plans").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tuần này\",\"healthGoal\":\"Tăng cơ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }

    @Test
    void generateWithUnknownGoalTypeIs400NotA500() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/ai/meal-plans").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tuần này\",\"healthGoal\":\"Tăng cơ\",\"goalType\":\"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }
}
