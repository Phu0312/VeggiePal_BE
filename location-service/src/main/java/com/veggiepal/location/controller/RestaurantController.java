package com.veggiepal.location.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veggiepal.location.dto.response.ApiResponse;
import com.veggiepal.location.dto.response.RestaurantResponse;
import com.veggiepal.location.service.RestaurantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Restaurant", description = "Vegetarian and vegan restaurants on the map")
public class RestaurantController {

    RestaurantService restaurantService;

    @Operation(summary = "Restaurants within radiusKm (max 10) of a point, nearest first; no login needed")
    @GetMapping("/nearby")
    ApiResponse<List<RestaurantResponse>> findNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(name = "radiusKm", defaultValue = "5") double radiusKm,
            @RequestParam(name = "veganOnly", defaultValue = "false") boolean veganOnly
    ) {

        return ApiResponse
                .<List<RestaurantResponse>>builder()
                .result(restaurantService.findNearby(lat, lng, radiusKm, veganOnly))
                .build();
    }

    @Operation(summary = "Restaurants whose name contains q (2-100 characters), at most 20; lat/lng optional, adds distanceKm; no login needed")
    @GetMapping("/search")
    ApiResponse<List<RestaurantResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "veganOnly", defaultValue = "false") boolean veganOnly
    ) {

        return ApiResponse
                .<List<RestaurantResponse>>builder()
                .result(restaurantService.search(q, lat, lng, veganOnly))
                .build();
    }
}
