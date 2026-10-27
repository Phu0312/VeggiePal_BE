package com.veggiepal.location.entity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "vegetarian_restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    String address;

    @Column(nullable = false)
    Double latitude;

    @Column(nullable = false)
    Double longitude;

    @Column(name = "phone_number")
    String phoneNumber;

    Double rating;

    // The table only holds vegetarian places (task sheet US15: Type=Vegetarian/Vegan);
    // this tells the strictly vegan ones apart.
    @Column(name = "is_vegan_only", nullable = false)
    Boolean veganOnly;

    // HTTPS image URL. Demo fixtures mix source images and related illustrations, not
    // the restaurant's own; null means the FE shows its default image.
    @Column(name = "image_url", length = 500)
    String imageUrl;
}
