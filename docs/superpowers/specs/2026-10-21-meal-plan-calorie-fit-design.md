# Chọn món theo ngân sách calo — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-21 |
| Nhánh | `fix/AI-recommendation` |
| Trạng thái | Chờ review |
| Tài liệu gốc | `fix_mealplan.docx` mục 1; task sheet "PHÂN CHIA TASK", US 10 `[BE] Thuật toán sinh Menu AI` |
| Phụ thuộc | `2026-10-21-recipe-nutrition-data-design.md` (dữ liệu món). Thiếu spec đó thì code vẫn chạy, nhưng test chấp nhận ở §4 không đạt được |
| Spec liên quan | `2026-10-21-calorie-target-design.md` (cách tính mục tiêu calo). Spec này chỉ dùng `dailyNutritionTarget`, không quan tâm ai tính ra nó |

## 1. Mục tiêu

BUSINESS RULE của `[BE] Thuật toán sinh Menu AI`:

> Tổng lượng Calo của thực đơn AI trả ra phải nằm trong mức (+/- 10%) so với TDEE của User.

**Cách hiểu đã chốt:** "TDEE" ở đây là mức calo sau khi đã điều chỉnh theo mục tiêu giảm/giữ/tăng. Khoảng `[minCalories, maxCalories]` chính là ±10% quanh mức đó.

Tiêu chí của tài liệu: với mục tiêu khoảng 2.800 kcal, thực đơn tạo ra phải có `targetMet: true` ở đa số ngày.

## 2. Hiện trạng (`MealPlanValidator`)

- **`validateAndEnrich` không nhìn calo khi chọn món.** Món LLM chọn được giữ nếu an toàn và không trùng trong ngày. Bữa nào thiếu món thì lấy món ít dùng nhất trong tuần.
- **`computePortionScale` dùng một hệ số cho cả ngày:** mục tiêu giữa ÷ tổng calo 3 món, kẹp trong [0,75; 1,5]. Ba món nhẹ thì nhân tối đa vẫn thiếu.
- **`markTarget` rộng hơn task sheet và quá chặt với macro.** Cả bốn chỉ số đều phải nằm trong `[min × (1 − 15%), max × (1 + 15%)]`. Với khoảng ±10% của BE, calo đang được chấp nhận từ khoảng −23% đến +26% so với mục tiêu. Trong khi đó chỉ cần protein, carb hoặc fat lệch là cả ngày bị đánh dấu không đạt.
- **`replace()` chạy lại `validateAndEnrich` trên toàn bộ thực đơn**, không chỉ bữa đang thay.

## 3. Thiết kế

### 3.1 Ngân sách calo từng bữa

- **Calo mục tiêu giữa:** `mid = (minCalories + maxCalories) / 2`. Nếu chỉ có một đầu thì dùng đầu đó.
- **Tỉ lệ cố định trong code:** sáng 0,25, trưa 0,40, tối 0,35. Không đưa ra cấu hình.
- **Tính lần lượt từng bữa,** để bữa trước thiếu thì bữa sau gánh:

```
ngân sách bữa i = (mid − calo đã xếp ở các bữa trước) × tỉ lệ bữa i ÷ tổng tỉ lệ các bữa còn lại
```

Ví dụ với `mid = 2.800`: ngân sách sáng là 700. Nếu bữa sáng chỉ đạt 600 thì ngân sách trưa là 2.200 × 0,40 / 0,75 = 1.173, và bữa tối nhận phần còn lại.

### 3.2 Chọn món

Gọi `k` là kcal một khẩu phần của món, `B` là ngân sách của bữa. Món **chạm được** ngân sách khi `B / k` nằm trong `[minScale, maxScale]`.

Với từng bữa:

1. Giữ món LLM chọn nếu món đó an toàn, không trùng trong ngày và chạm được ngân sách.
2. Nếu không, chọn trong các món an toàn chưa dùng trong ngày (riêng khi danh mục có ít hơn 3 món thì được trùng, giữ như code hiện tại):
   - Ưu tiên món chạm được ngân sách; trong số đó lấy món dùng ít nhất trong tuần.
   - Không có món nào chạm được thì lấy món có sai số `|kẹp(B/k) × k − B|` nhỏ nhất.
   - Khi hòa, giữ thứ tự sẵn có của danh sách candidate (vốn đã ưu tiên món dùng nguyên liệu sẵn có).

### 3.3 Co giãn khẩu phần theo từng bữa

- **Hệ số từng bữa:** `s = kẹp(B / k, minScale, maxScale)`, làm tròn 2 chữ số **trước khi nhân**. Nhờ vậy `servingMultiplier` hiển thị nhân với dinh dưỡng gốc ra đúng con số `nutrition` hiển thị.
- **Bỏ `computePortionScale`.**
- **`maxScale`:** mặc định đổi từ 1,50 lên **2,00** (`ai.meal-plan.max-portion-scale`, biến môi trường `AI_MEAL_PLAN_MAX_PORTION_SCALE`; đổi cả default trong `application.properties` lẫn constructor). `minScale` giữ 0,75.

### 3.4 `targetMet`

- **Chỉ xét calo:** `targetMet = minCalories ≤ calo trong ngày ≤ maxCalories`, không cộng thêm dung sai.
- **`targetDeviations` vẫn liệt kê mọi chỉ số nằm ngoài `[min, max]`** (calo, protein, carb, fat), mỗi mục có `actual`, `min`, `max`, để FE hiển thị. Protein, carb và fat không làm `targetMet` thành false.
- **Xóa property `ai.meal-plan.target-tolerance-ratio`** (`AI_MEAL_PLAN_TOLERANCE_RATIO`) vì không còn dùng.
- **`manualEdit` cũng gọi `markTarget`**, nên áp dụng cùng quy tắc.
- **`target` null:** không có `targetMet`, giữ như hiện tại.

### 3.5 Prompt

- **Prompt `generate`:** thêm ngân sách từng bữa (ví dụ "BREAKFAST khoảng 700 kcal, LUNCH khoảng 1.120 kcal, DINNER khoảng 980 kcal"). Thêm một câu nói rõ khẩu phần sẽ được nhân 0,75 đến 2 lần, nên hãy chọn món có `nutritionPerServing.calories` gần ngân sách. `promptCandidates` đã gửi sẵn `nutritionPerServing`.
- **Prompt `replace`:** thêm ngân sách của bữa đang thay.

### 3.6 Khi nào được chọn lại món

`validateAndEnrich` thêm tham số `boolean repick`:

| Nơi gọi | `repick` | Lý do |
|---|---|---|
| `generate` | `true` | Món LLM chọn mà không chạm được ngân sách thì thay bằng món khác |
| `updateFromHealth` | `true` | Mục tiêu đã đổi; người dùng xem bản preview trước khi xác nhận |
| `replace` | `false` | Chỉ bữa đang thay được đổi món; các bữa khác giữ món và chỉ co giãn lại |

**Khi `repick = false`:** món đang có được giữ nếu vẫn an toàn (đúng như hiện tại). Chỉ chọn món mới khi món cũ không còn an toàn hoặc bữa đó chưa có món.

**Thay món trong `replace()`:** món thay phải khác `currentRecipeId` và chạm được ngân sách của bữa đó. Ở đây ngân sách tính thẳng `tỉ lệ × mid`, không tính lần lượt. Nếu LLM chọn món không chạm được thì dùng bộ chọn ở §3.2, đã loại `currentRecipeId`. Hàm `selectAlternativeReplacement` bị bỏ.

### 3.7 Báo lý do khi không đạt

Không thêm mã lý do riêng. `targetDeviations.calories` (gồm `actual`, `min`, `max`) đã đủ để FE hiển thị "còn thiếu X kcal". Chỉ thêm khi FE cần một câu giải thích khác.

## 4. Test

**`MealPlanValidatorTest`** (unit test, dựng recipe ngay trong test như các ca hiện có; constructor test đổi thành `(mapper, 0.75, 2.0)`):

| Ca | Kỳ vọng |
|---|---|
| LLM chọn món 120 kcal cho mọi bữa; danh mục có món 300, 500, 700 kcal; mục tiêu 2.522–3.082 | Ngày nào calo cũng nằm trong khoảng, `targetMet = true`, mọi `servingMultiplier` nằm trong [0,75; 2,0] |
| Món LLM chọn đã chạm được ngân sách | Giữ nguyên `recipeId` |
| Không có món sáng nào chạm được ngân sách sáng | Bữa trưa và tối gánh phần thiếu, cả ngày vẫn đạt |
| Danh mục chỉ có món 50 kcal | Không ném lỗi; `targetMet = false`; `targetDeviations.calories` có `actual`, `min`, `max` |
| Calo đạt nhưng protein dưới `min` | `targetMet = true`, `targetDeviations` có mục `protein` |
| `repick = false` | `recipeId` của các bữa khác không đổi |
| `servingMultiplier` × dinh dưỡng gốc | Bằng đúng `nutrition` hiển thị |

Các ca cũ có dựa vào dung sai 15% hoặc hệ số chung cả ngày thì sửa theo quy tắc mới.

**`MealPlanSeedAcceptanceTest`** (test mới):
- Đọc `src/test/resources/mealplan/recipes-seed.json`, file xuất ra ở spec dữ liệu.
- Chạy với thực đơn LLM rỗng (giống khi Groq lỗi), không có dị ứng.
- Các mục tiêu ±10% quanh 1.500, 2.000, 2.500 và 3.000 kcal, cộng thêm ca trong tài liệu (2.522–3.082).
- Kỳ vọng: **cả 7 ngày `targetMet = true`**.

Test này fail nếu dữ liệu và thuật toán cùng nhau không còn đạt mục tiêu. Khi đổi seed thì xuất lại file JSON.

**`MealPlanUpgradeServiceTest`:** thêm một ca kiểm `replace` chỉ đổi đúng bữa được yêu cầu.

## 5. Tài liệu

`docs/AI_SERVICE_DOCUMENTATION.md`, phần meal plan:
- `targetMet` chỉ xét calo, không còn dung sai 15%.
- `targetDeviations` liệt kê mọi chỉ số lệch.
- `servingMultiplier` tính riêng cho từng bữa, trong khoảng 0,75 đến 2,0.
- Bỏ property dung sai.
- Sửa câu "từng ngày sai min/max … sẽ bị từ chối" (đã cũ: ngày lệch chỉ bị đánh dấu chứ không bị từ chối).

## 6. Không làm

| Phương án | Lý do bỏ |
|---|---|
| Thêm bữa phụ `SNACK` | Lịch tuần ở FE đang hiển thị 3 bữa. Với dữ liệu mới, hệ số ×2 đã đủ cho 3.000 kcal |
| Làm tròn khẩu phần theo bước 0,25 | Hiện đang làm tròn 2 chữ số. Đổi khi FE muốn hiển thị dạng "1¼ khẩu phần" |
| Tối ưu cả protein khi chọn món | Task sheet chỉ yêu cầu calo; protein vẫn được báo trong `targetDeviations` |
| Đưa tỉ lệ bữa ra cấu hình | Không có nhu cầu đổi |
| Mã lý do riêng khi không đạt | Xem §3.7 |

## 7. Vấn đề còn mở

- **Dị ứng làm hẹp danh mục.** Người dị ứng một nhóm phổ biến (ví dụ đậu nành) có thể không còn món nào chạm được ngân sách. Khi đó `targetMet = false` và `targetDeviations` báo phần thiếu. Test chấp nhận chưa có ca này.
- **Bữa thêm bằng sửa tay bị mất (lỗi có từ trước, ngoài phạm vi spec này).** `validateAndEnrich` chỉ giữ 3 bữa chuẩn. Một bữa thêm qua sửa tay với `mealType` khác (ví dụ `SNACK`) sẽ bị bỏ khi `replace` hoặc `updateFromHealth` chạy lại trên thực đơn đó.
