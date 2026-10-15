package com.veggiepal.location.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestaurantResponse {

    Long id;

    String name;

    String address;

    Double latitude;

    Double longitude;

    String phoneNumber;

    Double rating;

    Boolean veganOnly;

    // Null when the restaurant has no image; the FE falls back to its default
    String imageUrl;

    // From the search point, rounded to 2 decimals
    Double distanceKm;
}
