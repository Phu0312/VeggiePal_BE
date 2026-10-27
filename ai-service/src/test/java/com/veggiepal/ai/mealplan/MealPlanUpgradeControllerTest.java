package com.veggiepal.ai.mealplan;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MealPlanUpgradeControllerTest {

    private MealPlanService service;
    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        service = mock(MealPlanService.class);
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new MealPlanController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(Jwt.withTokenValue("mock-jwt-token")
                        .header("alg", "HS256")
                        .claim("userId", 42L)
                        .issuedAt(Instant.now())
                        .build())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void favoriteEndpoint_returnsSuccess() throws Exception {
        MealPlanDtos.MealPlanResponse res = new MealPlanDtos.MealPlanResponse(
                10L, "Thực đơn tuần", "Tăng cơ", List.of(), LocalDateTime.now(), LocalDateTime.now(), 1, true, null
        );
        when(service.favorite(42L, 10L)).thenReturn(res);

        mockMvc.perform(post("/ai/meal-plans/10/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.isFavorite").value(true));
    }

    @Test
    void unfavoriteEndpoint_returnsSuccess() throws Exception {
        doNothing().when(service).unfavorite(42L, 10L);

        mockMvc.perform(delete("/ai/meal-plans/10/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        mockMvc.perform(post("/ai/meal-plans/10/unfavorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void listFavoritesEndpoint_returnsPagedResponse() throws Exception {
        MealPlanDtos.MealPlanResponse item = new MealPlanDtos.MealPlanResponse(
                10L, "Thực đơn tuần", "Tăng cơ", List.of(), LocalDateTime.now(), LocalDateTime.now(), 1, true, null
        );
        PageResponse<MealPlanDtos.MealPlanResponse> pageRes = PageResponse.<MealPlanDtos.MealPlanResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(service.listFavorites(42L, 0, 10)).thenReturn(pageRes);

        mockMvc.perform(get("/ai/meal-plans/favorites?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].id").value(10))
                .andExpect(jsonPath("$.result.content[0].isFavorite").value(true))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].id").value(10))
                .andExpect(jsonPath("$.result.first").doesNotExist())
                .andExpect(jsonPath("$.result.last").doesNotExist());
    }

    @Test
    void getFavoriteDetailEndpoint_returnsSuccess() throws Exception {
        MealPlanDtos.MealPlanResponse item = new MealPlanDtos.MealPlanResponse(
                10L, "Thực đơn tuần", "Tăng cơ", List.of(), LocalDateTime.now(), LocalDateTime.now(), 1, true, null
        );
        when(service.getFavoriteDetail(42L, 10L)).thenReturn(item);

        mockMvc.perform(get("/ai/meal-plans/favorites/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.isFavorite").value(true));
    }

    @Test
    void updateFromHealthEndpoint_returnsResponse() throws Exception {
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                BigDecimal.valueOf(1800), BigDecimal.valueOf(2200),
                BigDecimal.valueOf(70), BigDecimal.valueOf(120),
                BigDecimal.valueOf(200), BigDecimal.valueOf(300),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70)
        );
        MealPlanDtos.UpdateFromHealthResponse healthRes = new MealPlanDtos.UpdateFromHealthResponse(
                false, "Cần xác nhận", target, "currentPlan", "proposedPlan", null
        );
        when(service.updateFromHealth(eq(42L), eq(10L), anyString(), any())).thenReturn(healthRes);

        mockMvc.perform(post("/ai/meal-plans/10/update-from-health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.confirmed").value(false))
                .andExpect(jsonPath("$.result.recalculatedTarget.minCalories").value(1800));
    }

    @Test
    void manualEditEndpoint_returnsResponse() throws Exception {
        MealPlanDtos.MealPlanEditResponse editRes = new MealPlanDtos.MealPlanEditResponse(
                false, "Chỉnh sửa thành công", "planNode", null
        );
        when(service.manualEdit(eq(42L), eq(10L), anyString(), any())).thenReturn(editRes);

        String body = """
                {
                    "preview": false,
                    "changeSummary": "Đổi món thứ 2",
                    "edits": [
                        {
                            "day": "MONDAY",
                            "mealType": "DINNER",
                            "action": "REPLACE",
                            "recipeId": 12,
                            "servingMultiplier": 1.5,
                            "note": "Canh chua"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/ai/meal-plans/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.preview").value(false))
                .andExpect(jsonPath("$.result.message").value("Chỉnh sửa thành công"));
    }

    @Test
    void previewEditEndpoint_returnsPreview() throws Exception {
        MealPlanDtos.MealPlanEditResponse previewRes = new MealPlanDtos.MealPlanEditResponse(
                true, "Bản xem trước", "planNode", null
        );
        when(service.manualEdit(eq(42L), eq(10L), anyString(), any())).thenReturn(previewRes);

        String body = """
                {
                    "edits": [
                        {
                            "day": "MONDAY",
                            "mealType": "BREAKFAST",
                            "servingMultiplier": 1.2
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/ai/meal-plans/10/preview-edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.preview").value(true));
    }

    @Test
    void versionsEndpoints_returnVersionsListAndDetail() throws Exception {
        MealPlanDtos.MealPlanVersionResponse v1 = new MealPlanDtos.MealPlanVersionResponse(
                1L, 10L, 1, "Thực đơn", "Tăng cân", "plan", "Khởi tạo", LocalDateTime.now()
        );
        when(service.listVersions(42L, 10L)).thenReturn(List.of(v1));
        when(service.getVersion(42L, 10L, 1)).thenReturn(v1);

        mockMvc.perform(get("/ai/meal-plans/10/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].version").value(1));

        mockMvc.perform(get("/ai/meal-plans/10/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.version").value(1))
                .andExpect(jsonPath("$.result.changeSummary").value("Khởi tạo"));
    }
}
