package com.veggiepal.blog.controller;

import com.veggiepal.blog.configuration.InternalServiceToken;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.InternalVideoResponse;
import com.veggiepal.blog.entity.Video;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/videos")
@RequiredArgsConstructor
public class InternalVideoController {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final VideoRepository videos;
    private final InternalServiceToken tokens;

    @GetMapping("/{id}")
    ApiResponse<InternalVideoResponse> get(
            @PathVariable Long id,
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String token) {
        tokens.require(token);
        Video video = videos.findById(id).orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_EXISTED));
        return ApiResponse.<InternalVideoResponse>builder()
                .result(new InternalVideoResponse(video.getId(), video.getStatus(), video.getSourceType(),
                        video.getVideoUrl()))
                .build();
    }
}
