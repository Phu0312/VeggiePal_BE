package com.veggiepal.ai.mealplan;

import static com.veggiepal.ai.mealplan.MealPlanDtos.*;
import java.util.List;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/ai/meal-plans") @RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanService service;
    @PostMapping ApiResponse<MealPlanResponse> generate(@AuthenticationPrincipal Jwt jwt,@RequestBody @Valid GenerateRequest request){return ApiResponse.success(service.generate(CurrentUser.id(jwt),jwt.getTokenValue(),request));}
    @GetMapping ApiResponse<List<MealPlanResponse>> list(@AuthenticationPrincipal Jwt jwt){return ApiResponse.success(service.list(CurrentUser.id(jwt)));}
    @GetMapping("/{id}") ApiResponse<MealPlanResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return ApiResponse.success(service.get(CurrentUser.id(jwt),id));}
    @PostMapping("/{id}/replace") ApiResponse<MealPlanResponse> replace(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,@RequestBody @Valid ReplaceMealRequest request){
        return ApiResponse.success(service.replace(CurrentUser.id(jwt),id,jwt.getTokenValue(),request));}
}
