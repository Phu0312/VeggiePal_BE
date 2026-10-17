package com.veggiepal.ai.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.quota.QuotaProperties;
import com.veggiepal.ai.quota.TokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

@RestController @RequestMapping("/ai/admin/monitoring") @RequiredArgsConstructor
public class MonitoringController {
    private final UsageLogRepository repository;
    private final ModelProperties models;
    private final MonitoringService service;
    private final QuotaProperties quotas;
    private final TokenProperties tokens;
    @GetMapping("/summary") ApiResponse<Map<String,Object>> summary(@RequestParam(defaultValue="7") int days){
        int safe=Math.max(1,Math.min(days,90));Object[] aggregate=repository.aggregateSince(LocalDateTime.now().minusDays(safe)).getFirst();
        return ApiResponse.success(Map.of("periodDays",safe,"requests",aggregate[0],"promptTokens",aggregate[1],
                "completionTokens",aggregate[2],"averageLatencyMs",aggregate[3],"failedRequests",aggregate[4]));
    }
    @GetMapping("/logs") ApiResponse<List<UsageLog>> logs(){return ApiResponse.success(repository.findTop100ByOrderByCreatedAtDesc());}
    @GetMapping("/logs/search") ApiResponse<MonitoringService.UsageLogPage> search(
            @RequestParam(required=false) String feature,@RequestParam(required=false) String status,
            @RequestParam(required=false) Long userId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        return ApiResponse.success(service.search(feature,status,userId,from,to,page,size));
    }
    @GetMapping("/models") ApiResponse<Map<String,String>> models(){return ApiResponse.success(Map.of(
            "chat",models.chat(),"moderation",models.moderation(),"recommendation",models.recommendation(),
            "mealPlan",models.mealPlan(),"videoExtraction",models.videoExtraction(),"transcription",models.transcription()));}
    @GetMapping("/limits") ApiResponse<Map<String,Object>> limits(){return ApiResponse.success(Map.of(
            "guestChatRequestsPerDay",quotas.guestDailyLimit(),"memberChatRequestsPerDay",quotas.memberDailyLimit(),
            "dailyTokenLimit",tokens.dailyLimit(),"maxHistoryMessages",quotas.maxHistoryMessages(),
            "maxInputCharacters",quotas.maxInputCharacters()));}
}
