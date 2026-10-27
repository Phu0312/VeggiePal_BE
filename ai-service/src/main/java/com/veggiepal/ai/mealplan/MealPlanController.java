package com.veggiepal.ai.mealplan;

import static com.veggiepal.ai.mealplan.MealPlanDtos.*;

import java.util.List;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.common.PageResponse;
import com.veggiepal.ai.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService service;

    @PostMapping
    public ApiResponse<MealPlanResponse> generate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid GenerateRequest request) {
        return ApiResponse.success(service.generate(CurrentUser.id(jwt), jwt.getTokenValue(), request));
    }

    @GetMapping
    public ApiResponse<List<MealPlanResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.list(CurrentUser.id(jwt)));
    }

    @GetMapping("/nutrition-target")
    public ApiResponse<NutritionTargetResponse> nutritionTarget(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam GoalType goalType) {
        return ApiResponse.success(service.nutritionTarget(jwt.getTokenValue(), goalType));
    }

    // ==========================================
    // FEATURE 1: FAVORITES
    // ==========================================
    @GetMapping("/favorites")
    public ApiResponse<PageResponse<MealPlanResponse>> listFavorites(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.listFavorites(CurrentUser.id(jwt), page, size));
    }

    @GetMapping("/favorites/{id}")
    public ApiResponse<MealPlanResponse> getFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ApiResponse.success(service.getFavoriteDetail(CurrentUser.id(jwt), id));
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<MealPlanResponse> favorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ApiResponse.success(service.favorite(CurrentUser.id(jwt), id));
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> unfavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        service.unfavorite(CurrentUser.id(jwt), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/unfavorite")
    public ApiResponse<Void> unfavoritePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        service.unfavorite(CurrentUser.id(jwt), id);
        return ApiResponse.success(null);
    }

    // ==========================================
    // FEATURE 2: HEALTH METRICS UPDATE & VERSIONS
    // ==========================================
    @PostMapping("/{id}/update-from-health")
    public ApiResponse<UpdateFromHealthResponse> updateFromHealth(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody(required = false) @Valid UpdateFromHealthRequest request) {
        return ApiResponse.success(service.updateFromHealth(CurrentUser.id(jwt), id, jwt.getTokenValue(), request));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<MealPlanVersionResponse>> listVersions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ApiResponse.success(service.listVersions(CurrentUser.id(jwt), id));
    }

    @GetMapping("/{id}/versions/{version}")
    public ApiResponse<MealPlanVersionResponse> getVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @PathVariable Integer version) {
        return ApiResponse.success(service.getVersion(CurrentUser.id(jwt), id, version));
    }

    // ==========================================
    // FEATURE 3: MANUAL EDITING & PREVIEW
    // ==========================================
    @PostMapping("/{id}/edit")
    public ApiResponse<MealPlanEditResponse> manualEdit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid ManualEditRequest request) {
        return ApiResponse.success(service.manualEdit(CurrentUser.id(jwt), id, jwt.getTokenValue(), request));
    }

    @PostMapping("/{id}/preview-edit")
    public ApiResponse<MealPlanEditResponse> previewEdit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid ManualEditRequest request) {
        ManualEditRequest previewReq = new ManualEditRequest(true, request.changeSummary(), request.edits());
        return ApiResponse.success(service.manualEdit(CurrentUser.id(jwt), id, jwt.getTokenValue(), previewReq));
    }

    // ==========================================
    // EXISTING GET & REPLACE
    // ==========================================
    @GetMapping("/{id}")
    public ApiResponse<MealPlanResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ApiResponse.success(service.get(CurrentUser.id(jwt), id));
    }

    @PostMapping("/{id}/replace")
    public ApiResponse<MealPlanResponse> replace(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid ReplaceMealRequest request) {
        return ApiResponse.success(service.replace(CurrentUser.id(jwt), id, jwt.getTokenValue(), request));
    }
}
