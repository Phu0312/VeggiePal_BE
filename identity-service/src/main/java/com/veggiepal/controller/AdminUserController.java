package com.veggiepal.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veggiepal.dto.request.ChangeRoleRequest;
import com.veggiepal.dto.request.LockUserRequest;
import com.veggiepal.dto.response.AdminUserResponse;
import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.PageResponse;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.service.AdminUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/** ROLE_ADMIN only, enforced by SecurityConfig's /admin/** rule (task sheet US17). */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserController {

    AdminUserService adminUserService;

    @Operation(summary = "Users, newest first; keyword matches email or full name")
    @GetMapping
    ApiResponse<PageResponse<AdminUserResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .result(adminUserService.search(keyword, status, role, page, size))
                .build();
    }

    @Operation(summary = "Lock a user (reason required); signs them out within 15 minutes")
    @PutMapping("/{id}/lock")
    ApiResponse<AdminUserResponse> lock(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid LockUserRequest request
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.lock(CurrentUser.id(jwt), id, request.getReason()))
                .build();
    }

    @Operation(summary = "Unlock a locked user")
    @PutMapping("/{id}/unlock")
    ApiResponse<AdminUserResponse> unlock(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.unlock(CurrentUser.id(jwt), id))
                .build();
    }

    @Operation(summary = "Change a user's role; revokes sessions and takes effect within 15 minutes")
    @PutMapping("/{id}/role")
    ApiResponse<AdminUserResponse> changeRole(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid ChangeRoleRequest request
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.changeRole(CurrentUser.id(jwt), id, request.getRole()))
                .build();
    }
}
