package com.veggiepal.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.veggiepal.dto.request.RegisterRequest;
import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.RegisterResponse;
import com.veggiepal.dto.request.LoginRequest;
import com.veggiepal.dto.request.GoogleLoginRequest;
import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.service.UserService;
import com.veggiepal.service.RefreshTokenService;
import com.veggiepal.dto.request.RefreshTokenRequest;
import com.veggiepal.dto.request.EmailRequest;
import com.veggiepal.dto.request.VerifyOtpRequest;
import com.veggiepal.dto.request.ResetPasswordRequest;
import com.veggiepal.dto.response.ResetTokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "User", description = "User APIs")
public class UserController {

    UserService userService;
    RefreshTokenService refreshTokenService;

    @Operation(summary = "Email a password-reset code; always succeeds, whether or not the email is registered")
    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@RequestBody @Valid EmailRequest request) {
        userService.forgotPassword(request.getEmail());
        return ApiResponse.<Void>builder()
                .message("If the email is registered, a code has been sent").build();
    }

    @Operation(summary = "Trade the emailed code for a one-use reset token (10 minutes)")
    @PostMapping("/verify-otp")
    ApiResponse<ResetTokenResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        return ApiResponse.<ResetTokenResponse>builder()
                .result(userService.verifyResetOtp(request.getEmail(), request.getOtp())).build();
    }

    @Operation(summary = "Set a new password with the reset token; signs every device out")
    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request.getResetToken(), request.getNewPassword());
        return ApiResponse.<Void>builder().build();
    }

    @Operation(summary = "Confirm the emailed code after registering; signs the user in")
    @PostMapping("/verify-email")
    ApiResponse<LoginResponse> verifyEmail(@RequestBody @Valid VerifyOtpRequest request) {
        return ApiResponse.<LoginResponse>builder()
                .result(userService.verifyEmail(request.getEmail(), request.getOtp()))
                .build();
    }

    @Operation(summary = "Send another verification code (at most one a minute); always succeeds")
    @PostMapping("/resend-verification")
    ApiResponse<Void> resendVerification(@RequestBody @Valid EmailRequest request) {
        userService.resendVerification(request.getEmail());
        return ApiResponse.<Void>builder()
                .message("If the account is waiting for verification, a new code has been sent")
                .build();
    }

    @Operation(
            summary = "Register new user"
    )
    @PostMapping("/register")
    ApiResponse<RegisterResponse> createUser(@RequestBody @Valid RegisterRequest request) {

        return ApiResponse
                .<RegisterResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @Operation(
            summary = "Login user"
    )
    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {

        return ApiResponse
                .<LoginResponse>builder()
                .result(userService.login(request))
                .build();
    }

    @Operation(
            summary = "Login with a Google ID token"
    )
    @PostMapping("/google")
    ApiResponse<LoginResponse> loginWithGoogle(@RequestBody @Valid GoogleLoginRequest request) {

        return ApiResponse
                .<LoginResponse>builder()
                .result(userService.loginWithGoogle(request.getIdToken()))
                .build();
    }

    @Operation(summary = "Trade a refresh token for a new access + refresh pair (the old one stops working)")
    @PostMapping("/refresh")
    ApiResponse<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {

        return ApiResponse
                .<LoginResponse>builder()
                .result(userService.refresh(request.getRefreshToken()))
                .build();
    }

    @Operation(summary = "Sign this device out (revokes its refresh token); always succeeds")
    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody RefreshTokenRequest request) {

        refreshTokenService.revoke(request.getRefreshToken());
        return ApiResponse.<Void>builder().build();
    }
}
