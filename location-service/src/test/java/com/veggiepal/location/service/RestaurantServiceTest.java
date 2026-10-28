package com.veggiepal.location.service;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import com.veggiepal.location.dto.response.RestaurantResponse;
import com.veggiepal.location.exception.AppException;
import com.veggiepal.location.exception.ErrorCode;
import com.veggiepal.location.repository.RestaurantRepository;
import com.veggiepal.location.repository.RestaurantRepository.NearbyRestaurant;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    // Bến Thành market
    static final double LAT = 10.7725;
    static final double LNG = 106.698;

    @Mock
    RestaurantRepository restaurantRepository;

    @InjectMocks
    RestaurantService restaurantService;

    // The repository returns Spring Data projections; this builds one from plain values.
    static NearbyRestaurant shamballa(double distanceMeters) {
        return new SpelAwareProxyProjectionFactory().createProjection(NearbyRestaurant.class, Map.of(
                "id", 2L,
                "name", "Shamballa Vegetarian Restaurant",
                "address", "31 Lý Tự Trọng, P. Bến Nghé, Quận 1, TP.HCM",
                "latitude", 10.7768,
                "longitude", 106.7001,
                "phoneNumber", "0917876788",
                "rating", 4.7,
                "veganOnly", true,
                "imageUrl", "https://res.cloudinary.com/demo/image/upload/veggiepal/restaurants/restaurant-3",
                "distanceMeters", distanceMeters));
    }

    @Test
    void findNearby_convertsRadiusToMetersAndDistanceToKm() {
        when(restaurantRepository.findNearby(LAT, LNG, 2000.0, true)).thenReturn(List.of(shamballa(530.4)));

        List<RestaurantResponse> result = restaurantService.findNearby(LAT, LNG, 2, true);

        assertThat(result).singleElement().satisfies(restaurant -> {
            assertThat(restaurant.getId()).isEqualTo(2L);
            assertThat(restaurant.getName()).isEqualTo("Shamballa Vegetarian Restaurant");
            assertThat(restaurant.getAddress()).isEqualTo("31 Lý Tự Trọng, P. Bến Nghé, Quận 1, TP.HCM");
            assertThat(restaurant.getLatitude()).isEqualTo(10.7768);
            assertThat(restaurant.getLongitude()).isEqualTo(106.7001);
            assertThat(restaurant.getPhoneNumber()).isEqualTo("0917876788");
            assertThat(restaurant.getRating()).isEqualTo(4.7);
            assertThat(restaurant.getVeganOnly()).isTrue();
            assertThat(restaurant.getImageUrl())
                    .isEqualTo("https://res.cloudinary.com/demo/image/upload/veggiepal/restaurants/restaurant-3");
            assertThat(restaurant.getDistanceKm()).isEqualTo(0.53);
        });
    }

    @Test
    void findNearby_acceptsTheMaximumRadius() {
        when(restaurantRepository.findNearby(LAT, LNG, 10_000.0, false)).thenReturn(List.of());

        assertThat(restaurantService.findNearby(LAT, LNG, 10, false)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "91, 106.698",
            "-91, 106.698",
            "10.7725, 181",
            "10.7725, -181",
            "NaN, 106.698",
            "10.7725, NaN"
    })
    void findNearby_rejectsCoordinatesOutOfRange(double lat, double lng) {
        assertThatThrownBy(() -> restaurantService.findNearby(lat, lng, 5, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COORDINATES);

        verifyNoInteractions(restaurantRepository);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, 10.01, Double.NaN})
    void findNearby_rejectsRadiusOutOfRange(double radiusKm) {
        assertThatThrownBy(() -> restaurantService.findNearby(LAT, LNG, radiusKm, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SEARCH_RADIUS);

        verifyNoInteractions(restaurantRepository);
    }

    @Test
    void search_trimsTheKeywordAndPassesTheCoordinatesThrough() {
        when(restaurantRepository.searchByName("hum", LAT, LNG, true)).thenReturn(List.of(shamballa(530.4)));

        List<RestaurantResponse> result = restaurantService.search("  hum ", LAT, LNG, true);

        assertThat(result).singleElement().satisfies(r -> assertThat(r.getDistanceKm()).isEqualTo(0.53));
    }

    @Test
    void search_withoutCoordinates_hasNoDistance() {
        Map<String, Object> values = new HashMap<>(Map.of(
                "id", 21L, "name", "Hum Vegetarian Võ Văn Tần", "address", "32 Võ Văn Tần",
                "latitude", 10.7772, "longitude", 106.6912, "veganOnly", true));
        values.put("distanceMeters", null);
        NearbyRestaurant hum = new SpelAwareProxyProjectionFactory().createProjection(NearbyRestaurant.class, values);
        when(restaurantRepository.searchByName("hum", null, null, false)).thenReturn(List.of(hum));

        List<RestaurantResponse> result = restaurantService.search("hum", null, null, false);

        assertThat(result).singleElement().satisfies(r -> assertThat(r.getDistanceKm()).isNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "h", " h ", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
    void search_rejectsKeywordsOutside2To100Characters(String q) {
        assertThatThrownBy(() -> restaurantService.search(q, null, null, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SEARCH_KEYWORD);

        verifyNoInteractions(restaurantRepository);
    }

    @Test
    void search_rejectsHalfACoordinate() {
        assertThatThrownBy(() -> restaurantService.search("hum", LAT, null, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COORDINATES);

        verifyNoInteractions(restaurantRepository);
    }

    @Test
    void search_rejectsCoordinatesOutOfRange() {
        assertThatThrownBy(() -> restaurantService.search("hum", 91.0, LNG, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COORDINATES);
    }
}
