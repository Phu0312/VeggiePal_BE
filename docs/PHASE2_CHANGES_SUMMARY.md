# PHASE 2 — AI MEAL PLANNING & RECIPE DATASET — CHANGES SUMMARY

**Dự án:** VeggiePal MVP Backend  
**Branch:** `feat/phase2-meal-planning`  
**Ngày thực hiện:** 17/10/2026  
**Kỹ sư thực hiện:** Senior Spring Boot Microservices Engineer  

---

## 1. TỔNG QUAN KẾT QUẢ PHASE 2

| Task | Mục tiêu | Trạng thái |
|---|---|---|
| **Task 1** | Cải thiện `availableIngredients` thành tiêu chí ưu tiên thay vì bộ lọc bắt buộc (không nới lỏng dị ứng/vegan, không lỗi nếu 0 món khớp) | **HOÀN THÀNH** |
| **Task 2** | Tối ưu thực đơn 7 ngày: Đảm bảo đúng 21 bữa (7 ngày × 3 bữa), không lặp món cùng ngày, kiểm soát lặp tuần, tự sửa Recipe ID ảo/không an toàn | **HOÀN THÀNH** |
| **Task 3** | Tối ưu dinh dưỡng và Macro: Tính từ dữ liệu thật qua `IngredientUnitConverter`, điều chỉnh khẩu phần (0.75x–1.50x), dung sai có cấu hình (15%), trả mã lỗi 6012 (`NUTRITION_TARGET_UNATTAINABLE`) khi không thể đáp ứng | **HOÀN THÀNH** |
| **Task 4** | Mở rộng dataset với 100 món chay Việt Nam thực tế, đầy đủ macro/nguyên liệu/đơn vị/dị ứng/bước nấu; Seeder idempotent không tạo trùng | **HOÀN THÀNH** |
| **Task 5** | Bảo vệ dữ liệu Recipe đã lưu: Chuyển xóa cứng sang soft delete với trạng thái `ARCHIVED`; ẩn khỏi tạo thực đơn mới nhưng giữ hiển thị cho thực đơn cũ | **HOÀN THÀNH** |

---

## 2. CHI TIẾT CÁC THAY ĐỔI THEO TỪNG TASK

### Task 1 — Cải thiện nguyên liệu có sẵn (`availableIngredients`)

- **Vấn đề cũ:** Nếu người dùng truyền danh sách `availableIngredients` mà không có Recipe nào khớp 100%, hệ thống ném `AppException(ErrorCode.NO_VALID_RECIPES)` (HTTP 400).
- **Giải pháp:**
  - `MealPlanValidator.safeCandidates(...)`: Lọc các Recipe an toàn trước (`isVegan`, `isAllergySafe`, `canCalculateMacro`). Nếu không có Recipe an toàn nào, mới ném `NO_VALID_RECIPES`.
  - Nếu `availableIngredients` được cung cấp, tính điểm khớp `countMatchingAvailableIngredients(...)` (số nguyên liệu sẵn có xuất hiện trong công thức).
  - Sắp xếp danh sách ứng viên theo điểm khớp giảm dần (món dùng nhiều nguyên liệu có sẵn xếp đầu). Các món có 0 điểm khớp vẫn được giữ lại phía sau.
  - LLM và bộ tối ưu hóa luôn ưu tiên chọn các món đứng đầu danh sách.

### Task 2 — Tối ưu thực đơn 7 ngày (21 bữa) & Ngăn lặp món

- **Đảm bảo cấu trúc:** `MealPlanValidator.validateAndEnrich(...)` chuẩn hóa đúng 7 ngày (`MONDAY` đến `SUNDAY`) và 3 bữa/ngày (`BREAKFAST`, `LUNCH`, `DINNER`), tổng cộng chính xác 21 bữa.
- **Ngăn lặp món cùng ngày:** Khi số Recipe an toàn $\ge 3$, kiểm tra và thay thế các món trùng trong cùng một ngày bằng món an toàn khác chưa dùng trong ngày.
- **Kiểm soát lặp món trong tuần:** Thuật toán theo dõi tần suất sử dụng món trong tuần (`weeklyUsage`). Khi cần bổ sung/thay thế, ưu tiên chọn Recipe có số lần xuất hiện ít nhất.
- **Xử lý Recipe ID ảo từ Groq:** Nếu LLM trả Recipe ID không tồn tại hoặc vi phạm dị ứng, hệ thống tự động phát hiện và thay thế bằng Recipe an toàn tốt nhất từ danh sách ứng viên.
- **Fallback Deterministic Planner:** Trong `MealPlanService.generate(...)`, nếu Groq AI gặp sự cố timeout/lỗi mạng, hệ thống tự động kích hoạt bộ lập thực đơn xác định (deterministic planner) để trả về thực đơn 21 bữa hợp lệ mà không làm gián đoạn người dùng.

### Task 3 — Tối ưu dinh dưỡng, Macro & Điều chỉnh khẩu phần

- **Tính toán dinh dưỡng chính xác:** Dinh dưỡng calorie, protein, carbohydrate, fat được tính 100% từ nguyên liệu thực tế qua `IngredientUnitConverter.toGrams(...)` kết hợp khẩu phần `servings`. Tuyệt đối không tin số liệu do LLM sinh ra.
- **Điều chỉnh khẩu phần có kiểm soát (`servingMultiplier`):**
  - Cấu hình qua `application.properties`:
    - `ai.meal-plan.target-tolerance-ratio=0.15` (dung sai 15%)
    - `ai.meal-plan.min-portion-scale=0.75` (tối thiểu 0.75 khẩu phần)
    - `ai.meal-plan.max-portion-scale=1.50` (tối đa 1.50 khẩu phần)
  - Nếu tổng năng lượng ngày lệch khỏi mục tiêu (`minCalories`, `maxCalories`), hệ thống tự động tính hệ số khẩu phần tối ưu trong khoảng `[0.75, 1.50]` và cập nhật `servingMultiplier` trên từng món.
- **Xử lý mục tiêu không khả thi:**
  - Thêm mã lỗi mới vào `ErrorCode.java`:
    `NUTRITION_TARGET_UNATTAINABLE(6012, "Mục tiêu dinh dưỡng không thể đáp ứng với các món ăn hiện có.", HttpStatus.BAD_REQUEST)`
  - Nếu mục tiêu dinh dưỡng vượt ngoài khả năng đáp ứng (ngay cả khi đã áp dụng khẩu phần tối đa/tối thiểu và dung sai 15%), hệ thống trả lỗi HTTP 400 với thông điệp rõ ràng, thay vì lỗi 502 `INVALID_AI_RESPONSE`.

### Task 4 — Mở rộng dataset với 100 Recipe món chay thực tế qua `data.sql`

- **Cơ chế nạp dữ liệu:**
  - `recipe-service` sử dụng Hibernate JPA (`ddl-auto=update`) kết hợp Spring SQL Initialization (`spring.jpa.defer-datasource-initialization=true`, `spring.sql.init.mode=always`).
  - Toàn bộ dữ liệu từ 4 Java seeders được chuyển hoá 100% sang [`recipe-service/src/main/resources/data.sql`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/resources/data.sql).
  - 4 class Java Seeder (`RecipeDataSeeder`, `RecipeSeedCatalog1`, `RecipeSeedCatalog2`, `RecipeSeedData`) đã được gỡ bỏ hoàn toàn sau khi xác minh không còn phụ thuộc.
- **Dữ liệu chuẩn hóa:**
  - Bảo lưu 8 nguyên liệu và 2 món mẫu nguyên bản trong `data.sql`.
  - Bổ sung 61 nguyên liệu chay mới (tổng cộng 69 nguyên liệu) với đầy đủ chỉ số dinh dưỡng (cal, p, c, f, fiber/100g) và cờ dị ứng chuẩn.
  - Bổ sung 100 món chay Việt Nam thực tế (tổng cộng 102 món), phân bổ đầy đủ: 20 món ăn sáng, 30 món chính, 20 món giàu đạm, 15 món canh súp, 10 món gỏi/salad, 5 món kho/xào.
  - Mỗi món đều có liên kết nguyên liệu (`recipe_ingredients`), định lượng, đơn vị quy đổi chuẩn (`G`, `ML`, `qua`, `cu`, `muong_canh`, `muong_ca_phe`), hướng dẫn từng bước nấu (`recipe_steps`), và tổng dinh dưỡng theo khẩu phần.
- **Tính Idempotent & Tương thích:**
  - Sử dụng cấu trúc `INSERT INTO ... SELECT ... WHERE NOT EXISTS (...)` cho cả nguyên liệu, món ăn, quan hệ nguyên liệu và các bước nấu.
  - Tra cứu ID khóa ngoại thông qua khóa tự nhiên (`normalized_name` của ingredient, `title` của recipe), tuyệt đối không hardcode ID tự tăng.
  - Khởi động lại service hoặc chạy lại `data.sql` nhiều lần không làm tăng số lượng bản ghi hoặc trùng lặp dữ liệu.
  - Không xóa, ghi đè hoặc can thiệp vào các Recipe do người dùng tự tạo.
  - Tương thích hoàn toàn với MySQL 8.4 và H2 (chế độ MySQL).

### Task 5 — Soft Delete với trạng thái `ARCHIVED`

- **Cập nhật Enum:** Thêm `ARCHIVED` vào [`RecipeStatus.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/java/com/veggiepal/recipe/enums/RecipeStatus.java).
- **Cập nhật Repository:** Thêm `findByUserIdAndStatusNot(Long userId, RecipeStatus status, Pageable pageable)` trong [`RecipeRepository.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/java/com/veggiepal/recipe/repository/RecipeRepository.java).
- **Cập nhật Service:**
  - `RecipeService.deleteRecipe(...)`: Chuyển từ `delete(recipe)` sang `recipe.setStatus(RecipeStatus.ARCHIVED); recipeRepository.save(recipe);`.
  - `RecipeService.getMyRecipes(...)`: Lọc bỏ các món `ARCHIVED`.
  - `RecipeService.getPublishedRecipes(...)`: Chỉ lấy món `PUBLISHED`. Nâng giới hạn kích thước phân trang `Math.clamp(size, 1, 200)` để hỗ trợ AI lấy đầy đủ danh mục.
  - `RecipeService.getRecipe(id)`: Vẫn cho phép truy vấn Recipe `ARCHIVED` theo ID, đảm bảo các Meal Plan cũ đã lưu vẫn hiển thị được chi tiết món ăn.

---

## 3. DANH SÁCH FILE THAY ĐỔI & TẠO MỚI

### `ai-service`
1. [`ai-service/src/main/java/com/veggiepal/ai/common/ErrorCode.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/common/ErrorCode.java) — Thêm mã lỗi `NUTRITION_TARGET_UNATTAINABLE` (6012).
2. [`ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java) — Thuật toán ưu tiên nguyên liệu sẵn có, chuẩn hóa 21 bữa, kiểm soát lặp món, sửa ID ảo, tính portion scale và kiểm tra dung sai mục tiêu.
3. [`ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java) — Tăng size tải recipe lên 200, fallback planner khi AI lỗi, xử lý thay món an toàn.
4. [`ai-service/src/main/resources/application.properties`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties) — Cấu hình dung sai và tỷ lệ co giãn khẩu phần.
5. [`ai-service/src/test/resources/application.properties`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/resources/application.properties) — Cấu hình test tương ứng.
6. [`ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java) — Bổ sung 7 test case mới cho Phase 2 (tổng cộng 18 tests).
7. [`ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanServiceTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanServiceTest.java) — Bổ sung test fallback khi Groq timeout, test thay món, test quyền sở hữu.

### `recipe-service`
1. [`recipe-service/src/main/resources/data.sql`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/resources/data.sql) — Tệp SQL idempotent chứa toàn bộ 69 nguyên liệu và 102 món chay kèm định lượng, đơn vị và các bước nấu.
2. [`recipe-service/src/main/java/com/veggiepal/recipe/enums/RecipeStatus.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/java/com/veggiepal/recipe/enums/RecipeStatus.java) — Thêm trạng thái `ARCHIVED`.
3. [`recipe-service/src/main/java/com/veggiepal/recipe/repository/RecipeRepository.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/java/com/veggiepal/recipe/repository/RecipeRepository.java) — Thêm method tìm kiếm không bao gồm `ARCHIVED`.
4. [`recipe-service/src/main/java/com/veggiepal/recipe/service/RecipeService.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/main/java/com/veggiepal/recipe/service/RecipeService.java) — Triển khai soft delete `ARCHIVED`, nâng `size` phân trang lên 200.
5. [`recipe-service/pom.xml`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/pom.xml) — Thêm H2 test dependency.
6. [`recipe-service/src/test/resources/application.properties`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/test/resources/application.properties) — Cấu hình H2 test database và `defer-datasource-initialization=true`.
7. [`recipe-service/src/test/java/com/veggiepal/recipe/configuration/RecipeSqlDataInitTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/test/java/com/veggiepal/recipe/configuration/RecipeSqlDataInitTest.java) — Test `data.sql` khởi tạo thành công 102 món, 69 nguyên liệu và kiểm tra tính idempotent khi chạy lại script.
8. [`recipe-service/src/test/java/com/veggiepal/recipe/service/RecipeServiceTest.java`](file:///d:/project/VeggiePal/VeganApp_BE/recipe-service/src/test/java/com/veggiepal/recipe/service/RecipeServiceTest.java) — Test soft delete `ARCHIVED`, filter và getRecipe.

*(Đã xóa 4 class Java Seeder theo đúng yêu cầu: `RecipeDataSeeder`, `RecipeSeedCatalog1`, `RecipeSeedCatalog2`, `RecipeSeedData`)*

---

## 4. KẾT QUẢ KIỂM THỬ MAVEN

### `ai-service`
```text
[INFO] Results:
[INFO] Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 19.717 s
```
- Tất cả 70/70 unit & integration tests đều đạt 100%.
- Các kịch bản: lọc dị ứng, quy đổi đơn vị nhiều thứ tiếng, ưu tiên nguyên liệu sẵn có, thực đơn 21 bữa, chống lặp món trong ngày, sửa ID ảo, co giãn khẩu phần theo calorie, mục tiêu không khả thi trả 6012, thay món, kiểm soát hạn mức quota.

### `recipe-service`
```text
[INFO] Results:
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 11.623 s
```
- Tất cả 7/7 tests đều đạt 100%.
- `RecipeSqlDataInitTest`: Khởi tạo thành công 102 recipes và 69 ingredients từ `data.sql`; xác thực tính idempotent khi nạp lại `data.sql` nhiều lần (số lượng không tăng, không trùng lặp).
- `RecipeServiceTest`: Soft delete chuyển trạng thái `ARCHIVED` thành công.

