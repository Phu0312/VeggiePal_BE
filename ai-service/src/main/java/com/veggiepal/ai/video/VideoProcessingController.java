package com.veggiepal.ai.video;

import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.security.CurrentUser;
import com.veggiepal.ai.video.VideoDtos.VideoJobResponse;
import com.veggiepal.ai.video.VideoDtos.ExistingVideoRequest;
import com.veggiepal.ai.video.VideoDtos.SharedVideoRecipeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/ai/videos") @RequiredArgsConstructor
public class VideoProcessingController {
    private final VideoProcessingService service;
    @PostMapping(value="/process",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<VideoJobResponse>> process(@AuthenticationPrincipal Jwt jwt,@RequestPart("file")MultipartFile file){
        return ResponseEntity.accepted().body(ApiResponse.success(service.process(CurrentUser.id(jwt),file)));
    }
    @PostMapping("/process-existing")
    ResponseEntity<ApiResponse<VideoJobResponse>> processExisting(
            @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid ExistingVideoRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                service.processExisting(CurrentUser.id(jwt), request.videoId())));
    }
    @GetMapping("/by-video/{videoId}")
    ApiResponse<SharedVideoRecipeResponse> byVideo(@PathVariable Long videoId) {
        return ApiResponse.success(service.sharedResult(videoId));
    }
    @GetMapping ApiResponse<com.veggiepal.ai.common.PageResponse<VideoJobResponse>> history(
            @AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        return ApiResponse.success(service.history(CurrentUser.id(jwt),page,size));
    }
    @GetMapping("/{id}") ApiResponse<VideoJobResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return ApiResponse.success(service.get(CurrentUser.id(jwt),id));}
}
