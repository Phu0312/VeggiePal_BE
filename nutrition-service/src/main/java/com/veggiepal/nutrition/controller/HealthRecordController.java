package com.veggiepal.nutrition.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.ApiResponse;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.dto.response.PageResponse;
import com.veggiepal.nutrition.service.HealthRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/nutrition/me/health-records")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Health Record", description = "Height, weight and BMI history of the current user")
public class HealthRecordController {

    HealthRecordService healthRecordService;

    @Operation(
            summary = "Record height and weight; BMI is calculated by the server"
    )
    @PostMapping
    ApiResponse<HealthRecordResponse> createRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid HealthRecordRequest request
    ) {

        return ApiResponse
                .<HealthRecordResponse>builder()
                .result(healthRecordService.createRecord(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(
            summary = "Health record history, newest first"
    )
    @GetMapping
    ApiResponse<PageResponse<HealthRecordResponse>> getRecords(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<HealthRecordResponse>>builder()
                .result(healthRecordService.getRecords(CurrentUser.id(jwt), page, size))
                .build();
    }

    @Operation(
            summary = "Latest health record (current height, weight and BMI)"
    )
    @GetMapping("/latest")
    ApiResponse<HealthRecordResponse> getLatestRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {

        return ApiResponse
                .<HealthRecordResponse>builder()
                .result(healthRecordService.getLatestRecord(CurrentUser.id(jwt)))
                .build();
    }

    @Operation(
            summary = "Correct a health record; recordedAt is kept and BMI is recalculated"
    )
    @PutMapping("/{id}")
    ApiResponse<HealthRecordResponse> updateRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid HealthRecordRequest request
    ) {

        return ApiResponse
                .<HealthRecordResponse>builder()
                .result(healthRecordService.updateRecord(CurrentUser.id(jwt), id, request))
                .build();
    }
}
