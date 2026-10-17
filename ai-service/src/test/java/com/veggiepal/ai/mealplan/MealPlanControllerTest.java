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
                mapper, mock(UsageService.class), new MealPlanValidator(mapper));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MealPlanController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").claim("userId", 7L).issuedAt(Instant.now()).build()));

        mockMvc.perform(get("/ai/meal-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].plan.days.length()").value(7))
                .andExpect(jsonPath("$.result[0].plan.days[0].day").value("MONDAY"));
    }
}
