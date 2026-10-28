package com.veggiepal.location.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.location.entity.Restaurant;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** One row of {@link #findNearby}: a restaurant plus its distance from the search point. */
    interface NearbyRestaurant {

        Long getId();

        String getName();

        String getAddress();

        Double getLatitude();

        Double getLongitude();

        String getPhoneNumber();

        Double getRating();

        Boolean getVeganOnly();

        String getImageUrl();

        Double getDistanceMeters();
    }

    // ST_Distance_Sphere reads x as longitude and y as latitude: point(lng, lat), never
    // point(lat, lng). The aliases must match the NearbyRestaurant getters.
    // ponytail: full table scan, fine for the hand-seeded list; add a SPATIAL index on a
    // POINT SRID 4326 column once the table reaches tens of thousands of rows.
    @Query(nativeQuery = true, value = """
            select r.id as id, r.name as name, r.address as address,
                   r.latitude as latitude, r.longitude as longitude,
                   r.phone_number as phoneNumber, r.rating as rating, r.is_vegan_only as veganOnly,
                   r.image_url as imageUrl,
                   ST_Distance_Sphere(point(r.longitude, r.latitude), point(:lng, :lat)) as distanceMeters
            from vegetarian_restaurants r
            where ST_Distance_Sphere(point(r.longitude, r.latitude), point(:lng, :lat)) <= :radiusMeters
              and (:veganOnly = false or r.is_vegan_only = 1)
            order by distanceMeters
            """)
    List<NearbyRestaurant> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("veganOnly") boolean veganOnly
    );

    // Name search (task_be C19). Starts-with matches rank first (as in task sheet US7), then the
    // nearest when a point is given, else A-Z. The default ai_ci collation makes LIKE ignore case
    // and accents ("pho" finds "Phở"). distanceMeters is NULL without a point.
    // ponytail: full table scan with a leading-% LIKE, fine for the hand-seeded list; add a
    // FULLTEXT index (ngram parser) if the table grows to thousands of rows.
    @Query(nativeQuery = true, value = """
            select r.id as id, r.name as name, r.address as address,
                   r.latitude as latitude, r.longitude as longitude,
                   r.phone_number as phoneNumber, r.rating as rating, r.is_vegan_only as veganOnly,
                   r.image_url as imageUrl,
                   case when :lat is null then null
                        else ST_Distance_Sphere(point(r.longitude, r.latitude), point(:lng, :lat)) end
                       as distanceMeters
            from vegetarian_restaurants r
            where r.name like concat('%', :q, '%')
              and (:veganOnly = false or r.is_vegan_only = 1)
            order by case when r.name like concat(:q, '%') then 0 else 1 end,
                     distanceMeters is null, distanceMeters, r.name
            limit 20
            """)
    List<NearbyRestaurant> searchByName(
            @Param("q") String q,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("veganOnly") boolean veganOnly
    );
}
