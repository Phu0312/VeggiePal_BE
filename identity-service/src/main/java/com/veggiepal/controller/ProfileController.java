package com.veggiepal.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.dto.request.ChangePasswordRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.dto.response.BadgeResponse;
import com.veggiepal.dto.response.ChallengeProgressResponse;
import com.veggiepal.service.ProfileService;
import com.veggiepal.service.ChallengeBadgeService;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Profile", description = "Current user profile APIs")
public class ProfileController {

    ProfileService profileService;
    ChallengeBadgeService challengeBadgeService;

    @PostMapping("/challenges/7-day")
    ApiResponse<ChallengeProgressResponse> joinChallenge(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<ChallengeProgressResponse>builder()
                .result(challengeBadgeService.join(CurrentUser.id(jwt))).build();
    }

    @PostMapping("/challenges/7-day/check-ins")
    ApiResponse<ChallengeProgressResponse> checkIn(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<ChallengeProgressResponse>builder()
                .result(challengeBadgeService.checkIn(CurrentUser.id(jwt))).build();
    }

    @GetMapping("/challenges/7-day")
    ApiResponse<ChallengeProgressResponse> challengeProgress(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<ChallengeProgressResponse>builder()
                .result(challengeBadgeService.progress(CurrentUser.id(jwt))).build();
    }

    @GetMapping("/badges")
    ApiResponse<List<BadgeResponse>> badges(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<BadgeResponse>>builder()
                .result(challengeBadgeService.activeBadges(CurrentUser.id(jwt))).build();
    }

    @Operation(
            summary = "Get current user profile"
    )
    @GetMapping
    ApiResponse<UserProfileResponse> getProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse
                .<UserProfileResponse>builder()
                .result(profileService.getProfile(CurrentUser.id(jwt)))
                .build();
    }

    @Operation(
            summary = "Update current user profile; null fields are left unchanged, phone \"\" clears it"
    )
    @PatchMapping
    ApiResponse<UserProfileResponse> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateProfileRequest request
    ) {

        return ApiResponse
                .<UserProfileResponse>builder()
                .result(profileService.updateProfile(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(
            summary = "Change current user password"
    )
    @PutMapping("/password")
    ApiResponse<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ChangePasswordRequest request
    ) {

        profileService.changePassword(CurrentUser.id(jwt), request);

        return ApiResponse
                .<Void>builder()
                .build();
    }

    @Operation(
            summary = "Upload current user avatar (JPEG, PNG or WEBP, max 2MB)"
    )
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<UserProfileResponse> uploadAvatar(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) {

        return ApiResponse
                .<UserProfileResponse>builder()
                .result(profileService.uploadAvatar(CurrentUser.id(jwt), file))
                .build();
    }
}
