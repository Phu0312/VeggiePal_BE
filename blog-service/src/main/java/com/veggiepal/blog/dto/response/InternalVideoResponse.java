package com.veggiepal.blog.dto.response;

import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.enums.VideoSource;

public record InternalVideoResponse(Long id, ContentStatus status, VideoSource sourceType, String videoUrl) { }
