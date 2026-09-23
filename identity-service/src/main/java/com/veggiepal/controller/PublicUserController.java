package com.veggiepal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.service.PublicUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public User", description = "Display names and avatars shown next to public content")
public class PublicUserController {

    PublicUserService publicUserService;

    @Operation(summary = "Resolve up to 50 author ids into display names and avatars")
    @GetMapping("/batch")
    ApiResponse<List<PublicUserResponse>> getPublicUsers(
            @RequestParam("ids") List<Long> ids
    ) {

        return ApiResponse
                .<List<PublicUserResponse>>builder()
                .result(publicUserService.getPublicUsers(ids))
                .build();
    }
}
