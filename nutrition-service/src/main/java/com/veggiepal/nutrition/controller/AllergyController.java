package com.veggiepal.nutrition.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.nutrition.dto.request.UpdateAllergiesRequest;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.dto.response.ApiResponse;
import com.veggiepal.nutrition.service.AllergyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/nutrition")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Allergy", description = "Allergen catalog and current user allergies")
public class AllergyController {

    AllergyService allergyService;

    @Operation(
            summary = "Allergen catalog, grouped by category"
    )
    @GetMapping("/allergens")
    ApiResponse<List<AllergenResponse>> getAllergens() {

        return ApiResponse
                .<List<AllergenResponse>>builder()
                .result(allergyService.getAllAllergens())
                .build();
    }

    @Operation(
            summary = "Allergies of the current user"
    )
    @GetMapping("/me/allergies")
    ApiResponse<List<AllergenResponse>> getMyAllergies(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {

        return ApiResponse
                .<List<AllergenResponse>>builder()
                .result(allergyService.getMyAllergies(CurrentUser.id(jwt)))
                .build();
    }

    @Operation(
            summary = "Replace the current user's allergies; [] clears them"
    )
    @PutMapping("/me/allergies")
    ApiResponse<List<AllergenResponse>> replaceMyAllergies(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateAllergiesRequest request
    ) {

        return ApiResponse
                .<List<AllergenResponse>>builder()
                .result(allergyService.replaceAllergies(CurrentUser.id(jwt), request))
                .build();
    }
}
