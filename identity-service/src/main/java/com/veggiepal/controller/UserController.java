package com.veggiepal.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.veggiepal.dto.request.RegisterRequest;
import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.RegisterResponse;
import com.veggiepal.service.UserService;

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
}