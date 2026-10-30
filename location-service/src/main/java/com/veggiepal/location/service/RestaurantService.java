package com.veggiepal.location.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.veggiepal.location.dto.response.RestaurantResponse;
import com.veggiepal.location.exception.AppException;
import com.veggiepal.location.exception.ErrorCode;
import com.veggiepal.location.repository.RestaurantRepository;
import com.veggiepal.location.repository.RestaurantRepository.NearbyRestaurant;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RestaurantService {

    // Task sheet US15: the search radius is capped at 10 km.
    private static final double MAX_RADIUS_KM = 10;

    // Task sheet US7's search rule (type at least 2 characters), applied to restaurant names
    private static final int MIN_KEYWORD = 2;
    private static final int MAX_KEYWORD = 100;

    RestaurantRepository restaurantRepository;

    public List<RestaurantResponse> findNearby(double lat, double lng, double radiusKm, boolean veganOnly) {

        // Written as !(inside the range) so NaN, which Spring parses from "NaN", fails too.
        // Otherwise MySQL rejects it inside the query and the client gets a 500.
        if (!(lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180)) {
            throw new AppException(ErrorCode.INVALID_COORDINATES);
        }

        if (!(radiusKm > 0 && radiusKm <= MAX_RADIUS_KM)) {
            throw new AppException(ErrorCode.INVALID_SEARCH_RADIUS);
        }

        return restaurantRepository.findNearby(lat, lng, radiusKm * 1000, veganOnly)
                .stream()
                .map(RestaurantService::toResponse)
                .toList();
    }

    public List<RestaurantResponse> search(String q, Double lat, Double lng, boolean veganOnly) {

        String keyword = q == null ? "" : q.trim();
        if (keyword.length() < MIN_KEYWORD || keyword.length() > MAX_KEYWORD) {
            throw new AppException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        // A point needs both halves; the range check matches findNearby's (and rejects NaN)
        if ((lat == null) != (lng == null)
                || (lat != null && !(lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180))) {
            throw new AppException(ErrorCode.INVALID_COORDINATES);
        }

        String likeKeyword = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return restaurantRepository.searchByName(likeKeyword, lat, lng, veganOnly)
                .stream()
                .map(RestaurantService::toResponse)
                .toList();
    }

    private static RestaurantResponse toResponse(NearbyRestaurant restaurant) {

        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .phoneNumber(restaurant.getPhoneNumber())
                .rating(restaurant.getRating())
                .veganOnly(restaurant.getVeganOnly())
                .imageUrl(restaurant.getImageUrl())
                // Null for a name search without a point
                .distanceKm(restaurant.getDistanceMeters() == null
                        ? null
                        : Math.round(restaurant.getDistanceMeters() / 10) / 100.0)
                .build();
    }
}
