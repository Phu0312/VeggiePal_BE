# Nearby Vegetarian Restaurants (US 15) — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-14 |
| Nhánh | `feature/map-integration` |
| Trạng thái | Chờ review |
| Tài liệu gốc | Task sheet "PHÂN CHIA TASK", tab `TASK`, US 15: `[BE] API Quán chay (Toạ độ)`, `[FE] UI Geolocation Map`, `[FE] UI Đổ Marker Lên Map`; roadmap `IMPLEMENTATION_PHASES.md` Phase 7; file dữ liệu 100 quán chay do Long chuẩn bị |

## 1. Mục tiêu

BUSINESS RULE của dòng `[BE] API Quán chay (Toạ độ)` (Long, Sprint 3, Week 7):

> Bán kính tìm kiếm tối đa: 5-10km. Lọc cứng điều kiện: Type=Vegetarian/Vegan.

Luồng 4 bước đã thống nhất:

1. BE nạp sẵn danh sách quán chay (bắt buộc có `latitude`, `longitude`) bằng `data.sql`.
2. FE lấy tọa độ người dùng (`navigator.geolocation` hoặc ô tìm kiếm) và gọi API.
3. BE dùng `ST_Distance_Sphere` của MySQL lọc các quán trong bán kính, trả JSON.
4. FE `.map()` mảng kết quả thành marker trên Leaflet/Mapbox.

Spec này là phần BE: bước 1 và 3.

## 2. Hiện trạng

| Yêu cầu | Nguồn | Hiện trạng |
|---|---|---|
| Bảng quán chay có tọa độ | Workflow bước 1 | **Chưa có.** Không service nào lưu địa điểm |
| API tìm quán trong bán kính, tối đa 5-10 km | `[BE] API Quán chay (Toạ độ)` | **Chưa có** |
| Chỉ trả quán Vegetarian/Vegan | `[BE] API Quán chay (Toạ độ)` | **Chưa có** |
| Từ chối định vị thì lấy tâm TP.HCM | `[FE] UI Geolocation Map` | Việc của FE |
| Gom cụm marker khi > 50 | `[FE] UI Đổ Marker Lên Map` | Việc của FE |

Roadmap Phase 7 đặt tính năng này vào một `location-service` riêng. Đã chốt với Long: làm service mới, không nhét vào blog-service.

## 3. Thiết kế

### 3.1 location-service

| Mục | Giá trị |
|---|---|
| Thư mục | `location-service/` (Maven project riêng, có `mvnw`, không parent POM) |
| Port | `8085` |
| Package | `com.veggiepal.location` |
| Database | `veggiepal_location`, `createDatabaseIfNotExist=true` |
| Schema | Hibernate `ddl-auto=update`, `data.sql` chạy mỗi lần start (`defer-datasource-initialization`, `sql.init.mode=always`, `encoding=UTF-8`), giống nutrition-service |
| Dependency | `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `mysql-connector-j`, `lombok`, `springdoc-openapi-starter-webmvc-ui` 3.1.0; test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` |
| Mã lỗi riêng | dải `50xx` (identity 10xx, nutrition 20xx, blog 30xx, recipe 40xx) |

**Không có Spring Security / JWT.** Endpoint duy nhất là public, chỉ đọc, và khách chưa đăng nhập cũng phải xem được bản đồ. Không có security thì header `Authorization` cũ cũng không thể làm hỏng request. Khi service có endpoint cần đăng nhập (ví dụ admin thêm/sửa quán), chép `SecurityConfig`/`JwtConfig`/`SecurityExceptionHandler` từ blog-service lúc đó.

Cũng bỏ `spring-boot-starter-validation` (không có DTO `@Valid`), MapStruct (chỉ một phép map), Cloudinary và devtools.

Các class dùng chung chép từ blog-service, rút gọn:

- `ApiResponse<T>`: y nguyên (`code` mặc định 1000, bỏ field null).
- `AppException`: y nguyên.
- `ErrorCode`: chỉ `UNCATEGORIZED_EXCEPTION` (9999), `INVALID_REQUEST` (1018) và hai mã mới ở §3.4.
- `GlobalExceptionHandler`: catch-all → 9999; `AppException` → mã của nó; `MissingServletRequestParameterException` và `MethodArgumentTypeMismatchException` → 400/1018.
- `OpenApiConfig`: server `/api`, không khai báo `bearerAuth`.

### 3.2 Dữ liệu

Entity `Restaurant`, bảng `vegetarian_restaurants` (giữ tên trong file dữ liệu):

| Cột | Field | Kiểu | Ràng buộc |
|---|---|---|---|
| `id` | `id` | `BIGINT` auto increment | PK |
| `name` | `name` | `VARCHAR(255)` | NOT NULL |
| `address` | `address` | `VARCHAR(255)` | |
| `latitude` | `latitude` | `Double` | NOT NULL |
| `longitude` | `longitude` | `Double` | NOT NULL |
| `phone_number` | `phoneNumber` | `VARCHAR(255)` | |
| `rating` | `rating` | `Double` | |
| `is_vegan_only` | `veganOnly` | `Boolean` (`bit(1)`) | NOT NULL |
| `image_url` | `imageUrl` | `VARCHAR(500)` | null được (bổ sung 2026-10-15) |

`src/main/resources/data.sql` là file 100 quán của Long, sửa ba chỗ:

1. Bỏ dòng đầu `các quán chay` (không phải SQL).
2. `INSERT INTO ... AS new ON DUPLICATE KEY UPDATE` (upsert theo `id`): chạy lại mỗi lần start không lỗi trùng `id`, và sửa một dòng có sẵn trong file thì lần start sau database cập nhật theo. Xóa một dòng khỏi file không xóa nó khỏi database.
3. `\'` → `''` (`L''Herbanyste`, `Buffet Chay D''GEMMA`): cú pháp chuẩn, không phụ thuộc `sql_mode`.

Không thêm index: 100 dòng thì quét toàn bảng là đủ.

**Ảnh minh họa (bổ sung 2026-10-15).** File CSV ảnh kèm theo chỉ có link trang Google Images, trang Google Maps và Static Maps với API key giữ chỗ: không có ảnh nào tải được, và lưu ảnh Google lên Cloudinary vi phạm bản quyền/điều khoản Google Maps Platform. Thay vào đó mỗi dòng seed có `image_url` trỏ tới một trong 10 ảnh miễn phí bản quyền (Unsplash) đặt sẵn trong Cloudinary của nhóm, thư mục `veggiepal/restaurants/`, chọn theo loại quán (từ tên):

| Tên quán chứa | Ảnh | Số quán |
|---|---|---|
| "Lẩu" | `lau` | 2 |
| "Buffet" | `buffet` | 1 |
| "Café"/"Cafe" | `cafe` | 4 |
| "Hủ Tiếu"/"Bánh Mì" | `street-food` | 2 |
| "Cơm", hoặc tên bắt đầu bằng "Quán" | `com-chay-{id % 2 + 1}` | 25 |
| còn lại | `restaurant-{id % 4 + 1}` | 66 |

URL: `https://res.cloudinary.com/r3ubxlz3/image/upload/c_fill,w_600,h_400,f_auto,q_auto/veggiepal/restaurants/<ảnh>` (Cloudinary cắt 600×400 và tự chọn định dạng/chất lượng). Đây là ảnh minh họa, không phải ảnh thật của quán; khi có ảnh thật thì chỉ sửa URL của dòng đó trong `data.sql`.

**Ảnh thật và danh sách quán mới (bổ sung 2026-10-15, từ `map_data.docx` của Long).**

- id 81–90 đổi sang quán mới: 7 quán lẩu chay ở Dĩ An (Bình Dương) và 3 quán chay ở TP. Thủ Đức, thay cho các quán ở Vũng Tàu và Thủ Dầu Một. Các dòng khác giữ nguyên dữ liệu. Các quán mới cách chợ Bến Thành 10,4–17 km, nên giá trị pin của integration test không đổi; test bán kính 10 km giờ dùng id 90 (10,44 km) làm quán gần nhất nằm ngoài.
- 88 dòng có `image_url` là ảnh thật của quán, **hotlink** đúng link trong file (web của quán, TripAdvisor, PasGo, báo, Shopee, thumbnail Google Images/Google Maps…). Không sao chép các ảnh này lên Cloudinary: đó là ảnh có bản quyền của bên thứ ba. Link có thể hỏng bất cứ lúc nào, nên FE phải hiện ảnh mặc định khi ảnh tải lỗi (`onError`).
- 12 dòng không có ảnh dùng được giữ ảnh minh họa theo bảng trên: 91–100 (file dùng lại ảnh của quán khác), 57 (link Facebook trả về HTML) và 49 (ảnh stock chung).
- Cột vẫn là `image_url` một URL, không phải mảng JSON `img` như trong file, vì mỗi quán chỉ có một ảnh.

### 3.3 Hợp đồng API

```
GET /api/restaurants/nearby?lat=<vĩ độ>&lng=<kinh độ>[&radiusKm=5][&veganOnly=false]
```

Không cần token.

| Tham số | Bắt buộc | Mặc định | Hợp lệ |
|---|---|---|---|
| `lat` | có | | `-90 ≤ lat ≤ 90` |
| `lng` | có | | `-180 ≤ lng ≤ 180` |
| `radiusKm` | không | `5` | `0 < radiusKm ≤ 10` (BR: tối đa 5-10 km) |
| `veganOnly` | không | `false` | `true` chỉ trả quán thuần chay |

Response `ApiResponse<List<RestaurantResponse>>`, **xếp theo khoảng cách tăng dần**, không phân trang (bán kính ≤ 10 km đã chặn kích thước):

```json
{
  "code": 1000,
  "result": [
    {
      "id": 2,
      "name": "Shamballa Vegetarian Restaurant",
      "address": "31 Lý Tự Trọng, P. Bến Nghé, Quận 1, TP.HCM",
      "latitude": 10.7768,
      "longitude": 106.7001,
      "phoneNumber": "0917876788",
      "rating": 4.7,
      "veganOnly": true,
      "imageUrl": "https://www.shamballa.co/wp-content/uploads/2024/09/banner-m-1.jpg",
      "distanceKm": 0.53
    }
  ]
}
```

- `distanceKm` làm tròn 2 chữ số thập phân.
- `imageUrl` là ảnh thật của quán (hotlink) hoặc ảnh minh họa trên Cloudinary (xem §3.2). FE hiện ảnh mặc định khi `null` hoặc khi ảnh tải lỗi (`onError`).
- Không có quán nào trong bán kính: `result: []`, HTTP 200.
- Tên tham số là `lat`/`lng`, không phải `userLat`/`userLng` như bản phác workflow. Cần báo Hưng (FE).

**"Lọc cứng Type=Vegetarian/Vegan":** bảng chỉ chứa quán chay, nên mọi kết quả đã là Vegetarian hoặc Vegan nhờ dữ liệu. `is_vegan_only` phân biệt hai loại, `veganOnly=true` lọc riêng quán thuần chay.

### 3.4 Lỗi

| Trường hợp | HTTP | `code` | `message` |
|---|---|---|---|
| Thiếu `lat`/`lng`, hoặc tham số không phải số / boolean | 400 | 1018 `INVALID_REQUEST` | Invalid request data |
| `lat` hoặc `lng` ngoài phạm vi, hoặc `NaN` | 400 | 5001 `INVALID_COORDINATES` | Latitude must be within [-90, 90] and longitude within [-180, 180] |
| `radiusKm ≤ 0`, `> 10`, hoặc `NaN` | 400 | 5002 `INVALID_SEARCH_RADIUS` | Search radius must be greater than 0 and at most 10 km |

Kiểm tra viết dạng `!(lat >= -90 && lat <= 90)` để `NaN` (Spring parse được chuỗi `"NaN"` thành `double`) cũng bị từ chối. Không kiểm tra thì MySQL ném lỗi vĩ độ ngoài phạm vi và client nhận 500.

### 3.5 Truy vấn

Native query trong `RestaurantRepository`, đọc qua interface projection `NearbyRestaurant`:

```sql
select r.id as id, r.name as name, r.address as address,
       r.latitude as latitude, r.longitude as longitude,
       r.phone_number as phoneNumber, r.rating as rating, r.is_vegan_only as veganOnly,
       ST_Distance_Sphere(point(r.longitude, r.latitude), point(:lng, :lat)) as distanceMeters
from vegetarian_restaurants r
where ST_Distance_Sphere(point(r.longitude, r.latitude), point(:lng, :lat)) <= :radiusMeters
  and (:veganOnly = false or r.is_vegan_only = 1)
order by distanceMeters
```

- **`point(kinh độ, vĩ độ)`.** `ST_Distance_Sphere` đọc x là kinh độ, y là vĩ độ. Đảo thứ tự thì "vĩ độ" thành ~106 và MySQL báo lỗi ngoài phạm vi, nên integration test bắt được ngay.
- Service đổi `radiusKm * 1000` sang mét trước khi gọi, và đổi `distanceMeters` sang `distanceKm` khi map sang `RestaurantResponse`.
- Lấy `findAll()` rồi tính haversine bằng Java cũng chạy được với 100 dòng, nhưng lệch khỏi workflow đã chốt (lọc trong SQL) và phải nạp cả bảng mỗi request.

### 3.6 Gateway

Ba mục trong `api-gateway/src/main/resources/application.yaml`, giống các service khác:

1. Route `location-service-restaurants`: `Path=/api/restaurants/**` → `http://localhost:8085`, `StripPrefix=1`.
2. Route `location-service-docs`: `Path=/location-service/v3/api-docs/**` → `http://localhost:8085`, `StripPrefix=1`.
3. `springdoc.swagger-ui.urls`: `name: Location Service`, `url: /location-service/v3/api-docs`.

CORS đã nằm ở gateway, không đổi.

## 4. Test

| Test | Loại | Nội dung |
|---|---|---|
| `RestaurantServiceTest` | Unit, Mockito | Đổi km sang mét khi gọi repository; map `distanceMeters` sang `distanceKm` làm tròn 2 chữ số; từ chối `lat`/`lng` ngoài phạm vi và `NaN` (5001); từ chối `radiusKm` 0, 10.01, `NaN` (5002); chấp nhận đúng 10; lỗi thì không gọi repository |
| `RestaurantControllerTest` | `@WebMvcTest` | 200 không cần token; mặc định `radiusKm=5`, `veganOnly=false`; thiếu `lat` → 400/1018; `lat=abc` → 400/1018; service ném `INVALID_SEARCH_RADIUS` → 400/5002 |
| `LocationServiceIntegrationTests` | MySQL thật, schema `veggiepal_location_it` | Xem dưới |

`LocationServiceIntegrationTests` chạy toàn bộ context, dữ liệu nạp từ `data.sql`. Tâm đo là chợ Bến Thành `(10.7725, 106.6980)`:

- `radiusKm=1`: đúng 7 quán, theo thứ tự id `2, 3, 6, 26, 4, 22, 21`; quán đầu (Shamballa) cách ~0.53 km. Bắt được: sai thứ tự `point()`, quên `ORDER BY`, nhầm mét với km (bán kính hoặc `distanceKm`), sai alias projection.
- `radiusKm=10`: có id 20 (7.14 km), không có id 19 (11.52 km). Bắt được lỗi so sánh bán kính.
- `veganOnly`: cả 100 dòng seed đều là `is_vegan_only = 1`, nên test tự lưu một quán chỉ-chay ở Hà Nội (xa toàn bộ seed), kiểm `veganOnly=false` có nó và `veganOnly=true` không có, rồi xóa. Bắt được lỗi bind boolean.

Như `BlogServiceIntegrationTests`, test này là **cổng bắt buộc** trước khi commit thay đổi entity, repository hoặc `@Query` của location-service, và bị loại khỏi vòng test nhanh vì cần Docker:

```bash
./mvnw test -Dtest='!LocationServiceIntegrationTests'   # vòng nhanh
./mvnw test                                              # đủ, cần MySQL
```

Không có `LocationServiceApplicationTests.contextLoads` riêng: integration test đã khởi động cả context.

## 5. Tài liệu

`CLAUDE.md`:

- Overview: thêm `location-service/` vào danh sách service, ghi rõ nó không có Spring Security.
- Commands: `cd location-service && ./mvnw spring-boot:run    # :8085`, và hai lệnh test ở §4.
- Request flow: route `/api/restaurants/**` → location-service (8085).
- Error code ranges: thêm 50xx.
- Một mục `### location-service` ngắn: bảng `vegetarian_restaurants` seed từ `data.sql`, query `ST_Distance_Sphere` với `point(lng, lat)`, integration test là cổng bắt buộc.

## 6. Không làm

| Phương án | Lý do bỏ |
|---|---|
| Đặt trong blog-service | Long chọn service riêng theo roadmap Phase 7 |
| Spring Security / JWT | Không có endpoint cần đăng nhập. Thêm khi có |
| Cột `type` dạng ENUM (`VEGETARIAN`/`VEGAN`) | File dữ liệu đã có `is_vegan_only`; ENUM MySQL không thêm hằng mới qua `ddl-auto` được (xem CLAUDE.md) |
| `SPATIAL` index, cột `POINT SRID 4326`, bounding box | 100 dòng. Thêm khi bảng lên hàng chục nghìn dòng |
| Phân trang / `LIMIT` | Bán kính ≤ 10 km đã chặn; với seed hiện tại tối đa 79 quán |
| `GET /restaurants/{id}`, CRUD admin | Không có trong task sheet |
| Place provider ngoài (Google Places, Mapbox) | Roadmap Phase 7 có nhắc, nhưng task sheet chỉ yêu cầu dữ liệu tự chuẩn bị |

## 7. Vấn đề dữ liệu (không chặn)

- **Cả 100 quán đều `is_vegan_only = 1`.** `veganOnly=true` hiện không loại quán nào. Nhiều quán "cơm chay", "buffet chay" có thể chỉ là chay (có trứng/sữa). Sửa dữ liệu khi có nguồn xác minh; code không đổi.
- Tọa độ làm tròn 5 chữ số thập phân (~1 m) và có vẻ được ước lượng; marker có thể lệch vị trí thật vài chục đến vài trăm mét.
