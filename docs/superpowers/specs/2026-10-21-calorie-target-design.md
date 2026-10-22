# Tính nhu cầu calo từ hồ sơ — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-21 |
| Nhánh | `fix/AI-recommendation` |
| Trạng thái | Chờ review |
| Tài liệu gốc | `fix_mealplan.docx` mục 3; task sheet "PHÂN CHIA TASK", US 10: `[BE] Hàm tính BMI` (Detail: "Thuật toán tính chỉ số calo theo công thức Y tế"), `[FE] UI Form Nhập BMI` (Detail: "Form điền cân nặng, chiều cao, tuổi, giới tính"), `[BE] Thuật toán sinh Menu AI` ("±10% so với TDEE") |
| Spec liên quan | `2026-10-21-meal-plan-calorie-fit-design.md` dùng mục tiêu do spec này tính. Hai spec làm độc lập được |

## 1. Mục tiêu

Mục tiêu calo phải tính theo đúng người dùng: tuổi, giới tính, mức vận động và mục tiêu giảm/giữ/tăng cân được chọn sẵn. BE là nơi duy nhất tính con số này, FE không tự tính.

## 2. Hiện trạng

- **`MealPlanService.calculateTargetFromHealth` dùng công thức cố định:**
  - BMR = 10×kg + 6,25×cm − 120. Hằng số −120 tương đương với "nam 25 tuổi".
  - TDEE = BMR × 1,35 cho mọi người.
  - Mục tiêu nhận biết bằng cách tìm chữ "giảm/loss/diet" hoặc "tăng/gain/bulk" trong `goal`.
  - Thiếu chiều cao/cân nặng thì âm thầm dùng 170 cm / 65 kg.
- **Chỉ `update-from-health` gọi hàm này.** `POST /ai/meal-plans` dùng `dailyNutritionTarget` do FE gửi; FE không gửi thì không co giãn khẩu phần và không có `targetMet`.
- **Hồ sơ người dùng thiếu dữ liệu.** `User` của identity-service có `dateOfBirth` nhưng chưa có giới tính và mức vận động. ai-service chưa bao giờ gọi identity-service.
- **Ví dụ trong tài liệu:** 80 kg / 180 cm luôn ra 2.522–3.082 kcal, bất kể tuổi, giới tính hay mức vận động.
- **ai-service trả 500 khi request sai dạng.** `GlobalExceptionHandler` không bắt `HttpMessageNotReadableException`, `MissingServletRequestParameterException` và `MethodArgumentTypeMismatchException`, nên một giá trị enum sai hay thiếu query param đều rơi vào catch-all và trả 500.

## 3. Thiết kế

### 3.1 identity-service: giới tính và mức vận động

- **Enum mới** trong `com.veggiepal.enums`:
  - `Gender { MALE, FEMALE, OTHER }`
  - `ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }`
- **`User`:** thêm `gender` và `activityLevel` (cột `activity_level`), kiểu `@Enumerated(EnumType.STRING)`, cho phép null. `ddl-auto=update` tự thêm cột, dạng ENUM của MySQL. Sau này thêm hằng mới thì phải `ALTER` bằng tay (xem CLAUDE.md).
- **`UpdateProfileRequest`:** thêm `Gender gender` và `ActivityLevel activityLevel`; gửi null thì giữ giá trị cũ. Giá trị không hợp lệ sẽ ném `HttpMessageNotReadableException`, handler có sẵn trả 400/1018.
- **`UserProfileResponse`:** thêm hai field.
- **`UserMapper`:** `updateProfile` thêm hai `@Mapping`; `toUser` thêm `ignore = true` cho hai field.

Hợp đồng API: `PATCH /api/users/me` nhận thêm `{"gender": "MALE", "activityLevel": "MODERATE"}`, và `GET /api/users/me` trả thêm hai field này.

### 3.2 Công thức (ai-service)

| Bước | Công thức |
|---|---|
| Tuổi | Số năm tròn từ `dateOfBirth` đến hôm nay. Dưới 18 tuổi thì dừng ở đây và trả `AGE_NOT_SUPPORTED` (§3.3) |
| BMR (Mifflin-St Jeor) | `10 × kg + 6,25 × cm − 5 × tuổi + s`, trong đó `s` là +5 với `MALE`, −161 với `FEMALE`, −78 với `OTHER` (trung bình của hai giá trị) |
| TDEE | `BMR × hệ số`: SEDENTARY 1,2 · LIGHT 1,375 · MODERATE 1,55 · ACTIVE 1,725 · VERY_ACTIVE 1,9 |
| Calo mục tiêu | `TDEE × 0,85` (LOSE) / `× 1,00` (MAINTAIN) / `× 1,15` (GAIN), sau đó kẹp vào `[sàn, 4.000]`. Sàn là 1.500 với `MALE`, 1.200 với `FEMALE` và `OTHER` |
| Khoảng calo | `min = max(round(mục tiêu × 0,9), sàn)`, `max = min(round(mục tiêu × 1,1), 4.000)` |
| Macro | Giữ công thức hiện tại: đạm 1,2–1,8 g/kg; carb 45–60% và fat 20–30% của calo mục tiêu |

Mọi phép tính dùng số chưa làm tròn; chỉ làm tròn kết quả cuối.

| Ví dụ | BMR | TDEE | Calo mục tiêu | Khoảng |
|---|---|---|---|---|
| Nam, 80 kg, 180 cm, 25 tuổi, LIGHT, GAIN | 1.805 | 2.482 | 2.854 | 2.569–3.140 |
| Nữ, 60 kg, 165 cm, 30 tuổi, LIGHT, MAINTAIN | 1.320 | 1.815 | 1.815 | 1.634–1.997 |
| Nữ, 45 kg, 150 cm, 60 tuổi, SEDENTARY, LOSE | 927 | 1.112 | 945 → sàn 1.200 | 1.200–1.320 |
| Nam, 150 kg, 200 cm, 20 tuổi, VERY_ACTIVE, GAIN | 2.655 | 5.045 | 5.801 → trần 4.000 | 3.600–4.000 |

### 3.3 Dữ liệu ai-service cần đọc

| Nguồn | Endpoint | Trường |
|---|---|---|
| nutrition-service | `GET /nutrition/me/health-records/latest` (đã gọi sẵn) | `heightCm`, `weightKg` |
| identity-service | `GET /users/me` (gọi mới, kèm JWT của người dùng) | `dateOfBirth`, `gender`, `activityLevel` |

Thêm `services.identity.base-url=${IDENTITY_SERVICE_URL:http://localhost:8081}`.

| Trường hợp | Lỗi |
|---|---|
| Chưa có health record, hoặc record thiếu chiều cao/cân nặng | `HEALTH_RECORD_REQUIRED` (6010), như hiện tại. Bỏ hai giá trị mặc định 170 cm / 65 kg |
| Thiếu `dateOfBirth`, `gender` hoặc `activityLevel` | Mã mới `PROFILE_INCOMPLETE` (6020, 400): "Vui lòng cập nhật ngày sinh, giới tính và mức độ vận động trong hồ sơ trước khi lập thực đơn." |
| Người dùng dưới 18 tuổi | Mã mới `AGE_NOT_SUPPORTED` (6021, 400): "Thực đơn theo mục tiêu calo hiện chỉ dành cho người từ 18 tuổi trở lên." Mifflin-St Jeor được xây dựng cho người lớn, không dùng được cho trẻ em và tuổi dậy thì |
| identity-service không trả lời | `DOWNSTREAM_UNAVAILABLE` (6006), như các service downstream khác |

Việc kiểm tra tuổi nằm trong bước tính, nên chỉ chạy khi BE tự tính mục tiêu: `generate` không gửi `dailyNutritionTarget`, `update-from-health` không gửi `customTarget`, và `nutrition-target`. Khi FE gửi sẵn mục tiêu thì BE không tính, nên cũng không kiểm tra tuổi.

### 3.4 Mục tiêu dạng giá trị chọn sẵn

- **Enum mới** `GoalType { LOSE, MAINTAIN, GAIN }`, đặt trong package `mealplan`.
- **`GenerateRequest`:** thêm `@NotNull GoalType goalType`. Trường `healthGoal` giữ nguyên, vẫn là mô tả tự do đưa vào prompt.
- **Lưu vào DB:**
  - Flyway `V5__meal_plan_goal_type.sql`: `ALTER TABLE ai_meal_plans ADD COLUMN goal_type VARCHAR(20) NULL;`
  - Entity `MealPlan` thêm `String goalType`, lưu dạng chuỗi như các cột trạng thái khác của ai-service (`ddl-auto=validate`).
  - `MealPlanResponse` thêm `goalType`.
- **Thực đơn cũ có `goal_type` null:** tiếp tục dò chữ trong `goal` như hiện tại. Hàm dò chữ chỉ còn dùng cho trường hợp này.

### 3.5 Ai tính mục tiêu

**`POST /ai/meal-plans`:**
- Không gửi `dailyNutritionTarget`: BE tính theo §3.2 với `goalType` của request.
- Có gửi: dùng nguyên giá trị đó, coi như mục tiêu người dùng tự chọn.
- Mục tiêu thực sự được dùng được lưu vào `constraints.dailyNutritionTarget` và đưa vào prompt. `replace` đọc lại từ chỗ này.
- Chỉ gọi identity-service khi BE phải tự tính.

**`POST /ai/meal-plans/{id}/update-from-health`:** có `customTarget` thì dùng giá trị đó; không thì tính theo `goal_type` của thực đơn (thực đơn cũ thì dò chữ).

**Endpoint mới `GET /ai/meal-plans/nutrition-target?goalType=GAIN`**, để FE hiển thị mục tiêu trước khi tạo thực đơn:

```json
{
  "code": 1000,
  "result": {
    "goalType": "GAIN",
    "bmr": 1805,
    "tdee": 2482,
    "targetCalories": 2854,
    "target": {
      "minCalories": 2569, "maxCalories": 3140,
      "minProtein": 96, "maxProtein": 144,
      "minCarbohydrate": 321, "maxCarbohydrate": 428,
      "minFat": 63, "maxFat": 95
    }
  }
}
```

Endpoint này trả cùng các lỗi ở §3.3. Thiếu `goalType` hoặc `goalType` sai trả 400/6000.

**Request sai dạng:** `GlobalExceptionHandler` của ai-service thêm một handler cho `HttpMessageNotReadableException`, `MissingServletRequestParameterException` và `MethodArgumentTypeMismatchException`, trả 400/6000 `INVALID_REQUEST`, giống identity-service.

**Câu "chỉ là gợi ý":** FE hiển thị câu cố định "Mục tiêu calo chỉ là gợi ý, không thay thế tư vấn của chuyên gia dinh dưỡng." BE không thêm field nào cho việc này.

### 3.6 FE cần đổi

**Trang hồ sơ (Hưng):** thêm hai ô chọn, gửi qua `PATCH /users/me`.
- Giới tính.
- Mức vận động, nhãn gợi ý:
  - `SEDENTARY`: Ít vận động, ngồi nhiều
  - `LIGHT`: Nhẹ, 1–3 buổi/tuần
  - `MODERATE`: Vừa, 3–5 buổi/tuần
  - `ACTIVE`: Nhiều, 6–7 buổi/tuần
  - `VERY_ACTIVE`: Rất nhiều, lao động nặng hoặc tập 2 buổi/ngày

**Thực đơn (Huy):**
- Gửi `goalType` khi tạo thực đơn.
- Bỏ phần tự tính `dailyNutritionTarget`.
- Gọi `GET /ai/meal-plans/nutrition-target` để hiển thị mục tiêu trước khi tạo.
- Gặp mã 6020 thì chuyển người dùng sang trang hồ sơ.
- Gặp mã 6021 thì hiện thông báo trả về và không cho tạo thực đơn.

## 4. Test

**identity-service:**
- `ProfileServiceTest`: cập nhật được `gender` và `activityLevel`; gửi null thì giữ giá trị cũ.
- Test controller của `/users/me`:
  - `PATCH` với `gender` không hợp lệ trả 400/1018.
  - `GET` trả về hai field mới.

**ai-service:**

| Test | Ca |
|---|---|
| `MealPlanUpgradeServiceTest` | 4 ví dụ ở bảng §3.2 ra đúng BMR, TDEE và khoảng calo; thiếu `gender` trả 6020; record thiếu `weightKg` trả 6010; thực đơn có `goal_type` null thì vẫn dò chữ; người còn một ngày nữa mới tròn 18 tuổi trả 6021, đúng ngày sinh nhật 18 tuổi thì tính bình thường |
| `MealPlanServiceTest` | `generate` không có `dailyNutritionTarget`: `constraints.dailyNutritionTarget` bằng mục tiêu BE tính; có gửi thì giữ nguyên giá trị đó và không gọi identity-service |
| `MealPlanControllerTest` | `generate` thiếu `goalType` trả 400; `GET nutrition-target` thiếu `goalType` hoặc `goalType=FOO` trả 400/6000, không phải 500 |
| `AiServiceApplicationTests` (MySQL) | Flyway V5 chạy được và entity `MealPlan` qua được `validate` |

## 5. Tài liệu

- `docs/AI_SERVICE_DOCUMENTATION.md`:
  - Request của `POST /ai/meal-plans` (thêm `goalType`; `dailyNutritionTarget` thành tùy chọn).
  - Endpoint `nutrition-target`.
  - Mã lỗi 6020 và 6021.
  - Công thức ở §3.2.
- Báo cho Hưng và Huy các thay đổi ở §3.6.

## 6. Không làm

| Phương án | Lý do bỏ |
|---|---|
| Lưu giới tính và mức vận động trong health record | Đã chốt lưu ở identity-service, cạnh ngày sinh: đây là thông tin ổn định của người dùng, không phải số đo theo thời gian |
| Nhật ký vận động, theo dõi nước | Mục 4 của tài liệu, làm sau |
| Field câu "chỉ là gợi ý" trong API | FE hiển thị câu cố định |
| Cho người dùng tự chỉnh hệ số giảm/tăng (ví dụ −500 kcal) | Chưa có yêu cầu |
| Đưa hệ số và sàn/trần ra cấu hình | Là hằng số y học, không có nhu cầu đổi |

## 7. Vấn đề còn mở

- **Form BMI cho nhập tuổi > 5.** Task sheet cho phép người từ 6 đến 17 tuổi nhập số đo, nhưng BE sẽ từ chối lập thực đơn cho họ (§3.3). FE nên báo trước điều này để người dùng không nhập xong rồi mới bị từ chối.
- **Hệ số giảm/tăng.** LOSE ×0,85 và GAIN ×1,15 giữ đúng như code hiện tại.
- **Hệ số VERY_ACTIVE 1,9** là giá trị chuẩn của bảng Harris-Benedict/Mifflin; tài liệu gốc chỉ liệt kê đến 1,725.
