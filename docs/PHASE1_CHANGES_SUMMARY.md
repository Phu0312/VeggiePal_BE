# TỔNG HỢP CÁC THAY ĐỔI THEO TỪNG CLASS — VEGGIEPAL (PHASE 1 & GUEST MVP)

Tài liệu này ghi lại toàn bộ các file và class đã được tạo mới hoặc chỉnh sửa trong Phase 1 (Fix 4 lỗi Critical P0) và quá trình đơn giản hóa Guest Chatbot theo định hướng MVP.

---

## 1. Danh sách tổng quan các File & Class

| STT | File / Class | Trạng thái | Nhiệm vụ liên quan |
|---|---|---|---|
| 1 | `ai-service/.../mealplan/IngredientUnitConverter.java` | **MỚI** | Task 1: Quy đổi đơn vị nguyên liệu Recipe |
| 2 | `ai-service/.../mealplan/AllergenDetector.java` | **MỚI** | Task 2: Nhận diện 32 mã dị ứng chuẩn |
| 3 | `ai-service/.../mealplan/MealPlanValidator.java` | **SỬA** | Task 1 & Task 2: Lọc đơn vị, tính macro an toàn, đối chiếu dị ứng |
| 4 | `ai-service/.../mealplan/MealPlanService.java` | **SỬA** | Task 2 & Task 3: Bắt 404 Health Record, kiểm tra dị ứng đổi món |
| 5 | `ai-service/.../common/ErrorCode.java` | **SỬA** | Task 1 & Task 3: Thêm mã lỗi nghiệp vụ |
| 6 | `ai-service/.../chat/GuestSession.java` | **SỬA** | Task 4 MVP: Constructor mặc định không phụ thuộc IP |
| 7 | `ai-service/.../chat/GuestSessionRepository.java` | **SỬA** | Task 4 MVP: Tìm kiếm theo ID & hạn dùng (bỏ IP hash) |
| 8 | `ai-service/.../chat/GuestSessionService.java` | **SỬA** | Task 4 MVP: Đơn giản hóa tạo và xác thực session UUID 24h |
| 9 | `ai-service/.../chat/ChatService.java` | **SỬA** | Task 4 MVP: Bỏ remoteAddress, giữ quota 3 chat/session |
| 10 | `ai-service/.../chat/ChatController.java` | **SỬA** | Task 4 MVP: Bỏ IP Resolver, endpoint gọi thẳng service |
| 11 | `ai-service/.../mealplan/IngredientUnitConverterTest.java` | **MỚI** | Unit test quy đổi đơn vị (5 tests) |
| 12 | `ai-service/.../mealplan/AllergenDetectorTest.java` | **MỚI** | Unit test nhận diện dị ứng (5 tests) |
| 13 | `ai-service/.../mealplan/MealPlanServiceTest.java` | **MỚI** | Unit test 404 vs 503 Health Record (3 tests) |
| 14 | `ai-service/.../mealplan/MealPlanValidatorTest.java` | **SỬA** | Unit test validator macro & dị ứng (11 tests) |
| 15 | `ai-service/.../chat/GuestSessionServiceTest.java` | **SỬA** | Unit test guest session UUID & TTL (4 tests) |
| 16 | `ai-service/.../chat/ChatControllerTest.java` | **MỚI** | Unit test controller guest session & chat (3 tests) |
| 17 | `ai-service/.../chat/ChatServiceTest.java` | **SỬA** | Unit test chat service signature mới (1 test) |

---

## 2. Chi tiết thay đổi trong từng Class

### Task 1 — Sửa lỗi đơn vị nguyên liệu (Recipe → AI Meal Planning)

#### 1. [`IngredientUnitConverter.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/IngredientUnitConverter.java) *(Mới)*
- **Mục đích:** Quy đổi khối lượng, thể tích lỏng và đơn vị đếm thực tế sang gram dựa trên cơ sở khoa học, không dùng hệ số cào bằng.
- **Chi tiết triển khai:**
  - **Khối lượng chuẩn:** Hỗ trợ `g`, `gram`, `gam`, `kg`, `kilogram` (x1000), `mg` (/1000).
  - **Thể tích lỏng theo tỷ trọng ẩm thực (Culinary Density):**
    - Dầu ăn (`dau_an`, `dau_oliu`, `dau_me`, `dau_dau_phong`...): **0.92 g/ml**.
    - Nước tương, xì dầu, mật mía, siro đường: **1.15 - 1.33 g/ml**.
    - Sữa hạt, sữa chua thực vật: **1.03 g/ml**.
    - Nước lọc, nước canh: **1.00 g/ml**.
  - **Đơn vị thìa/muỗng:**
    - Muỗng canh (tablespoon): **15 ml** × tỷ trọng nguyên liệu.
    - Muỗng cà phê (teaspoon): **5 ml** × tỷ trọng nguyên liệu.
  - **Đơn vị đếm (quả, củ, bắp, tép, bìa):**
    - Cà chua: ~120g/quả; Cà rốt: ~100g/củ; Khoai tây: ~150g/củ.
    - Tép tỏi: ~4g/tép; Củ tỏi: ~35g/củ; Hành tím: ~15g/củ; Hành tây: ~150g/củ.
    - Chuối: ~118g/quả; Bơ: ~150g/quả; Chanh: ~60g/quả; Ớt: ~5g/trái.
    - Bìa đậu phụ: ~150g/bìa; Bắp ngô: ~250g/bắp; Bắp cải: ~800g/bắp.
  - **Khớp từ an toàn:** Sử dụng hàm `matchesWord()` theo ranh giới từ (word boundary) và sắp xếp từ khóa theo độ dài giảm dần, tránh tình trạng "cà rốt" bị khớp nhầm vào "ớt".
  - **Không tự bịa dữ liệu:** Trả về `OptionalDouble.empty()` nếu gặp đơn vị không xác định.

#### 2. [`MealPlanValidator.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java) *(Chỉnh sửa)*
- **Phương thức `grams()`:** Sử dụng `IngredientUnitConverter.convertToGrams(unit, amount, ingredientName)` thay vì logic nhân chia thô sơ cũ.
- **Phương thức `canCalculateMacro()` & `tryCalculateMacro()`:** Kiểm tra tính toàn vẹn của nguyên liệu. Nếu nguyên liệu thiếu calorie hoặc đơn vị không thể quy đổi:
  - Ghi log `WARN`: `Excluding recipe id=..., title=...: cannot convert unit '...' for ingredient '...'`.
  - Loại Recipe khỏi danh sách ứng viên, **không làm cả hệ thống trả 503**.
- **Phương thức `safeCandidates()`:** Lọc bỏ các món lỗi dữ liệu. Nếu không còn Recipe nào hợp lệ, trả về lỗi nghiệp vụ rõ ràng `NO_VALID_RECIPES`.

---

### Task 2 — Sửa lỗi dị ứng (Allergen Mapping)

#### 3. [`AllergenDetector.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/AllergenDetector.java) *(Mới)*
- **Mục đích:** Nhận diện chính xác 32 mã dị ứng chính thức của VeggiePal từ tên nguyên liệu Recipe.
- **32 Mã dị ứng hỗ trợ:**
  `PEANUTS`, `SOY`, `SESAME`, `GLUTEN`, `WHEAT`, `MUSHROOMS`, `CELERY`, `MUSTARD`, `WALNUTS`, `CASHEWS`, `ALMONDS`, `STRAWBERRIES`, `KIWI`, `PEACHES`, `TOMATOES`, `CORN`, `GARLIC`, `ONION`, `CHILI`, `EGGPLANT`, `BELL_PEPPERS`, `SULPHITES`, `CITRUS`, `YEAST`, `COCONUT`, `PINEAPPLE`, `AVOCADO`, `MANGO`, `PAPAYA`, `CINNAMON`, `SUNFLOWER_SEEDS`, `PUMPKIN_SEEDS`.
- **Phương thức `detectAllergens(String ingredientName)`:** Chuẩn hóa tiếng Việt (loại bỏ dấu bằng `Normalizer`) và khớp từ chính xác, trả về `Set<String>` các mã dị ứng.

#### 4. [`MealPlanValidator.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java) *(Bổ sung)*
- **Phương thức `isAllergySafe()`:**
  - **Không loại bỏ toàn bộ Recipe chỉ vì `allergen=true`:** Đối chiếu mã dị ứng của từng nguyên liệu với danh sách dị ứng của người dùng (`userAllergies`).
  - Nếu món có `allergen=true` nhưng chỉ chứa đậu phộng (`PEANUTS`) mà người dùng chỉ dị ứng nấm (`MUSHROOMS`), món ăn vẫn được xem là **AN TOÀN**.
  - **Fail-safe:** Nếu nguyên liệu bị gắn cờ dị ứng mà không xác định được mã cụ thể, hoặc thiếu hẳn thông tin dị ứng (`ingredient.allergen() == null`), hệ thống coi là **KHÔNG AN TOÀN** và loại bỏ để bảo vệ người dùng.

#### 5. [`MealPlanService.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java) *(Chỉnh sửa)*
- Kiểm tra dị ứng ở 3 bước:
  1. Lọc trước khi gửi sang Groq AI (`safeCandidates`).
  2. Kiểm tra sau khi Groq trả về thực đơn 7 ngày (`validateAndEnrich`).
  3. Kiểm tra khi người dùng yêu cầu thay món (`replaceMeal`), đảm bảo món mới an toàn với dị ứng của user.

---

### Task 3 — Xử lý thiếu Health Record (404 vs 503)

#### 6. [`MealPlanService.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java) *(Chỉnh sửa)*
- Bắt `RestClientResponseException` khi gọi Nutrition Service `/api/nutrition/health-records/latest`:
  - Nếu mã phản hồi là **HTTP 404** (user chưa có hồ sơ sức khỏe): Bắn ngoại lệ `AppException(ErrorCode.HEALTH_RECORD_REQUIRED)` (HTTP 400).
  - Không tự bịa chiều cao, cân nặng mặc định.
  - Phân biệt rõ ràng với lỗi timeout hoặc HTTP 5xx từ downstream (vẫn trả `DOWNSTREAM_UNAVAILABLE` 503).

#### 7. [`ErrorCode.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/common/ErrorCode.java) *(Bổ sung)*
```java
HEALTH_RECORD_REQUIRED(6010, "Yêu cầu cập nhật hồ sơ sức khỏe trước khi tạo thực đơn", HttpStatus.BAD_REQUEST),
NO_VALID_RECIPES(6011, "Không tìm thấy công thức nấu ăn phù hợp với yêu cầu dinh dưỡng và dị ứng", HttpStatus.BAD_REQUEST);
```

---

### Task 4 — Đơn giản hóa Guest Chatbot theo MVP

#### 8. [`GuestSession.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/chat/GuestSession.java) *(Chỉnh sửa)*
- Thêm constructor `GuestSession(String id, LocalDateTime createdAt, LocalDateTime expiresAt)`.
- Mặc định lưu `ipHash = "guest"` để thỏa mãn ràng buộc `NOT NULL` của bảng `ai_guest_sessions` mà không cần chạy migration sửa database.

#### 9. [`GuestSessionRepository.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/chat/GuestSessionRepository.java) *(Chỉnh sửa)*
- Đơn giản hóa method truy vấn:
```java
public interface GuestSessionRepository extends JpaRepository<GuestSession, String> {
    Optional<GuestSession> findByIdAndExpiresAtAfter(String id, LocalDateTime now);
}
```

#### 10. [`GuestSessionService.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/chat/GuestSessionService.java) *(Chỉnh sửa)*
- Xóa bỏ logic băm SHA-256 IP, pepper bí mật, và giới hạn 3 session/IP/ngày.
- `create()`: Sinh UUID ngẫu nhiên, hạn dùng TTL 24h, lưu vào DB.
- `requireValid(sessionId)`: Kiểm tra session có tồn tại và chưa quá 24h. Bắn `ErrorCode.UNAUTHENTICATED` nếu hết hạn hoặc không tồn tại.

#### 11. [`ChatService.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/chat/ChatService.java) *(Chỉnh sửa)*
- Phương thức `guest(String guestSession, String text)`: Bỏ tham số `remoteAddress`.
- Duy trì giới hạn Quota: `quota.reserveChat("guest:" + guestSession, true)` đảm bảo mỗi session chỉ được chat tối đa 3 lần/ngày.

#### 12. [`ChatController.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/chat/ChatController.java) *(Chỉnh sửa)*
- Gỡ bỏ hoàn toàn `ClientIpResolver` và `HttpServletRequest`.
- Endpoint `POST /ai/chat/guest-sessions`: Không nhận tham số, tạo session UUID và trả về `GuestSessionResponse`.
- Endpoint `POST /ai/chat/guest`: Nhận `GuestChatRequest` và gọi trực tiếp `service.guest(request.guestSession(), request.message())`.

---

## 3. Các File Kiểm Thử (Tests) Đã Viết & Cập Nhật

1. [`IngredientUnitConverterTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/mealplan/IngredientUnitConverterTest.java) *(Mới - 5 tests)*
   - `convertsMassUnitsCorrectly()`
   - `convertsLiquidVolumesUsingDensity()`
   - `convertsSpoonsToGrams()`
   - `convertsPieceUnits()`
   - `returnsEmptyOnUnsupportedOrZero()`
2. [`AllergenDetectorTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/mealplan/AllergenDetectorTest.java) *(Mới - 5 tests)*
   - `detectsStandardAllergenKeywords()`
   - `detectsVietnameseAccentedKeywords()`
   - `returnsEmptyForSafeIngredients()`
   - `detectsMultipleAllergensInSingleName()`
   - `handlesNullAndEmptyGracefully()`
3. [`MealPlanServiceTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanServiceTest.java) *(Mới - 3 tests)*
   - `getThrowsHealthRecordRequiredWhenNutritionReturns404()`
   - `getThrowsDownstreamUnavailableWhenNutritionReturns500()`
   - `getThrowsDownstreamUnavailableOnNetworkFailure()`
4. [`MealPlanValidatorTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java) *(Bổ sung - 11 tests)*
   - Đơn vị hợp lệ, đơn vị lạ, thiếu calorie, recipe lỗi xen kẽ recipe đúng.
   - Dị ứng đúng loại, dị ứng khác loại (vẫn an toàn), nhiều dị ứng, thiếu dữ liệu allergen (bị loại).
5. [`GuestSessionServiceTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/chat/GuestSessionServiceTest.java) *(Cập nhật - 4 tests)*
   - `createGeneratesUuidSessionWithConfiguredTtl()`
   - `requireValidPassesForActiveSession()`
   - `requireValidRejectsUnknownSession()`
   - `requireValidRejectsExpiredSession()`
6. [`ChatControllerTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/chat/ChatControllerTest.java) *(Mới - 3 tests)*
   - `createGuestSessionReturnsUuidAndExpiresAt()`
   - `guestChatCallsServiceAndReturnsReply()`
   - `guestChatRejectsInvalidSessionFormat()`
7. [`ChatServiceTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/chat/ChatServiceTest.java) *(Cập nhật - 1 test)*
   - Cập nhật test gọi `guest()` theo signature mới không có IP.

---

## 4. Xác nhận tính an toàn & Phạm vi

- **Toàn bộ 60 tests trong `ai-service` chạy qua (0 failure, 0 error).**
- **Không chỉnh sửa `identity-service/**`** (giữ nguyên thay đổi của bạn trong `UserService.java`).
- **Không can thiệp `recipe-service`, `nutrition-service`, `api-gateway routes` hay `docker-compose`.**
- **Không có thao tác `git commit` hay `git push`.**
