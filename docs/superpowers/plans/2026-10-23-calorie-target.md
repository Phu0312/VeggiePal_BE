# Tính nhu cầu calo từ hồ sơ Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** BE tự tính mục tiêu calo theo tuổi, giới tính, mức vận động và mục tiêu giảm/giữ/tăng cân (Mifflin-St Jeor), dùng nó khi tạo và cập nhật thực đơn, và trả nó qua `GET /ai/meal-plans/nutrition-target`.

**Architecture:**
- identity-service thêm `gender` và `activityLevel` vào hồ sơ (`PATCH`/`GET /users/me`).
- ai-service có một lớp tính thuần `NutritionTargetCalculator` (không I/O, có tham số `today` để test được).
- `MealPlanService` đọc health record (nutrition-service) và hồ sơ (identity-service, `GET /users/me` kèm JWT), gọi bộ tính. Kết quả dùng cho `generate` (khi FE không gửi mục tiêu), `updateFromHealth` và endpoint mới.
- Mục tiêu giảm/giữ/tăng thành enum `GoalType`, lưu ở cột mới `goal_type` (Flyway V5).

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Data JPA, MapStruct (identity), Flyway (ai-service), JUnit 5, Mockito, MockMvc.

**Spec:** [`docs/superpowers/specs/2026-10-21-calorie-target-design.md`](../specs/2026-10-21-calorie-target-design.md)

## Global Constraints

**Git và môi trường**
- **Không commit, không push, không chạy lệnh git đổi trạng thái.** Long tự review và commit. Step cuối mỗi task chỉ dừng lại và bàn giao.
- **Chạy Maven bằng Git Bash** (`./mvnw`) từ trong thư mục service. PowerShell làm hỏng `-Dtest=...`. Các test trong plan không cần MySQL: identity dùng unit test và `@WebMvcTest`; ai-service dùng H2 (`src/test/resources`).
- Test nhanh identity: `cd identity-service && ./mvnw test -Dtest='!VeggiepalApplicationTests'`.
- Test meal plan của ai-service: `cd ai-service && ./mvnw test -Dtest='NutritionTargetCalculatorTest,MealPlanValidatorTest,MealPlanServiceTest,MealPlanUpgradeServiceTest,MealPlanControllerTest,MealPlanUpgradeControllerTest,MealPlanSeedAcceptanceTest'`.

**Giá trị cố định** (lấy từ spec §3.2–3.3)
- **Enum identity:** `Gender { MALE, FEMALE, OTHER }`, `ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }`. Cột `gender`, `activity_level`, cho phép null. Enum của ai-service: `GoalType { LOSE, MAINTAIN, GAIN }`.
- **Tuổi:** số năm tròn từ `dateOfBirth` đến hôm nay; **dưới 18 tuổi** thì trả `AGE_NOT_SUPPORTED`.
- **BMR:** `10 × kg + 6,25 × cm − 5 × tuổi + s`, với `s` là +5 (`MALE`), −161 (`FEMALE`), −78 (`OTHER`).
- **TDEE:** `BMR × hệ số`, với SEDENTARY 1,2 · LIGHT 1,375 · MODERATE 1,55 · ACTIVE 1,725 · VERY_ACTIVE 1,9.
- **Calo mục tiêu:** `TDEE × 0,85` (LOSE) / `× 1,00` (MAINTAIN) / `× 1,15` (GAIN), kẹp vào `[sàn, 4000]`. Sàn là 1500 (`MALE`) hoặc 1200 (`FEMALE`, `OTHER`).
- **Khoảng calo:** `min = max(round(mục tiêu × 0,9), sàn)`, `max = min(round(mục tiêu × 1,1), 4000)`.
- **Macro** (tính trên calo mục tiêu đã kẹp): đạm `round(kg × 1,2)`–`round(kg × 1,8)`; carb `round(mục tiêu × 0,45 / 4)`–`round(mục tiêu × 0,60 / 4)`; fat `round(mục tiêu × 0,20 / 9)`–`round(mục tiêu × 0,30 / 9)`.
- **Làm tròn:** mọi phép tính dùng số chưa làm tròn; chỉ làm tròn kết quả cuối (`Math.round`). `bmr`, `tdee`, `targetCalories` trả về là số nguyên đã làm tròn.
- **Mã lỗi:** `PROFILE_INCOMPLETE` 6020 (400), message "Vui lòng cập nhật ngày sinh, giới tính và mức độ vận động trong hồ sơ trước khi lập thực đơn."; `AGE_NOT_SUPPORTED` 6021 (400), message "Thực đơn theo mục tiêu calo hiện chỉ dành cho người từ 18 tuổi trở lên."; `HEALTH_RECORD_REQUIRED` 6010 khi thiếu chiều cao/cân nặng (bỏ hai giá trị mặc định 170/65); `DOWNSTREAM_UNAVAILABLE` 6006 khi identity-service lỗi; request sai dạng trả 400/6000 `INVALID_REQUEST`.
- **Chỉ kiểm tra tuổi và hồ sơ khi BE tự tính mục tiêu:** `generate` không có `dailyNutritionTarget`, `updateFromHealth` không có `customTarget`, và `nutrition-target`. Khi FE gửi sẵn mục tiêu thì không gọi identity-service.
- `services.identity.base-url=${IDENTITY_SERVICE_URL:http://localhost:8081}`.

**Quyết định của plan**
- Bộ tính đặt trong lớp thuần mới `NutritionTargetCalculator`, và test của nó trong `NutritionTargetCalculatorTest`, thay vì `MealPlanUpgradeServiceTest` như spec §4 ghi. `MealPlanService` đã dài hơn 750 dòng, còn một hàm thuần thì test được mà không cần mock.
- ai-service nhận `gender`/`activityLevel` từ identity dưới dạng chuỗi và tra bảng hệ số. Giá trị lạ cũng trả `PROFILE_INCOMPLETE`. Không chép enum của identity sang ai-service.

---

## File Structure

| File | Trách nhiệm |
|---|---|
| `identity-service/src/main/java/com/veggiepal/enums/Gender.java`, `ActivityLevel.java` (mới) | Enum hồ sơ |
| `identity-service/src/main/java/com/veggiepal/entity/User.java` | Hai cột mới |
| `identity-service/src/main/java/com/veggiepal/dto/request/UpdateProfileRequest.java`, `dto/response/UserProfileResponse.java`, `mapper/UserMapper.java` | Nhận và trả hai field |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/GoalType.java` (mới) | `LOSE`, `MAINTAIN`, `GAIN` |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/NutritionTargetCalculator.java` (mới) | Công thức §3.2, thuần |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanDtos.java` | `GenerateRequest.goalType`, `NutritionTargetResponse`, `MealPlanResponse.goalType` |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java` | Đọc hồ sơ, tính mục tiêu cho `generate`/`updateFromHealth`/`nutritionTarget` |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlan.java` + `db/migration/V5__meal_plan_goal_type.sql` (mới) | Cột `goal_type` |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanController.java` | `GET /ai/meal-plans/nutrition-target` |
| `ai-service/src/main/java/com/veggiepal/ai/common/ErrorCode.java`, `GlobalExceptionHandler.java` | 6020, 6021; 400 cho request sai dạng |
| `ai-service/src/main/resources/application.properties`, `src/test/resources/application.properties` | `services.identity.base-url` |
| `docs/AI_SERVICE_DOCUMENTATION.md` | Hợp đồng mới |

---

### Task 1: identity-service — giới tính và mức vận động trong hồ sơ

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/enums/Gender.java`, `identity-service/src/main/java/com/veggiepal/enums/ActivityLevel.java`
- Modify: `identity-service/src/main/java/com/veggiepal/entity/User.java`, `dto/request/UpdateProfileRequest.java`, `dto/response/UserProfileResponse.java`, `mapper/UserMapper.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/ProfileServiceTest.java`, `identity-service/src/test/java/com/veggiepal/controller/ProfileControllerTest.java`

**Interfaces:**
- Produces: JSON của `GET /users/me` có thêm `"gender"` và `"activityLevel"` (tên hằng enum hoặc `null`). Task 3 đọc hai field này cùng `"dateOfBirth"` (`yyyy-MM-dd`) từ `result`.

- [ ] **Step 1: Viết test fail**

Trong `ProfileServiceTest` (thêm import `com.veggiepal.enums.Gender`, `com.veggiepal.enums.ActivityLevel`):

```java
    @Test
    void updateProfile_setsGenderAndActivityLevel() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserProfileResponse profile = profileService.updateProfile(USER_ID, UpdateProfileRequest.builder()
                .gender(Gender.FEMALE).activityLevel(ActivityLevel.MODERATE).build());

        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getActivityLevel()).isEqualTo(ActivityLevel.MODERATE);
        assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(profile.getActivityLevel()).isEqualTo(ActivityLevel.MODERATE);
    }

    @Test
    void updateProfile_nullGenderAndActivityLevel_keepExistingValues() {
        User user = existingUser();
        user.setGender(Gender.MALE);
        user.setActivityLevel(ActivityLevel.LIGHT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        profileService.updateProfile(USER_ID, new UpdateProfileRequest());

        assertThat(user.getGender()).isEqualTo(Gender.MALE);
        assertThat(user.getActivityLevel()).isEqualTo(ActivityLevel.LIGHT);
    }
```

Trong `ProfileControllerTest` (thêm import `com.veggiepal.enums.Gender`, `com.veggiepal.enums.ActivityLevel`):

```java
    @Test
    void getProfile_returnsGenderAndActivityLevel() throws Exception {
        when(profileService.getProfile(USER_ID)).thenReturn(UserProfileResponse.builder()
                .id(USER_ID).gender(Gender.MALE).activityLevel(ActivityLevel.ACTIVE).build());

        mockMvc.perform(get("/users/me").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.gender").value("MALE"))
                .andExpect(jsonPath("$.result.activityLevel").value("ACTIVE"));
    }

    @Test
    void updateProfile_unknownGender_returnsInvalidRequest() throws Exception {
        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "ROBOT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }
```

- [ ] **Step 2: Chạy, xác nhận fail**

`cd identity-service && ./mvnw test -Dtest='ProfileServiceTest,ProfileControllerTest'`. Kỳ vọng: compile error (chưa có `Gender`, `ActivityLevel`, các getter/builder). Ghi lại làm bằng chứng RED.

- [ ] **Step 3: Cài đặt**

`enums/Gender.java`:

```java
package com.veggiepal.enums;

public enum Gender {
    MALE,
    FEMALE,
    OTHER
}
```

`enums/ActivityLevel.java`:

```java
package com.veggiepal.enums;

public enum ActivityLevel {
    SEDENTARY,
    LIGHT,
    MODERATE,
    ACTIVE,
    VERY_ACTIVE
}
```

**`User`:** thêm import hai enum và hai field, đặt ngay sau `dateOfBirth`:

```java
    // Nullable: the meal planner needs both to estimate calorie needs and asks the user to fill them in.
    // ddl-auto=update creates native MySQL ENUM columns; a constant added later needs a manual ALTER (see CLAUDE.md).
    @Enumerated(EnumType.STRING)
    Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    ActivityLevel activityLevel;
```

**`UpdateProfileRequest`:** thêm sau `dateOfBirth` (import hai enum):

```java
    // null keeps the current value; an unknown name fails JSON parsing (400 INVALID_REQUEST)
    Gender gender;

    ActivityLevel activityLevel;
```

**`UserProfileResponse`:** thêm `Gender gender;` và `ActivityLevel activityLevel;` sau `dateOfBirth` (import hai enum).

**`UserMapper`:**
- `toUser(RegisterRequest)` thêm `@Mapping(target = "gender", ignore = true)` và `@Mapping(target = "activityLevel", ignore = true)`.
- `updateProfile` thêm `@Mapping(target = "gender", source = "gender")` và `@Mapping(target = "activityLevel", source = "activityLevel")`.

- [ ] **Step 4: Chạy test nhanh của identity**

`cd identity-service && ./mvnw test -Dtest='!VeggiepalApplicationTests'`. Kỳ vọng: BUILD SUCCESS, 4 test mới PASS.

- [ ] **Step 5: Dừng, bàn giao cho Long review**

---

### Task 2: ai-service — bộ tính mục tiêu calo

**Files:**
- Create: `ai-service/src/main/java/com/veggiepal/ai/mealplan/GoalType.java`, `ai-service/src/main/java/com/veggiepal/ai/mealplan/NutritionTargetCalculator.java`
- Modify: `ai-service/src/main/java/com/veggiepal/ai/common/ErrorCode.java`, `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanDtos.java` (thêm record `NutritionTargetResponse`)
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/NutritionTargetCalculatorTest.java` (mới)

**Interfaces:**
- Produces:
  - `enum GoalType { LOSE, MAINTAIN, GAIN }` (package `com.veggiepal.ai.mealplan`).
  - `record MealPlanDtos.NutritionTargetResponse(GoalType goalType, int bmr, int tdee, int targetCalories, NutritionTarget target)`.
  - `static MealPlanDtos.NutritionTargetResponse NutritionTargetCalculator.calculate(BigDecimal heightCm, BigDecimal weightKg, LocalDate dateOfBirth, String gender, String activityLevel, GoalType goalType, LocalDate today)`. Ném `AppException(HEALTH_RECORD_REQUIRED)` khi chiều cao hoặc cân nặng null; `AppException(PROFILE_INCOMPLETE)` khi `dateOfBirth`, `gender` hoặc `activityLevel` null hay không nhận ra; `AppException(AGE_NOT_SUPPORTED)` khi dưới 18 tuổi.
  - `static GoalType NutritionTargetCalculator.guessGoal(String goalText)`: dò chữ như code cũ ("giảm"/"loss"/"diet" → LOSE, "tăng"/"gain"/"bulk" → GAIN, còn lại MAINTAIN). Chỉ dùng cho thực đơn cũ có `goal_type` null.
  - `ErrorCode.PROFILE_INCOMPLETE`, `ErrorCode.AGE_NOT_SUPPORTED`.

- [ ] **Step 1: Viết test fail**

`NutritionTargetCalculatorTest.java`:

```java
package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NutritionTargetCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    private static MealPlanDtos.NutritionTargetResponse calc(int kg, int cm, int age, String gender,
            String activity, GoalType goal) {
        return NutritionTargetCalculator.calculate(BigDecimal.valueOf(cm), BigDecimal.valueOf(kg),
                TODAY.minusYears(age), gender, activity, goal, TODAY);
    }

    private static void assertRange(MealPlanDtos.NutritionTargetResponse r, int min, int max) {
        assertEquals(0, BigDecimal.valueOf(min).compareTo(r.target().minCalories()));
        assertEquals(0, BigDecimal.valueOf(max).compareTo(r.target().maxCalories()));
    }

    @Test
    void maleLightGain() {
        var r = calc(80, 180, 25, "MALE", "LIGHT", GoalType.GAIN);
        assertEquals(1805, r.bmr());
        assertEquals(2482, r.tdee());
        assertEquals(2854, r.targetCalories());
        assertRange(r, 2569, 3140);
        assertEquals(GoalType.GAIN, r.goalType());
        assertEquals(0, BigDecimal.valueOf(96).compareTo(r.target().minProtein()));
        assertEquals(0, BigDecimal.valueOf(144).compareTo(r.target().maxProtein()));
        assertEquals(0, BigDecimal.valueOf(321).compareTo(r.target().minCarbohydrate()));
        assertEquals(0, BigDecimal.valueOf(428).compareTo(r.target().maxCarbohydrate()));
        assertEquals(0, BigDecimal.valueOf(63).compareTo(r.target().minFat()));
        assertEquals(0, BigDecimal.valueOf(95).compareTo(r.target().maxFat()));
    }

    @Test
    void femaleLightMaintain() {
        var r = calc(60, 165, 30, "FEMALE", "LIGHT", GoalType.MAINTAIN);
        assertEquals(1320, r.bmr());
        assertEquals(1815, r.tdee());
        assertEquals(1815, r.targetCalories());
        assertRange(r, 1634, 1997);
    }

    @Test
    void femaleSedentaryLoseIsRaisedToTheFloor() {
        var r = calc(45, 150, 60, "FEMALE", "SEDENTARY", GoalType.LOSE);
        assertEquals(927, r.bmr());
        assertEquals(1112, r.tdee());
        assertEquals(1200, r.targetCalories());
        assertRange(r, 1200, 1320);
    }

    @Test
    void maleVeryActiveGainIsCappedAtTheCeiling() {
        var r = calc(150, 200, 20, "MALE", "VERY_ACTIVE", GoalType.GAIN);
        assertEquals(2655, r.bmr());
        assertEquals(5045, r.tdee());
        assertEquals(4000, r.targetCalories());
        assertRange(r, 3600, 4000);
    }

    @Test
    void otherUsesTheAverageOffsetAndTheLowerFloor() {
        // 10 x 60 + 6.25 x 165 - 5 x 30 - 78 = 1403.25
        assertEquals(1403, calc(60, 165, 30, "OTHER", "SEDENTARY", GoalType.MAINTAIN).bmr());
    }

    @Test
    void missingOrUnknownProfileFieldsAreProfileIncomplete() {
        BigDecimal cm = BigDecimal.valueOf(170), kg = BigDecimal.valueOf(65);
        LocalDate dob = TODAY.minusYears(30);
        for (Runnable call : new Runnable[] {
                () -> NutritionTargetCalculator.calculate(cm, kg, null, "MALE", "LIGHT", GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, null, "LIGHT", GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, "MALE", null, GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, "ROBOT", "LIGHT", GoalType.MAINTAIN, TODAY),
                () -> NutritionTargetCalculator.calculate(cm, kg, dob, "MALE", "COUCH", GoalType.MAINTAIN, TODAY)}) {
            AppException ex = assertThrows(AppException.class, call::run);
            assertEquals(ErrorCode.PROFILE_INCOMPLETE, ex.getErrorCode());
        }
    }

    @Test
    void missingHeightOrWeightIsHealthRecordRequired() {
        AppException ex = assertThrows(AppException.class, () -> NutritionTargetCalculator.calculate(
                BigDecimal.valueOf(170), null, TODAY.minusYears(30), "MALE", "LIGHT", GoalType.MAINTAIN, TODAY));
        assertEquals(ErrorCode.HEALTH_RECORD_REQUIRED, ex.getErrorCode());
    }

    @Test
    void underEighteenIsRejectedAndTheEighteenthBirthdayIsAccepted() {
        BigDecimal cm = BigDecimal.valueOf(170), kg = BigDecimal.valueOf(60);
        AppException ex = assertThrows(AppException.class, () -> NutritionTargetCalculator.calculate(
                cm, kg, TODAY.minusYears(18).plusDays(1), "FEMALE", "LIGHT", GoalType.MAINTAIN, TODAY));
        assertEquals(ErrorCode.AGE_NOT_SUPPORTED, ex.getErrorCode());

        assertEquals(GoalType.MAINTAIN, NutritionTargetCalculator.calculate(
                cm, kg, TODAY.minusYears(18), "FEMALE", "LIGHT", GoalType.MAINTAIN, TODAY).goalType());
    }

    @Test
    void guessGoalReadsTheOldFreeText() {
        assertEquals(GoalType.LOSE, NutritionTargetCalculator.guessGoal("Giảm mỡ toàn thân"));
        assertEquals(GoalType.GAIN, NutritionTargetCalculator.guessGoal("Tăng cơ"));
        assertEquals(GoalType.MAINTAIN, NutritionTargetCalculator.guessGoal("Ăn lành mạnh"));
        assertEquals(GoalType.MAINTAIN, NutritionTargetCalculator.guessGoal(null));
    }
}
```

- [ ] **Step 2: Chạy, xác nhận fail**

`cd ai-service && ./mvnw test -Dtest=NutritionTargetCalculatorTest`. Kỳ vọng: compile error. Ghi lại làm RED.

- [ ] **Step 3: Cài đặt**

**`ErrorCode`:** thêm sau `RECIPE_CONTAINS_ALLERGEN`:

```java
    PROFILE_INCOMPLETE(6020, "Vui lòng cập nhật ngày sinh, giới tính và mức độ vận động trong hồ sơ trước khi lập thực đơn.", HttpStatus.BAD_REQUEST),
    AGE_NOT_SUPPORTED(6021, "Thực đơn theo mục tiêu calo hiện chỉ dành cho người từ 18 tuổi trở lên.", HttpStatus.BAD_REQUEST),
```

**`GoalType.java`:**

```java
package com.veggiepal.ai.mealplan;

public enum GoalType {
    LOSE,
    MAINTAIN,
    GAIN
}
```

**`MealPlanDtos`:** thêm:

```java
    public record NutritionTargetResponse(
            GoalType goalType,
            int bmr,
            int tdee,
            int targetCalories,
            NutritionTarget target) {}
```

**`NutritionTargetCalculator.java`:**

```java
package com.veggiepal.ai.mealplan;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Map;

// Mifflin-St Jeor daily calorie target (spec 2026-10-21-calorie-target-design §3.2). Pure: the caller
// supplies today's date so tests can pin ages. All arithmetic stays unrounded until the final results.
public final class NutritionTargetCalculator {
    private static final Map<String, Integer> GENDER_OFFSET = Map.of("MALE", 5, "FEMALE", -161, "OTHER", -78);
    private static final Map<String, Double> ACTIVITY_FACTOR = Map.of(
            "SEDENTARY", 1.2, "LIGHT", 1.375, "MODERATE", 1.55, "ACTIVE", 1.725, "VERY_ACTIVE", 1.9);
    private static final Map<GoalType, Double> GOAL_FACTOR = Map.of(
            GoalType.LOSE, 0.85, GoalType.MAINTAIN, 1.0, GoalType.GAIN, 1.15);
    private static final double CEILING = 4000;
    private static final int ADULT_AGE = 18;

    private NutritionTargetCalculator() {}

    public static MealPlanDtos.NutritionTargetResponse calculate(BigDecimal heightCm, BigDecimal weightKg,
            LocalDate dateOfBirth, String gender, String activityLevel, GoalType goalType, LocalDate today) {
        if (heightCm == null || weightKg == null) throw new AppException(ErrorCode.HEALTH_RECORD_REQUIRED);
        Integer offset = gender == null ? null : GENDER_OFFSET.get(gender);
        Double activity = activityLevel == null ? null : ACTIVITY_FACTOR.get(activityLevel);
        if (dateOfBirth == null || offset == null || activity == null) {
            throw new AppException(ErrorCode.PROFILE_INCOMPLETE);
        }
        int age = Period.between(dateOfBirth, today).getYears();
        if (age < ADULT_AGE) throw new AppException(ErrorCode.AGE_NOT_SUPPORTED);

        double kg = weightKg.doubleValue();
        double bmr = 10 * kg + 6.25 * heightCm.doubleValue() - 5 * age + offset;
        double tdee = bmr * activity;
        double floor = "MALE".equals(gender) ? 1500 : 1200;
        double target = Math.min(Math.max(tdee * GOAL_FACTOR.get(goalType), floor), CEILING);

        MealPlanDtos.NutritionTarget range = new MealPlanDtos.NutritionTarget(
                whole(Math.max(Math.round(target * 0.9), (long) floor)),
                whole(Math.min(Math.round(target * 1.1), (long) CEILING)),
                whole(Math.round(kg * 1.2)), whole(Math.round(kg * 1.8)),
                whole(Math.round(target * 0.45 / 4)), whole(Math.round(target * 0.60 / 4)),
                whole(Math.round(target * 0.20 / 9)), whole(Math.round(target * 0.30 / 9)));
        return new MealPlanDtos.NutritionTargetResponse(goalType, (int) Math.round(bmr), (int) Math.round(tdee),
                (int) Math.round(target), range);
    }

    // Plans saved before goal_type existed only carry free text; read it the way the old code did.
    public static GoalType guessGoal(String goalText) {
        String g = goalText == null ? "" : goalText.toLowerCase(Locale.ROOT);
        if (g.contains("giảm") || g.contains("loss") || g.contains("diet")) return GoalType.LOSE;
        if (g.contains("tăng") || g.contains("gain") || g.contains("bulk")) return GoalType.GAIN;
        return GoalType.MAINTAIN;
    }

    private static BigDecimal whole(long value) {
        return BigDecimal.valueOf(value);
    }
}
```

- [ ] **Step 4: Chạy lại**

`cd ai-service && ./mvnw test -Dtest=NutritionTargetCalculatorTest`. Kỳ vọng: 9/9 PASS. Nếu một ví dụ lệch 1 đơn vị, kiểm tra lại số chưa làm tròn với bảng ví dụ của spec §3.2 trước khi sửa code. Không được sửa số kỳ vọng.

- [ ] **Step 5: Dừng, bàn giao cho Long review**

---

### Task 3: ai-service — dùng mục tiêu tính được trong `generate` và `updateFromHealth`

**Files:**
- Create: `ai-service/src/main/resources/db/migration/V5__meal_plan_goal_type.sql`
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java`, `MealPlan.java`, `MealPlanDtos.java`; `ai-service/src/main/resources/application.properties`, `ai-service/src/test/resources/application.properties`
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanServiceTest.java`, `MealPlanUpgradeServiceTest.java` (và mọi chỗ khác tạo `MealPlanService` hoặc `GenerateRequest` mà compile hỏng)

**Interfaces:**
- Consumes: `NutritionTargetCalculator.calculate(...)`, `guessGoal(...)`, `NutritionTargetResponse`, `GoalType` (Task 2). JSON `result.dateOfBirth`/`gender`/`activityLevel` của `GET /users/me` (Task 1).
- Produces:
  - `public MealPlanDtos.NutritionTargetResponse nutritionTarget(String jwt, GoalType goalType)` trên `MealPlanService` (Task 4 gọi).
  - `GenerateRequest` có thêm thành phần cuối `@NotNull GoalType goalType`.
  - `MealPlanResponse` có thêm thành phần cuối `String goalType`.
  - `MealPlan` có `getGoalType()` và constructor `MealPlan(Long userId, String name, String goal, String goalType, String json)`.

- [ ] **Step 1: Viết test fail**

**`MealPlanServiceTest`:**
- Constructor 10 tham số của `MealPlanService` sẽ dùng URL identity mặc định `http://identity-service` (Step 3).
- Mọi `new MealPlanDtos.GenerateRequest(...)` có sẵn thêm đối số cuối `GoalType.MAINTAIN`.
- Test có sẵn nào truyền `dailyNutritionTarget = null` mà vẫn mong `generate` chạy hết (ví dụ `generateCompletes21MealPlanSuccessfullyWithDeterministicFallbackOnAiError`): giờ BE sẽ gọi identity-service, nên thêm `stubProfile("1995-01-01", "FEMALE", "LIGHT")` vào test đó. Các test mong lỗi từ nutrition-service (404 → 6010, timeout → 6006) không cần stub, vì lỗi xảy ra trước khi gọi identity.
- Thêm hai test, dùng lại helper `addRecipe` và `stubGet` đã có trong file:

```java
    private void stubProfile(String dob, String gender, String activity) {
        ObjectNode profile = mapper.createObjectNode();
        profile.putObject("result").put("dateOfBirth", dob).put("gender", gender).put("activityLevel", activity);
        stubGet("http://identity-service/users/me", profile, true);
    }

    private void stubGenerateDownstreams() {
        ObjectNode recipes = mapper.createObjectNode();
        addRecipe(recipes.putObject("result").putArray("content"), 1, 600);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);
        ObjectNode health = mapper.createObjectNode();
        health.putObject("result").put("weightKg", 80).put("heightCm", 180);
        stubGet("http://nutrition-service/nutrition/me/health-records/latest", health, true);
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class))).thenThrow(new RuntimeException("Groq down"));
        when(repository.save(any(MealPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generateWithoutTargetUsesTheTargetComputedFromTheProfile() {
        stubGenerateDownstreams();
        // 25 years old today, so the expected numbers match the calculator's male/LIGHT/GAIN example
        stubProfile(java.time.LocalDate.now().minusYears(25).toString(), "MALE", "LIGHT");

        MealPlanDtos.MealPlanResponse response = service.generate(1L, "test-jwt", new MealPlanDtos.GenerateRequest(
                "Kế hoạch", "Tăng cơ", List.of(), List.of(), null, GoalType.GAIN));

        var target = mapper.valueToTree(response.plan()).path("constraints").path("dailyNutritionTarget");
        assertEquals(2569, target.path("minCalories").asInt());
        assertEquals(3140, target.path("maxCalories").asInt());
        assertEquals("GAIN", response.goalType());
    }

    @Test
    void generateWithAnExplicitTargetKeepsItAndNeverCallsIdentity() {
        stubGenerateDownstreams();

        MealPlanDtos.MealPlanResponse response = service.generate(1L, "test-jwt", new MealPlanDtos.GenerateRequest(
                "Kế hoạch", "Tăng cơ", List.of(), List.of(),
                new MealPlanDtos.NutritionTarget(new BigDecimal("2000"), new BigDecimal("2200"),
                        null, null, null, null, null, null), GoalType.GAIN));

        var target = mapper.valueToTree(response.plan()).path("constraints").path("dailyNutritionTarget");
        assertEquals(2000, target.path("minCalories").asInt());
        verify(http.get(), org.mockito.Mockito.never()).uri("http://identity-service/users/me");
    }
```

**`MealPlanUpgradeServiceTest`:**
- Constructor 12 tham số: chèn `"http://identity-service"` ngay sau `"http://nutrition-service"`.
- Xóa test `calculateTargetFromHealth_calculatesAppropriateMacros`; Task 2 đã thay nó bằng `NutritionTargetCalculatorTest`.
- Trong helper `stubDownstreams`, thêm một stub hồ sơ hợp lệ, để các test `updateFromHealth` có sẵn vẫn chạy:

```java
        RestClient.RequestHeadersUriSpec profileUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec profileRespSpec = mock(RestClient.ResponseSpec.class);
        ObjectNode profile = mapper.createObjectNode();
        profile.putObject("result").put("dateOfBirth", "1995-01-01").put("gender", "FEMALE").put("activityLevel", "LIGHT");
        when(http.get().uri("http://identity-service/users/me")).thenReturn(profileUriSpec);
        when(profileUriSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(profileUriSpec);
        when(profileUriSpec.retrieve()).thenReturn(profileRespSpec);
        when(profileRespSpec.body(any(Class.class))).thenReturn(profile);
```

- Thêm test:

```java
    @Test
    void updateFromHealth_oldPlanWithoutGoalTypeStillReadsTheFreeTextGoal() {
        // goal_type is null, so "Giảm cân" must be read as LOSE; the stub profile is FEMALE, LIGHT, born 1995-01-01
        MealPlan plan = new MealPlan(USER_ID, "Kế hoạch tuần", "Giảm cân", samplePlanJson(), 1);
        when(repository.findByIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.of(plan));
        ObjectNode healthResult = mapper.createObjectNode();
        healthResult.putObject("result").put("heightCm", 175).put("weightKg", 70);
        ObjectNode allergyResult = mapper.createObjectNode();
        allergyResult.putArray("result");
        ObjectNode recipeResult = mapper.createObjectNode();
        recipeResult.putObject("result").putArray("content").add(sampleRecipe(1, "Đậu hũ kho", true, false));
        stubDownstreams(healthResult, allergyResult, recipeResult);

        MealPlanDtos.UpdateFromHealthResponse res = service.updateFromHealth(USER_ID, PLAN_ID, "jwt-token",
                new MealPlanDtos.UpdateFromHealthRequest(false, null, null));

        MealPlanDtos.NutritionTarget expected = NutritionTargetCalculator.calculate(BigDecimal.valueOf(175),
                BigDecimal.valueOf(70), java.time.LocalDate.of(1995, 1, 1), "FEMALE", "LIGHT", GoalType.LOSE,
                java.time.LocalDate.now()).target();
        assertEquals(expected, res.recalculatedTarget());
    }
```

Chạy `cd ai-service && ./mvnw test -Dtest='MealPlanServiceTest,MealPlanUpgradeServiceTest'`. Kỳ vọng: compile error (chưa có `GoalType` trong `GenerateRequest`, `response.goalType()`, constructor mới). Ghi lại làm RED.

- [ ] **Step 2: Dữ liệu và cấu hình**

`V5__meal_plan_goal_type.sql`:

```sql
ALTER TABLE ai_meal_plans ADD COLUMN goal_type VARCHAR(20) NULL;
```

**`MealPlan`:**
- Field `@Column(name="goal_type",length=20) private String goalType;`, đặt sau `goal`.
- Constructor mới `public MealPlan(Long userId,String name,String goal,String goalType,String json)`: gán như constructor 4 tham số, thêm `this.goalType=goalType`.
- Hai constructor cũ giữ nguyên chữ ký, `goalType` để null (thực đơn tạo qua đường cũ và trong test).

**`MealPlanDtos`:**
- `GenerateRequest` thêm thành phần cuối `@NotNull GoalType goalType` (import `jakarta.validation.constraints.NotNull`).
- `MealPlanResponse` thêm thành phần cuối `String goalType`. Hai constructor tiện lợi có sẵn (6 và 7 tham số) truyền `null` cho nó.

**Cấu hình:** `services.identity.base-url=${IDENTITY_SERVICE_URL:http://localhost:8081}` vào `src/main/resources/application.properties`, cạnh `services.nutrition.base-url`. `services.identity.base-url=http://localhost:1` vào `src/test/resources/application.properties`.

- [ ] **Step 3: `MealPlanService`**

1. **Constructor.**
   - Constructor `@Autowired` thêm `@Value("${services.identity.base-url}") String identityUrl` ngay sau `nutritionUrl`, lưu vào field `private final String identityUrl;`.
   - Constructor 10 tham số truyền `"http://identity-service"` cho tham số mới.

2. **Tính mục tiêu.** Thay `calculateTargetFromHealth` bằng:

```java
    public NutritionTargetResponse nutritionTarget(String jwt, GoalType goalType) {
        JsonNode health = result(get(nutritionUrl + "/nutrition/me/health-records/latest", jwt));
        return computeTarget(health, jwt, goalType);
    }

    // Reads the profile from identity-service only when the backend has to work the target out itself.
    private NutritionTargetResponse computeTarget(JsonNode health, String jwt, GoalType goalType) {
        JsonNode profile = result(get(identityUrl + "/users/me", jwt));
        String dob = profile.path("dateOfBirth").asText(null);
        return NutritionTargetCalculator.calculate(
                health.hasNonNull("heightCm") ? health.path("heightCm").decimalValue() : null,
                health.hasNonNull("weightKg") ? health.path("weightKg").decimalValue() : null,
                dob == null || dob.isBlank() ? null : java.time.LocalDate.parse(dob),
                profile.path("gender").asText(null),
                profile.path("activityLevel").asText(null),
                goalType,
                java.time.LocalDate.now());
    }
```

3. **`generate`.**
   - Sau khi đã lấy `health`, xác định mục tiêu thực dùng:

```java
        NutritionTarget target = request.dailyNutritionTarget() != null
                ? request.dailyNutritionTarget()
                : computeTarget(health, jwt, request.goalType()).target();
        validator.validateTarget(target);
```

   - Bỏ lời gọi `validator.validateTarget(request.dailyNutritionTarget())` ở đầu hàm.
   - Mọi chỗ còn lại của `generate` đang dùng `request.dailyNutritionTarget()` (prompt, `mealBudgetHint`, `validateAndEnrich`, `constraints.dailyNutritionTarget`) chuyển sang dùng `target`.
   - Lưu bằng `new MealPlan(userId, request.name().trim(), request.healthGoal().trim(), request.goalType().name(), plan.toString())`.

4. **`updateFromHealth`.** Thay `calculateTargetFromHealth(health, entity.getGoal())` bằng:

```java
                : computeTarget(health, jwt, entity.getGoalType() != null
                        ? GoalType.valueOf(entity.getGoalType())
                        : NutritionTargetCalculator.guessGoal(entity.getGoal())).target();
```

5. **`view(...)`.** Truyền `plan.getGoalType()` cho thành phần mới của `MealPlanResponse`.

Lưu ý: việc thiếu chiều cao/cân nặng giờ được `NutritionTargetCalculator` kiểm tra (`HEALTH_RECORD_REQUIRED`); hai giá trị mặc định 170/65 biến mất cùng `calculateTargetFromHealth`.

- [ ] **Step 4: Chạy lại test meal plan**

Chạy lệnh test meal plan ở Global Constraints. Kỳ vọng: tất cả PASS.
- Nếu `MealPlanControllerTest` hoặc `MealPlanUpgradeControllerTest` không compile vì chữ ký mới (constructor, `GenerateRequest`, `MealPlanResponse`), sửa chúng theo cùng cách: URL identity mặc định, `GoalType.MAINTAIN`, `goalType` null.
- Nếu `MealPlanUpgradeControllerTest` gửi JSON tạo thực đơn mà không có `goalType`, thêm `"goalType": "MAINTAIN"` vào JSON đó.

- [ ] **Step 5: Dừng, bàn giao cho Long review**

---

### Task 4: ai-service — endpoint `nutrition-target`, lỗi 400 cho request sai dạng, tài liệu

**Files:**
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanController.java`, `ai-service/src/main/java/com/veggiepal/ai/common/GlobalExceptionHandler.java`, `docs/AI_SERVICE_DOCUMENTATION.md`
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanControllerTest.java`

**Interfaces:**
- Consumes: `MealPlanService.nutritionTarget(String jwt, GoalType goalType)` (Task 3), `NutritionTargetResponse` (Task 2).

- [ ] **Step 1: Viết test fail**

Thêm vào `MealPlanControllerTest` (dùng `MockMvcBuilders.standaloneSetup` như test có sẵn, kèm `.setControllerAdvice(new com.veggiepal.ai.common.GlobalExceptionHandler())`):

```java
    private MockMvc mockMvcFor(MealPlanService service) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").claim("userId", 7L).issuedAt(Instant.now()).build()));
        return MockMvcBuilders.standaloneSetup(new MealPlanController(service))
                .setControllerAdvice(new com.veggiepal.ai.common.GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void nutritionTargetReturnsTheComputedTarget() throws Exception {
        MealPlanService service = mock(MealPlanService.class);
        when(service.nutritionTarget("token", GoalType.GAIN)).thenReturn(new MealPlanDtos.NutritionTargetResponse(
                GoalType.GAIN, 1805, 2482, 2854, new MealPlanDtos.NutritionTarget(
                        java.math.BigDecimal.valueOf(2569), java.math.BigDecimal.valueOf(3140),
                        null, null, null, null, null, null)));

        mockMvcFor(service).perform(get("/ai/meal-plans/nutrition-target").param("goalType", "GAIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.goalType").value("GAIN"))
                .andExpect(jsonPath("$.result.bmr").value(1805))
                .andExpect(jsonPath("$.result.targetCalories").value(2854))
                .andExpect(jsonPath("$.result.target.minCalories").value(2569));
    }

    @Test
    void nutritionTargetWithoutGoalTypeIs400() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(get("/ai/meal-plans/nutrition-target"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }

    @Test
    void nutritionTargetWithUnknownGoalTypeIs400() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(get("/ai/meal-plans/nutrition-target").param("goalType", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }

    @Test
    void generateWithoutGoalTypeIs400() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/ai/meal-plans").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tuần này\",\"healthGoal\":\"Tăng cơ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }

    @Test
    void generateWithUnknownGoalTypeIs400NotA500() throws Exception {
        mockMvcFor(mock(MealPlanService.class)).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/ai/meal-plans").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tuần này\",\"healthGoal\":\"Tăng cơ\",\"goalType\":\"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(6000));
    }
```

Chạy `cd ai-service && ./mvnw test -Dtest=MealPlanControllerTest`. Kỳ vọng:
- `nutritionTarget*` fail (endpoint chưa có).
- `generateWithUnknownGoalTypeIs400NotA500` fail với 500, vì `HttpMessageNotReadableException` rơi vào catch-all.
- `generateWithoutGoalTypeIs400` đã PASS nhờ `@NotNull` của Task 3; đó là bằng chứng cho hành vi đó, không phải RED.

- [ ] **Step 2: Cài đặt**

**`MealPlanController`:** thêm ngay sau `list(...)`, trước `@GetMapping("/{id}")`. Path cố định được ưu tiên hơn `/{id}`, giống `/favorites`.

```java
    @GetMapping("/nutrition-target")
    public ApiResponse<NutritionTargetResponse> nutritionTarget(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam GoalType goalType) {
        return ApiResponse.success(service.nutritionTarget(jwt.getTokenValue(), goalType));
    }
```

**`GlobalExceptionHandler`:** thêm (import `org.springframework.http.converter.HttpMessageNotReadableException`, `org.springframework.web.bind.MissingServletRequestParameterException`, `org.springframework.web.method.annotation.MethodArgumentTypeMismatchException`):

```java
    // Malformed requests (bad enum value, missing or mistyped query parameter) are the client's error,
    // not a 500 from the catch-all below.
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(ErrorCode.INVALID_REQUEST.code, ErrorCode.INVALID_REQUEST.message, null));
    }
```

- [ ] **Step 3: Chạy lại toàn bộ test ai-service**

`cd ai-service && ./mvnw test`. Kỳ vọng: BUILD SUCCESS. `AiServiceApplicationTests` chạy Flyway V5 trên H2 và `ddl-auto=validate` cho entity `MealPlan`. Nếu V5 hoặc `validate` lỗi, đó là lỗi thật cần sửa.

- [ ] **Step 4: Tài liệu**

Trong `docs/AI_SERVICE_DOCUMENTATION.md`:
- **Request `POST /ai/meal-plans`:**
  - Thêm `"goalType": "GAIN"` vào ví dụ JSON.
  - Ghi `goalType` bắt buộc (`LOSE`, `MAINTAIN`, `GAIN`).
  - Ghi `dailyNutritionTarget` thành tùy chọn: không gửi thì BE tự tính từ chiều cao/cân nặng (nutrition-service) và ngày sinh, giới tính, mức vận động (identity-service `GET /users/me`). `healthGoal` vẫn là mô tả tự do.
- **Mục mới `#### GET /ai/meal-plans/nutrition-target?goalType=GAIN`:** ví dụ response của spec §3.5, kèm công thức §3.2 dạng bảng ngắn (BMR, hệ số vận động, hệ số mục tiêu, sàn/trần, khoảng ±10%, macro).
- **Mã lỗi:** thêm 6020 `PROFILE_INCOMPLETE`, 6021 `AGE_NOT_SUPPORTED` (dưới 18 tuổi), và 400/6000 cho request sai dạng (`goalType` thiếu hoặc lạ). Ghi rõ hai lỗi hồ sơ chỉ xảy ra khi BE tự tính mục tiêu.
- **`MealPlanResponse`:** thêm field `goalType` (null với thực đơn tạo trước thay đổi này).
- **Dòng "chỉ là gợi ý":** FE hiển thị câu "Mục tiêu calo chỉ là gợi ý, không thay thế tư vấn của chuyên gia dinh dưỡng."

- [ ] **Step 5: Dừng, bàn giao cho Long review**

Báo cho Long:
- Các file đã đổi.
- Số test PASS của identity-service và ai-service.
- FE cần làm theo spec §3.6: trang hồ sơ thêm giới tính và mức vận động; trang thực đơn gửi `goalType`, gọi `nutrition-target`, xử lý 6020/6021.
