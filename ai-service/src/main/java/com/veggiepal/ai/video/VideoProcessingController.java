package com.veggiepal.ai.video;

import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.security.CurrentUser;
import com.veggiepal.ai.video.VideoDtos.VideoJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/ai/videos") @RequiredArgsConstructor
public class VideoProcessingController {
    private final VideoProcessingService service;
    @PostMapping(value="/process",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ApiResponse<VideoJobResponse> process(@AuthenticationPrincipal Jwt jwt,@RequestPart("file")MultipartFile file){return ApiResponse.success(service.process(CurrentUser.id(jwt),file));}
    @GetMapping("/{id}") ApiResponse<VideoJobResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return ApiResponse.success(service.get(CurrentUser.id(jwt),id));}
}
