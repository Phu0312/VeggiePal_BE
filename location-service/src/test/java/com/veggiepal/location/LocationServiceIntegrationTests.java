package com.veggiepal.location;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.veggiepal.location.entity.Restaurant;
import com.veggiepal.location.repository.RestaurantRepository;

/**
 * End-to-end tests against a real MySQL schema seeded by {@code data.sql}.
 *
 * <p>The nearby search is a native query: Hibernate does not check it at startup and a mocked
 * repository never runs it, so this is the only test that catches a swapped
 * {@code point(lng, lat)}, a missing {@code ORDER BY}, a metre/kilometre mix-up or an alias
 * that does not match the projection. Expected values are pinned to the seed, measured from
 * Bến Thành market.
 *
 * <p>Runs against its own schema so a test run never leaves rows in {@code veggiepal_location}.
 * Needs Docker up ({@code docker compose up -d mysql}); it is excluded from the fast loop for
 * that reason, and {@code CLAUDE.md} records that it is a required gate before committing a
 * change to the entity, the repository or {@code data.sql}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_location_it?createDatabaseIfNotExist=true",
        "spring.jpa.show-sql=false"
})
class LocationServiceIntegrationTests {

    // Bến Thành market
    private static final String CENTER_LAT = "10.7725";
    private static final String CENTER_LNG = "106.6980";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RestaurantRepository restaurantRepository;

    @Test
    void nearby_returnsRestaurantsWithinRadiusNearestFirst() throws Exception {
        mockMvc.perform(nearby(CENTER_LAT, CENTER_LNG).param("radiusKm", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].id", contains(2, 3, 6, 26, 4, 22, 21)))
                .andExpect(jsonPath("$.result[0].name").value("Shamballa Vegetarian Restaurant"))
                .andExpect(jsonPath("$.result[0].phoneNumber").value("0917876788"))
                .andExpect(jsonPath("$.result[0].veganOnly").value(true))
                // Seeded by data.sql: Shamballa is a plain restaurant, and id 2 rotates to restaurant-3.
                .andExpect(jsonPath("$.result[0].imageUrl").value(
                        "https://res.cloudinary.com/r3ubxlz3/image/upload/c_fill,w_600,h_400,f_auto,q_auto/veggiepal/restaurants/restaurant-3"))
                .andExpect(jsonPath("$.result[0].distanceKm", closeTo(0.53, 0.02)))
                .andExpect(jsonPath("$.result[6].distanceKm", lessThanOrEqualTo(1.0)));
    }

    @Test
    void nearby_radiusIsTheCutOff() throws Exception {
        // In the seed, id 20 is 7.14 km from Bến Thành and id 19 is 11.52 km; nothing lies between.
        mockMvc.perform(nearby(CENTER_LAT, CENTER_LNG).param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].id", hasItem(20)))
                .andExpect(jsonPath("$.result[*].id", not(hasItem(19))));
    }

    @Test
    void nearby_veganOnlyDropsVegetarianRestaurants() throws Exception {
        // Every seeded row is vegan, so add a vegetarian one in Hà Nội, far from the whole seed.
        Restaurant vegetarian = restaurantRepository.save(Restaurant.builder()
                .name("IT vegetarian " + UUID.randomUUID())
                .address("Hoàn Kiếm, Hà Nội")
                .latitude(21.0285)
                .longitude(105.8542)
                .veganOnly(false)
                .build());
        int id = vegetarian.getId().intValue();

        try {
            mockMvc.perform(nearby("21.0285", "105.8542").param("radiusKm", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[*].id", hasItem(id)));

            mockMvc.perform(nearby("21.0285", "105.8542").param("radiusKm", "1").param("veganOnly", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[*].id", not(hasItem(id))));
        } finally {
            restaurantRepository.delete(vegetarian);
        }
    }

    @Test
    void nearby_veganOnlyKeepsVeganRestaurants() throws Exception {
        // Every seeded row is vegan, so veganOnly=true must return the same seven as the 1 km search.
        mockMvc.perform(nearby(CENTER_LAT, CENTER_LNG).param("radiusKm", "1").param("veganOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].id", contains(2, 3, 6, 26, 4, 22, 21)));
    }

    private static MockHttpServletRequestBuilder nearby(String lat, String lng) {
        return get("/restaurants/nearby").param("lat", lat).param("lng", lng);
    }
}
