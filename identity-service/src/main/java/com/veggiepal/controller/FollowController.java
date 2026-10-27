package com.veggiepal.controller;

import com.veggiepal.dto.response.*;
import com.veggiepal.service.FollowService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/users") @RequiredArgsConstructor
public class FollowController {
    private final FollowService service;
    @PutMapping("/{id}/follow") public ApiResponse<FollowStats> follow(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {
        return response(service.follow(CurrentUser.id(jwt),id));
    }
    @DeleteMapping("/{id}/follow") public ApiResponse<FollowStats> unfollow(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {
        return response(service.unfollow(CurrentUser.id(jwt),id));
    }
    @GetMapping("/{id}/follow-stats") public ApiResponse<FollowStats> stats(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {
        return response(service.stats(CurrentUser.id(jwt),id));
    }
    @GetMapping("/me/following") public ApiResponse<PageResponse<PublicUserResponse>> myFollowing(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return response(service.list(CurrentUser.id(jwt),CurrentUser.id(jwt),true,page,size));
    }
    @GetMapping("/me/followers") public ApiResponse<PageResponse<PublicUserResponse>> myFollowers(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return response(service.list(CurrentUser.id(jwt),CurrentUser.id(jwt),false,page,size));
    }
    @GetMapping("/me/following/ids") public ApiResponse<List<Long>> followingIds(@AuthenticationPrincipal Jwt jwt) {
        return response(service.followingIds(CurrentUser.id(jwt)));
    }
    @GetMapping("/{id}/following") public ApiResponse<PageResponse<PublicUserResponse>> following(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return response(service.list(CurrentUser.id(jwt),id,true,page,size));
    }
    @GetMapping("/{id}/followers") public ApiResponse<PageResponse<PublicUserResponse>> followers(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return response(service.list(CurrentUser.id(jwt),id,false,page,size));
    }
    private static <T> ApiResponse<T> response(T result) { return ApiResponse.<T>builder().result(result).build(); }
}
