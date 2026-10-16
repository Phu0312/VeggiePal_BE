package com.veggiepal.ai.recommendation;

import java.util.List;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.recommendation.RecommendationDtos.Recommendation;
import com.veggiepal.ai.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/ai/recommendations") @RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService service;
    @GetMapping ApiResponse<List<Recommendation>> get(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) String interest,@RequestParam(defaultValue="6") int limit) {
        return ApiResponse.success(service.recommend(CurrentUser.id(jwt),interest,limit));
    }
}
