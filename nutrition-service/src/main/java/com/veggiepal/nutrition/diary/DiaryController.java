package com.veggiepal.nutrition.diary;

import java.time.LocalDate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.veggiepal.nutrition.controller.CurrentUser;
import com.veggiepal.nutrition.dto.response.ApiResponse;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;

@RestController @RequestMapping("/nutrition/me/diary") @RequiredArgsConstructor
public class DiaryController {
    private final DiaryService service;
    @GetMapping public ApiResponse<DailySummary> summary(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) LocalDate date) {
        return response(service.summary(CurrentUser.id(jwt),date));
    }
    @PutMapping("/meal-plans/{planId}/meals/{mealType}")
    public ApiResponse<MealEntry> confirm(@AuthenticationPrincipal Jwt jwt,@PathVariable Long planId,
            @PathVariable MealType mealType,@RequestBody @Valid ConfirmMealRequest request) {
        return response(service.confirm(CurrentUser.id(jwt),jwt.getTokenValue(),planId,mealType,request));
    }
    @DeleteMapping("/meal-plans/{planId}/meals/{mealType}")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable Long planId,
            @PathVariable MealType mealType,@RequestParam(required=false) LocalDate date) {
        service.cancel(CurrentUser.id(jwt),planId,mealType,date);return response(null);
    }
    @PostMapping("/water") public ApiResponse<WaterEntry> addWater(@AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid WaterRequest request) { return response(service.saveWater(CurrentUser.id(jwt),null,request)); }
    @PutMapping("/water/{id}") public ApiResponse<WaterEntry> updateWater(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,@RequestBody @Valid WaterRequest request) { return response(service.saveWater(CurrentUser.id(jwt),id,request)); }
    @DeleteMapping("/water/{id}") public ApiResponse<Void> deleteWater(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {
        service.deleteWater(CurrentUser.id(jwt),id);return response(null);
    }
    @PostMapping("/activities") public ApiResponse<ActivityEntry> addActivity(@AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ActivityRequest request) { return response(service.saveActivity(CurrentUser.id(jwt),null,request)); }
    @PutMapping("/activities/{id}") public ApiResponse<ActivityEntry> updateActivity(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,@RequestBody @Valid ActivityRequest request) { return response(service.saveActivity(CurrentUser.id(jwt),id,request)); }
    @DeleteMapping("/activities/{id}") public ApiResponse<Void> deleteActivity(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {
        service.deleteActivity(CurrentUser.id(jwt),id);return response(null);
    }
    private static <T> ApiResponse<T> response(T result) { return ApiResponse.<T>builder().result(result).build(); }
}
