package com.veggiepal.blog.feed;
import com.veggiepal.blog.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/blogs/feed/following") @RequiredArgsConstructor
public class FollowingFeedController {
    private final FollowingFeedService service;
    @GetMapping public ApiResponse<PageResponse<BlogSummaryResponse>> get(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.<PageResponse<BlogSummaryResponse>>builder().result(service.get(jwt.getTokenValue(),page,size)).build();
    }
}
