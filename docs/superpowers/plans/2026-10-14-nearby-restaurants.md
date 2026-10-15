# Nearby Vegetarian Restaurants (US 15) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GET /api/restaurants/nearby?lat=&lng=[&radiusKm=5][&veganOnly=false]` trả các quán chay trong bán kính tối đa 10 km, gần nhất trước (BR của task sheet `[BE] API Quán chay (Toạ độ)`).

**Architecture:** Maven project mới `location-service` (port 8085, DB `veggiepal_location`, không Spring Security). Bảng `vegetarian_restaurants` do Hibernate tạo từ entity `Restaurant` và được `data.sql` nạp 100 quán. `RestaurantRepository` dùng native query `ST_Distance_Sphere`, `RestaurantService` validate tham số và đổi đơn vị, `RestaurantController` bọc kết quả trong `ApiResponse`. Gateway thêm route `/api/restaurants/**` và mục Swagger.

**Tech Stack:** Java 21, Spring Boot 4.1.1 (webmvc, data-jpa), Hibernate 7, MySQL 8.4, Lombok, springdoc 3.1.0, JUnit 5 + Mockito + MockMvc.

**Spec:** [`docs/superpowers/specs/2026-10-14-nearby-restaurants-design.md`](../specs/2026-10-14-nearby-restaurants-design.md)

## Global Constraints

- **Không commit, không push.** Long tự review diff và tự commit. Step cuối mỗi task chỉ dừng lại và bàn giao.
- **Không có parent POM.** Chạy Maven từ trong thư mục service (`location-service/`, `api-gateway/`). Trên Windows dùng Git Bash (`./mvnw`); PowerShell làm hỏng tham số `-Dtest=...`.
- Port `8085`, package `com.veggiepal.location`, database `veggiepal_location` (`createDatabaseIfNotExist=true`).
- **Không thêm** Spring Security, OAuth2 resource server, `spring-boot-starter-validation`, MapStruct, Cloudinary, devtools vào `location-service`.
- Mã lỗi: `UNCATEGORIZED_EXCEPTION` 9999, `INVALID_REQUEST` 1018 (giữ số chung), `INVALID_COORDINATES` 5001, `INVALID_SEARCH_RADIUS` 5002.
- Bán kính: mặc định 5 km, hợp lệ `0 < radiusKm ≤ 10`.
- Native query luôn là `point(longitude, latitude)`, **không bao giờ** `point(latitude, longitude)`.
- **`LocationServiceIntegrationTests` cần MySQL** ở `localhost:3307`, root/12345 (`docker compose up -d mysql` ở thư mục gốc repo). Đây là cổng bắt buộc cho mọi thay đổi entity, repository, `data.sql`.
- **Không có linter/formatter.** Bám style blog-service: `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)`, field khai báo trần, DTO là class Lombok `@Data @Builder`.

---

## File Structure

| File | Trách nhiệm |
|---|---|
| `location-service/pom.xml` | Dependency tối thiểu của service |
| `location-service/mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Maven Wrapper, chép từ blog-service |
| `location-service/src/main/resources/application.properties` | Port, datasource, `data.sql` init, springdoc |
| `location-service/src/main/resources/data.sql` | 100 quán chay, `INSERT IGNORE` |
| `.../location/LocationServiceApplication.java` | Main class |
| `.../location/exception/ErrorCode.java`, `AppException.java` | Mã lỗi và exception nghiệp vụ |
| `.../location/exception/GlobalExceptionHandler.java` | Đổi exception thành `ApiResponse` lỗi |
| `.../location/dto/response/ApiResponse.java` | Envelope chung, chép từ blog-service |
| `.../location/dto/response/RestaurantResponse.java` | Một quán trong kết quả, kèm `distanceKm` |
| `.../location/entity/Restaurant.java` | Bảng `vegetarian_restaurants` |
| `.../location/repository/RestaurantRepository.java` | Native query `findNearby` + projection `NearbyRestaurant` |
| `.../location/service/RestaurantService.java` | Validate, km → m, map projection → response |
| `.../location/controller/RestaurantController.java` | `GET /restaurants/nearby` |
| `.../location/configuration/OpenApiConfig.java` | Swagger server `/api` |
| `location-service/src/test/.../service/RestaurantServiceTest.java` | Unit test service |
| `location-service/src/test/.../controller/RestaurantControllerTest.java` | `@WebMvcTest` |
| `location-service/src/test/.../LocationServiceIntegrationTests.java` | MySQL thật, kiểm query |
| `api-gateway/src/main/resources/application.yaml` | 2 route + 1 mục springdoc |
| `CLAUDE.md` | Ghi lại service mới và cổng integration test |

`...` = `location-service/src/main/java/com/veggiepal/location` (main) hoặc `location-service/src/test/java/com/veggiepal/location` (test).

Bốn task: (1) khung service + logic service, (2) lớp HTTP, (3) dữ liệu + query trên MySQL thật, (4) gateway + tài liệu + smoke test. Reviewer có thể duyệt riêng từng task.

---

## Task 1: Khung location-service và `RestaurantService`

**Files:**
- Create: `location-service/pom.xml`
- Create: `location-service/mvnw`, `location-service/mvnw.cmd`, `location-service/.mvn/wrapper/maven-wrapper.properties` (chép)
- Create: `location-service/src/main/java/com/veggiepal/location/LocationServiceApplication.java`
- Create: `location-service/src/main/resources/application.properties`
- Create: `location-service/src/main/java/com/veggiepal/location/exception/ErrorCode.java`
- Create: `location-service/src/main/java/com/veggiepal/location/exception/AppException.java`
- Create: `location-service/src/main/java/com/veggiepal/location/entity/Restaurant.java`
- Create: `location-service/src/main/java/com/veggiepal/location/repository/RestaurantRepository.java`
- Create: `location-service/src/main/java/com/veggiepal/location/dto/response/RestaurantResponse.java`
- Create: `location-service/src/main/java/com/veggiepal/location/service/RestaurantService.java`
- Test: `location-service/src/test/java/com/veggiepal/location/service/RestaurantServiceTest.java`

**Interfaces:**
- Consumes: không có.
- Produces:
  - `RestaurantService.findNearby(double lat, double lng, double radiusKm, boolean veganOnly)` → `List<RestaurantResponse>`; ném `AppException(ErrorCode.INVALID_COORDINATES)` / `AppException(ErrorCode.INVALID_SEARCH_RADIUS)`.
  - `RestaurantResponse` (Lombok `@Data @Builder`): `Long id, String name, String address, Double latitude, Double longitude, String phoneNumber, Double rating, Boolean veganOnly, Double distanceKm`.
  - `RestaurantRepository.findNearby(double lat, double lng, double radiusMeters, boolean veganOnly)` → `List<RestaurantRepository.NearbyRestaurant>`.
  - `Restaurant` (Lombok `@Data @Builder`): `Long id, String name, String address, Double latitude, Double longitude, String phoneNumber, Double rating, Boolean veganOnly`.
  - `ErrorCode.UNCATEGORIZED_EXCEPTION`, `INVALID_REQUEST`, `INVALID_COORDINATES`, `INVALID_SEARCH_RADIUS`; `AppException(ErrorCode)` + `getErrorCode()`.

- [ ] **Step 1: Chép Maven Wrapper**

Từ thư mục gốc repo (Git Bash):

```bash
mkdir -p location-service/.mvn/wrapper
cp blog-service/mvnw blog-service/mvnw.cmd location-service/
cp blog-service/.mvn/wrapper/maven-wrapper.properties location-service/.mvn/wrapper/
```

`maven-wrapper.jar` không cần: `distributionType=only-script`, và `.gitignore` đã bỏ qua jar.

- [ ] **Step 2: Viết `location-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

	<modelVersion>4.0.0</modelVersion>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/>
	</parent>

	<groupId>com.veggiepal</groupId>
	<artifactId>location-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<name>location-service</name>
	<description>VeggiePal Location Service</description>

	<properties>
		<java.version>21</java.version>
	</properties>

	<dependencies>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>

		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>3.1.0</version>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>

	</dependencies>

	<build>
		<plugins>

			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>

			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>

				<configuration>
					<annotationProcessorPaths>
						<path>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</path>
					</annotationProcessorPaths>
				</configuration>
			</plugin>

		</plugins>
	</build>

</project>
```

- [ ] **Step 3: Viết main class và `application.properties`**

`location-service/src/main/java/com/veggiepal/location/LocationServiceApplication.java`:

```java
package com.veggiepal.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}
```

`location-service/src/main/resources/application.properties`:

```properties
spring.application.name=location-service
server.port=8085

spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_location?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=12345

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# Run data.sql after Hibernate has created the tables
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.sql.init.encoding=UTF-8

springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

Không có `jwt.secret`: service này không đọc token.

- [ ] **Step 4: Viết `ErrorCode` và `AppException`**

`location-service/src/main/java/com/veggiepal/location/exception/ErrorCode.java`:

```java
package com.veggiepal.location.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Shared codes: keep the same numbers as the other services
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST),

    // Restaurant search
    INVALID_COORDINATES(5001, "Latitude must be within [-90, 90] and longitude within [-180, 180]", HttpStatus.BAD_REQUEST),

    INVALID_SEARCH_RADIUS(5002, "Search radius must be greater than 0 and at most 10 km", HttpStatus.BAD_REQUEST);

    ErrorCode(
            int code,
            String message,
            HttpStatusCode statusCode
    ) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    final int code;

    final String message;

    final HttpStatusCode statusCode;
}
```

`location-service/src/main/java/com/veggiepal/location/exception/AppException.java`:

```java
package com.veggiepal.location.exception;

public class AppException extends RuntimeException {

    private ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 5: Viết entity, repository và DTO response**

`location-service/src/main/java/com/veggiepal/location/entity/Restaurant.java`:

```java
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
}
```

`location-service/src/main/java/com/veggiepal/location/repository/RestaurantRepository.java`:

```java
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
}
```

`location-service/src/main/java/com/veggiepal/location/dto/response/RestaurantResponse.java`:

```java
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

    // From the search point, rounded to 2 decimals
    Double distanceKm;
}
```

- [ ] **Step 6: Kiểm tra khung compile được**

```bash
cd location-service && ./mvnw -q compile
```

Expected: không in lỗi, exit code 0. Lần đầu wrapper tải Maven 3.9.16 nên mất vài phút.

- [ ] **Step 7: Viết test hỏng cho `RestaurantService`**

`location-service/src/test/java/com/veggiepal/location/service/RestaurantServiceTest.java`:

```java
package com.veggiepal.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
}
```

- [ ] **Step 8: Chạy test, xác nhận hỏng**

```bash
cd location-service && ./mvnw test -Dtest=RestaurantServiceTest
```

Expected: `COMPILATION ERROR`, `cannot find symbol ... class RestaurantService`.

- [ ] **Step 9: Viết `RestaurantService`**

`location-service/src/main/java/com/veggiepal/location/service/RestaurantService.java`:

```java
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
                .distanceKm(Math.round(restaurant.getDistanceMeters() / 10) / 100.0)
                .build();
    }
}
```

- [ ] **Step 10: Chạy test, xác nhận qua**

```bash
cd location-service && ./mvnw test -Dtest=RestaurantServiceTest
```

Expected: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

- [ ] **Step 11: Dừng, bàn giao để review**

Không commit. Báo lại danh sách file đã tạo và output của Step 10.

---

## Task 2: Lớp HTTP: `RestaurantController`, exception handler, Swagger

**Files:**
- Create: `location-service/src/main/java/com/veggiepal/location/dto/response/ApiResponse.java`
- Create: `location-service/src/main/java/com/veggiepal/location/exception/GlobalExceptionHandler.java`
- Create: `location-service/src/main/java/com/veggiepal/location/controller/RestaurantController.java`
- Create: `location-service/src/main/java/com/veggiepal/location/configuration/OpenApiConfig.java`
- Test: `location-service/src/test/java/com/veggiepal/location/controller/RestaurantControllerTest.java`

**Interfaces:**
- Consumes (Task 1): `RestaurantService.findNearby(double, double, double, boolean)` → `List<RestaurantResponse>`; `RestaurantResponse.builder()`; `AppException(ErrorCode)`; `ErrorCode.INVALID_REQUEST`, `INVALID_SEARCH_RADIUS`, `UNCATEGORIZED_EXCEPTION`.
- Produces: `GET /restaurants/nearby` với `lat` (bắt buộc), `lng` (bắt buộc), `radiusKm` (mặc định `5`), `veganOnly` (mặc định `false`), trả `ApiResponse<List<RestaurantResponse>>`; `ApiResponse<T>` (`int code` mặc định 1000, `String message`, `T result`, bỏ field null).

- [ ] **Step 1: Viết test hỏng cho controller**

`location-service/src/test/java/com/veggiepal/location/controller/RestaurantControllerTest.java`:

```java
package com.veggiepal.location.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.location.dto.response.RestaurantResponse;
import com.veggiepal.location.exception.AppException;
import com.veggiepal.location.exception.ErrorCode;
import com.veggiepal.location.service.RestaurantService;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RestaurantService restaurantService;

    @Test
    void nearby_withoutToken_defaultsTo5KmAndAllRestaurants() throws Exception {
        when(restaurantService.findNearby(10.7725, 106.698, 5.0, false)).thenReturn(List.of(
                RestaurantResponse.builder()
                        .id(2L)
                        .name("Shamballa Vegetarian Restaurant")
                        .latitude(10.7768)
                        .longitude(106.7001)
                        .veganOnly(true)
                        .distanceKm(0.53)
                        .build()));

        mockMvc.perform(get("/restaurants/nearby").param("lat", "10.7725").param("lng", "106.698"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].id").value(2))
                .andExpect(jsonPath("$.result[0].name").value("Shamballa Vegetarian Restaurant"))
                .andExpect(jsonPath("$.result[0].veganOnly").value(true))
                .andExpect(jsonPath("$.result[0].distanceKm").value(0.53));
    }

    @Test
    void nearby_passesRadiusAndVeganOnlyThrough() throws Exception {
        when(restaurantService.findNearby(10.7725, 106.698, 2.5, true)).thenReturn(List.of());

        mockMvc.perform(get("/restaurants/nearby")
                        .param("lat", "10.7725")
                        .param("lng", "106.698")
                        .param("radiusKm", "2.5")
                        .param("veganOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty());

        // @MockitoBean is lenient: an unmatched call would also return an empty list.
        verify(restaurantService).findNearby(10.7725, 106.698, 2.5, true);
    }

    @Test
    void nearby_withoutLat_isInvalidRequest() throws Exception {
        mockMvc.perform(get("/restaurants/nearby").param("lng", "106.698"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));

        verifyNoInteractions(restaurantService);
    }

    @Test
    void nearby_withNonNumericLat_isInvalidRequest() throws Exception {
        mockMvc.perform(get("/restaurants/nearby").param("lat", "abc").param("lng", "106.698"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));

        verifyNoInteractions(restaurantService);
    }

    @Test
    void nearby_serviceRejection_returnsItsErrorCode() throws Exception {
        when(restaurantService.findNearby(anyDouble(), anyDouble(), anyDouble(), anyBoolean()))
                .thenThrow(new AppException(ErrorCode.INVALID_SEARCH_RADIUS));

        mockMvc.perform(get("/restaurants/nearby")
                        .param("lat", "10.7725")
                        .param("lng", "106.698")
                        .param("radiusKm", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(5002))
                .andExpect(jsonPath("$.message").value("Search radius must be greater than 0 and at most 10 km"));
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận hỏng**

```bash
cd location-service && ./mvnw test -Dtest=RestaurantControllerTest
```

Expected: `COMPILATION ERROR`, `cannot find symbol ... class RestaurantController`.

- [ ] **Step 3: Viết `ApiResponse` và `GlobalExceptionHandler`**

`location-service/src/main/java/com/veggiepal/location/dto/response/ApiResponse.java`:

```java
package com.veggiepal.location.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    int code = 1000;

    String message;

    T result;
}
```

`location-service/src/main/java/com/veggiepal/location/exception/GlobalExceptionHandler.java`:

```java
package com.veggiepal.location.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.veggiepal.location.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingException(
            Exception exception
    ) {

        log.error("Exception: ", exception);

        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(
            AppException exception
    ) {

        return errorResponse(exception.getErrorCode());
    }

    // A missing lat/lng, or one that is not a number, is a client error rather than the 500
    // the catch-all above would give it.
    @ExceptionHandler(value = {
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiResponse<?>> handlingInvalidRequest(
            Exception exception
    ) {

        return errorResponse(ErrorCode.INVALID_REQUEST);
    }

    private ResponseEntity<ApiResponse<?>> errorResponse(
            ErrorCode errorCode
    ) {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }
}
```

- [ ] **Step 4: Viết `RestaurantController` và `OpenApiConfig`**

`location-service/src/main/java/com/veggiepal/location/controller/RestaurantController.java`:

```java
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
}
```

`location-service/src/main/java/com/veggiepal/location/configuration/OpenApiConfig.java`:

```java
package com.veggiepal.location.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

// No bearerAuth scheme: every endpoint here is public.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI veggiePalOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("VeggiePal Location Service API")
                                .version("1.0")
                                .description("Nearby vegetarian and vegan restaurants for VeggiePal")
                )
                .servers(
                        List.of(
                                new Server()
                                        .url("/api")
                                        .description("API Gateway")
                        )
                );
    }
}
```

- [ ] **Step 5: Chạy test controller, xác nhận qua**

```bash
cd location-service && ./mvnw test -Dtest=RestaurantControllerTest
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

- [ ] **Step 6: Chạy toàn bộ test nhanh**

```bash
cd location-service && ./mvnw test
```

Expected: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0` (12 service + 5 controller). Chưa có integration test nên lệnh này chưa cần MySQL.

- [ ] **Step 7: Dừng, bàn giao để review**

Không commit. Báo lại file đã tạo và output Step 6.

---

## Task 3: Dữ liệu 100 quán và integration test trên MySQL

**Files:**
- Create: `location-service/src/main/resources/data.sql` (sinh từ `C:\Users\MSI-PC\Downloads\các quán chay.txt`)
- Test: `location-service/src/test/java/com/veggiepal/location/LocationServiceIntegrationTests.java`
- Modify (chỉ khi test chỉ ra lỗi query): `location-service/src/main/java/com/veggiepal/location/repository/RestaurantRepository.java`

**Interfaces:**
- Consumes (Task 1, 2): `GET /restaurants/nearby`; `RestaurantRepository` (`save`, `delete`); `Restaurant.builder()`.
- Produces: bảng `vegetarian_restaurants` có id 1–100 sau mỗi lần start. Các giá trị test dựa trên seed (đo từ chợ Bến Thành `10.7725, 106.6980`, bán kính Trái Đất 6 370 986 m như `ST_Distance_Sphere`):

| Bán kính | Kết quả |
|---|---|
| 1 km | đúng 7 quán, theo thứ tự id `2, 3, 6, 26, 4, 22, 21`; id 2 Shamballa cách 0.530 km, id 21 cách 0.908 km |
| 10 km | 79 quán; xa nhất là id 20 (7.143 km); id 19 (11.518 km) nằm ngoài |

- [ ] **Step 1: Bật MySQL**

Từ thư mục gốc repo:

```bash
docker compose up -d mysql
docker ps --filter name=veggiepal-mysql --format "{{.Names}} {{.Status}}"
```

Expected: `veggiepal-mysql Up ...`. Chỉ bật service `mysql`: compose còn định nghĩa container identity-service.

- [ ] **Step 2: Viết integration test hỏng**

`location-service/src/test/java/com/veggiepal/location/LocationServiceIntegrationTests.java`:

```java
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

    private static MockHttpServletRequestBuilder nearby(String lat, String lng) {
        return get("/restaurants/nearby").param("lat", lat).param("lng", lng);
    }
}
```

- [ ] **Step 3: Chạy integration test, xác nhận hỏng**

```bash
cd location-service && ./mvnw test -Dtest=LocationServiceIntegrationTests
```

Expected: `Tests run: 3, Failures: 2`. `nearby_returnsRestaurantsWithinRadiusNearestFirst` và `nearby_radiusIsTheCutOff` hỏng vì bảng trống. `nearby_veganOnlyDropsVegetarianRestaurants` qua. Nếu lỗi là `Errors` kèm SQL exception thay vì `Failures`, query trong `RestaurantRepository` sai: dừng và dùng superpowers:systematic-debugging trước khi sửa.

- [ ] **Step 4: Sinh `data.sql` từ file của Long**

Từ thư mục gốc repo (Git Bash). Script đọc file nguồn, bỏ dòng tiêu đề `các quán chay`, đổi `INSERT INTO` thành `INSERT IGNORE INTO`, và đổi `\'` thành `''` (`chr(92)` là dấu `\`, viết vậy để shell không nuốt escape):

```bash
python -I - "/c/Users/MSI-PC/Downloads/các quán chay.txt" location-service/src/main/resources/data.sql <<'EOF'
import sys
src, dst = sys.argv[1], sys.argv[2]
lines = open(src, encoding='utf-8').read().splitlines()
start = next(i for i, line in enumerate(lines) if line.startswith('INSERT INTO'))
body = '\n'.join(lines[start:])
body = body.replace('INSERT INTO', 'INSERT IGNORE INTO', 1).replace(chr(92) + "'", "''")
header = ('-- Vegetarian restaurants for the map (task sheet US15).\n'
          '-- INSERT IGNORE keeps re-runs idempotent: id is the primary key.\n'
          '-- LocationServiceIntegrationTests pins distances from Ben Thanh market to these rows.\n')
open(dst, 'w', encoding='utf-8', newline='\n').write(header + body.rstrip() + '\n')
EOF
```

- [ ] **Step 5: Kiểm tra file sinh ra**

```bash
cd location-service/src/main/resources
head -4 data.sql
grep -c "^(" data.sql
grep -n "Herbanyste\|GEMMA" data.sql
grep -c "\\\\'" data.sql
tail -1 data.sql
```

Expected:
- `head -4`: 3 dòng comment, rồi `INSERT IGNORE INTO \`vegetarian_restaurants\` (...) VALUES`.
- `grep -c "^("`: `100`.
- Herbanyste/GEMMA: `L''Herbanyste` và `Buffet Chay D''GEMMA`.
- Đếm `\'`: `0`.
- `tail -1`: dòng id 100 kết thúc bằng `);`.

- [ ] **Step 6: Chạy lại integration test, xác nhận qua**

```bash
cd location-service && ./mvnw test -Dtest=LocationServiceIntegrationTests
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`, `BUILD SUCCESS`. Nếu thứ tự id hoặc khoảng cách lệch, đừng sửa giá trị kỳ vọng cho khớp: các con số ở bảng Interfaces đã được tính độc lập từ seed. Lệch tức là query sai (ví dụ `point()` đảo, so sánh mét với km); debug theo superpowers:systematic-debugging.

- [ ] **Step 7: Chạy toàn bộ test**

```bash
cd location-service && ./mvnw test
```

Expected: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0` (12 + 5 + 3).

- [ ] **Step 8: Dừng, bàn giao để review**

Không commit. Báo lại output Step 5 và Step 7.

---

## Task 4: Gateway, CLAUDE.md và smoke test qua gateway

**Files:**
- Modify: `api-gateway/src/main/resources/application.yaml` (sau khối `RECIPE SERVICE`, và cuối danh sách `springdoc.swagger-ui.urls`)
- Modify: `CLAUDE.md:7`, `:48`, `:58-61`, `:71`, `:84`, `:107`, chèn mục mới trước `### Auth (JWT)` (dòng 129)

**Interfaces:**
- Consumes (Task 1-3): location-service chạy ở `:8085`, `GET /restaurants/nearby`, `/v3/api-docs`.
- Produces: `GET http://localhost:8080/api/restaurants/nearby` cho FE; Swagger tổng có "Location Service".

- [ ] **Step 1: Thêm route vào gateway**

Trong `api-gateway/src/main/resources/application.yaml`, ngay sau route `recipe-service-docs` (khối kết thúc bằng `- Path=/recipe-service/v3/api-docs/**` / `filters:` / `- StripPrefix=1`) và trước comment `# SWAGGER UI TỔNG`, chèn:

```yaml


            # =========================
            # LOCATION SERVICE
            # =========================
            - id: location-service-restaurants
              uri: http://localhost:8085
              predicates:
                - Path=/api/restaurants/**
              filters:
                - StripPrefix=1

            - id: location-service-docs
              uri: http://localhost:8085
              predicates:
                - Path=/location-service/v3/api-docs/**
              filters:
                - StripPrefix=1
```

Cuối file, sau mục `Recipe Service` trong `springdoc.swagger-ui.urls`, thêm (cùng thụt lề với các mục trên):

```yaml

      - name: Location Service
        url: /location-service/v3/api-docs
```

- [ ] **Step 2: Kiểm tra gateway vẫn khởi động được**

```bash
cd api-gateway && ./mvnw test
```

Expected: `ApiGatewayApplicationTests` qua, `BUILD SUCCESS` (YAML sai thụt lề sẽ làm context hỏng ở đây).

- [ ] **Step 3: Cập nhật `CLAUDE.md`**

Dòng 7, thay:

```
Each service (`api-gateway/`, `identity-service/`, `nutrition-service/`, `blog-service/`) is a separate Maven project
```

bằng:

```
Each service (`api-gateway/`, `identity-service/`, `nutrition-service/`, `blog-service/`, `location-service/`) is a separate Maven project
```

Dòng 48, sau `cd blog-service && ./mvnw spring-boot:run        # :8083` thêm dòng:

```
cd location-service && ./mvnw spring-boot:run    # :8085
```

Sau dòng 58 (`./mvnw test -Dtest='!BlogServiceApplicationTests,!BlogServiceIntegrationTests'   # blog-service: same`) thêm dòng:

```
./mvnw test -Dtest='!LocationServiceIntegrationTests'        # location-service: same
```

Sau dòng 61 (`./mvnw test -Dtest='BlogServiceApplicationTests,BlogServiceIntegrationTests'`) thêm:

```

# location-service only: the test that needs a real database
./mvnw test -Dtest=LocationServiceIntegrationTests
```

Dòng 71, thay:

```
and blog-service creates `veggiepal_blog` itself the same way.
```

bằng:

```
blog-service creates `veggiepal_blog` itself the same way, and location-service creates `veggiepal_location`.
```

Dòng 84, thay:

```
`/api/blogs/**`, `/api/categories/**`, `/api/comments/**` → blog-service (8083).
```

bằng:

```
`/api/blogs/**`, `/api/categories/**`, `/api/comments/**` → blog-service (8083); `/api/restaurants/**` → location-service (8085).
```

Dòng 107, thay:

```
identity-service uses 10xx, nutrition-service uses 20xx.
```

bằng:

```
identity-service uses 10xx, nutrition-service 20xx, blog-service 30xx, recipe-service 40xx, location-service 50xx.
```

Ngay trước dòng `### Auth (JWT)`, chèn:

```markdown
### location-service

Package `com.veggiepal.location`, port 8085, controllers map `/restaurants/**`. Everything it serves is public and read-only, so it has **no Spring Security and no JWT**: copy `SecurityConfig`, `JwtConfig` and `SecurityExceptionHandler` from blog-service before adding an endpoint that needs a login. `ApiResponse`, `AppException`, `ErrorCode` and `GlobalExceptionHandler` are trimmed copies.

- `GET /restaurants/nearby?lat&lng[&radiusKm=5][&veganOnly=false]` (task sheet US15): `radiusKm` must be in (0, 10]. Results are nearest first and not paged.
- The `vegetarian_restaurants` table is seeded from `src/main/resources/data.sql` (`INSERT IGNORE`, runs on every start). There is no admin CRUD; edit the seed. Every seeded row currently has `is_vegan_only = 1`.
- The search is a native query on `ST_Distance_Sphere(point(longitude, latitude), ...)`. **x is longitude**: `point(lat, lng)` makes MySQL reject the "latitude" (~106) as out of range.
- **`LocationServiceIntegrationTests` is a required gate** before committing a change to the entity, the repository or `data.sql`: a native query is not validated at startup and a mocked repository never runs it. Its expected values are distances from Bến Thành market pinned to the seed, so moving or adding a restaurant near District 1 means recomputing them.

```

- [ ] **Step 4: Smoke test qua gateway**

Kiểm tra port trống rồi chạy hai service ở hai terminal riêng (Git Bash, MySQL vẫn bật):

```bash
netstat -ano | grep -E ":(8080|8085) .*LISTENING"
```

Expected: không in gì.

```bash
cd location-service && ./mvnw spring-boot:run
```

```bash
cd api-gateway && ./mvnw spring-boot:run
```

Đợi cả hai in `Started ...Application`, rồi:

```bash
curl -s "http://localhost:8080/api/restaurants/nearby?lat=10.7725&lng=106.698&radiusKm=1"
```

Expected: `{"code":1000,"result":[{"id":2,"name":"Shamballa Vegetarian Restaurant",...,"distanceKm":0.53},...]}` với 7 phần tử.

```bash
curl -s "http://localhost:8080/api/restaurants/nearby?lat=10.7725&lng=106.698&radiusKm=1" | grep -o '"id":' | wc -l
```

Expected: `7`.

```bash
curl -s -H "Authorization: Bearer stale.token.value" "http://localhost:8080/api/restaurants/nearby?lat=10.7725&lng=106.698&radiusKm=1" | grep -o '"code":[0-9]*'
```

Expected: `"code":1000` (token cũ không ảnh hưởng).

```bash
curl -s -w " HTTP %{http_code}\n" "http://localhost:8080/api/restaurants/nearby?lat=10.7725&lng=106.698&radiusKm=11"
```

Expected: `{"code":5002,"message":"Search radius must be greater than 0 and at most 10 km"} HTTP 400`.

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X OPTIONS -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET" "http://localhost:8080/api/restaurants/nearby"
```

Expected: `200` (CORS preflight qua gateway).

```bash
curl -s http://localhost:8080/location-service/v3/api-docs | grep -o '"/restaurants/nearby"\|"url":"/api"'
```

Expected: cả `"url":"/api"` và `"/restaurants/nearby"`. Mở `http://localhost:8080/swagger-ui.html`, chọn "Location Service" trong dropdown, "Try it out" với `lat=10.7725`, `lng=106.698` trả 200.

Dừng cả hai process (Ctrl+C).

- [ ] **Step 5: Dừng, bàn giao để review**

Không commit. Báo lại output smoke test và danh sách toàn bộ file đã tạo/sửa trong 4 task. Nhắc Long hai việc ngoài code:
- Báo Hưng (FE): endpoint `GET /api/restaurants/nearby`, tham số `lat`/`lng`/`radiusKm`/`veganOnly`, response mẫu ở spec §3.3.
- Cả 100 dòng seed là `is_vegan_only = 1` nên `veganOnly=true` chưa loại quán nào (spec §7).
