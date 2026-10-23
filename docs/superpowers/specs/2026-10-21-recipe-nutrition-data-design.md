# Dữ liệu dinh dưỡng món ăn — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-21 |
| Nhánh | `fix/AI-recommendation` |
| Trạng thái | Đã triển khai (2026-10-22) |
| Tài liệu gốc | `fix_mealplan.docx` mục 2; task sheet "PHÂN CHIA TASK", US 10 `[BE] Thuật toán sinh Menu AI` |
| Spec liên quan | `2026-10-21-meal-plan-calorie-fit-design.md` (chọn món theo ngân sách calo) dùng dữ liệu của spec này; `2026-10-21-calorie-target-design.md` (tính nhu cầu calo) |

## 1. Mục tiêu

Calo một khẩu phần của mỗi món phải đúng với thực tế, đối chiếu với nguồn tham chiếu, và danh mục phải có đủ món để thực đơn đạt mục tiêu trong khoảng 1.500 đến 3.000 kcal/ngày.

Không tăng số liệu chỉ để đủ mục tiêu. Số nào sai thì sửa cho đúng, thiếu món thì thêm món.

## 2. Hiện trạng

Số liệu đo trên DB local `veggiepal_recipe` ngày 2026-10-21, tính kcal/khẩu phần đúng cách ai-service tính (`kcal/100g × gram ÷ servings`):

- 102 món `PUBLISHED`, món nào cũng `servings = 2`. Có 69 nguyên liệu.
- Trung vị **114 kcal/khẩu phần**. **91/102 món dưới 200 kcal**, cao nhất 396 kcal (Cơm gạo lứt đậu hũ).
- Ba món cao nhất (396 + 306 + 274) nhân ×1,5 chỉ được khoảng **1.460 kcal**, thấp hơn mức 2.522 kcal mà tài liệu ghi nhận. Vì vậy chỉ sửa thuật toán thì không bao giờ đạt mục tiêu.

| # | Vấn đề | Ví dụ | Quy mô |
|---|---|---|---|
| 1 | **Một nguyên liệu bị dùng cho cả trạng thái sống lẫn chín** | "Gạo thơm" lưu 130 kcal/100g (cơm chín), nhưng note của nhiều dòng là "Vo sạch", "Nấu cháo nhừ" (gạo sống, khoảng 345 kcal). Có dòng khác lại ghi "Cơm để nguội". "Gạo lứt" thì lưu 370 (sống). "Đậu lăng đỏ" lưu 116 (chín) nhưng có dòng ghi "Rửa sạch ráo nước". Gạo nếp 150 g ghi "Ngâm nở", miến dong 50 g ghi "Ngâm nở" | 39 món dùng tinh bột đang lưu giá trị chín; 17 dòng đậu các loại |
| 2 | **Thiếu dầu ăn** | Chỉ 1/102 món có "Dầu thực vật" | 44 món có tên xào, chiên, áp chảo, rang, kho, nướng, sốt |
| 3 | **Món phụ hoặc quá ít nguyên liệu** | "Cà tím kho tộ" chỉ có 80 g cà tím, ra 20 kcal | 21 món có ≤ 2 nguyên liệu; 17 món canh, súp, lẩu |
| 4 | **Món bánh mì bị loại mà không báo** | Món 6 và 7 ghi `2 QUA` "Bánh mì lúa mì". `IngredientUnitConverter` không có trọng lượng một ổ bánh mì nên `canCalculateMacro` trả false, và hai món này không bao giờ vào thực đơn | 2 món |
| 5 | **`data.sql` chỉ có `INSERT … WHERE NOT EXISTS`** | Sửa con số trong file không cập nhật DB đã có sẵn (máy các bạn, server) | Toàn bộ seed |

Ví dụ: riêng việc đổi gạo trong "Cháo nấm hạt sen" sang giá trị gạo sống đã đưa món từ 114 lên khoảng 224 kcal/khẩu phần, khớp với mức 200 đến 300 kcal mà tài liệu nêu.

## 3. Thiết kế

### 3.1 Quy ước dữ liệu

- **Mỗi nguyên liệu có đúng một trạng thái:** sống, khô, tươi hoặc đã nấu chín. Trạng thái ghi trong `description`, ví dụ "Gạo thơm hạt dài, sống (chưa nấu)". Giá trị dinh dưỡng là giá trị của đúng trạng thái đó. `normalized_name` giữ nguyên vì đó là khóa.
- **Mỗi dòng `recipe_ingredients` trỏ tới nguyên liệu đúng trạng thái lúc cân**, xác định từ `note` và các bước nấu. Nếu trạng thái cần dùng chưa có thì thêm nguyên liệu mới (ví dụ `Cơm trắng` đã nấu, đặt cạnh `Gạo thơm` sống). Không đổi khối lượng sang lượng tương đương, vì công thức phải còn nấu theo được.
- **Dầu ăn:** món xào, áp chảo hoặc rang có 15 g dầu thực vật cho 2 khẩu phần. Món chiên tính phần dầu thấm vào món, con số cụ thể ghi trong bảng đối chiếu.
- **Gia vị và chất béo khác** (đường, nước cốt dừa, đậu phộng, mè): chỉ thêm khi các bước nấu có dùng và phần đó góp từ 20 kcal/khẩu phần trở lên.
- **Nguồn số liệu:** dùng Bảng thành phần thực phẩm Việt Nam (Viện Dinh dưỡng, 2007) nếu nhóm cung cấp được file, còn lại dùng USDA FoodData Central. Mỗi giá trị ghi rõ mã nguồn.

### 3.2 Bảng đối chiếu (bước 1, nhóm duyệt trước khi sửa seed)

File `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md` gồm bốn bảng:

1. **Nguyên liệu:** tên, trạng thái, giá trị hiện tại và giá trị đề xuất (kcal, đạm, carb, béo, xơ trên 100 g), nguồn.
2. **Món có sẵn:** id, tên, kcal/khẩu phần hiện tại, vấn đề, thay đổi (dòng nào đổi nguyên liệu hoặc khối lượng, thêm dầu), kcal/khẩu phần sau khi sửa.
3. **Món mới:** tên, nguyên liệu kèm khối lượng, các bước, kcal/khẩu phần, bữa phù hợp.
4. **Bảng khả thi:** với từng mức mục tiêu 1.500 / 2.000 / 2.500 / 3.000 kcal, đếm số món chạm được ngân sách của từng bữa. Ngân sách lấy theo spec chọn món: sáng 25%, trưa 40%, tối 35%; một món "chạm được" khi ngân sách ÷ kcal của món nằm trong [0,75; 2,0]. **Ô nào cũng phải có ít nhất 7 món.**

Chỉ sửa `data.sql` sau khi nhóm duyệt bảng này. Người duyệt mặc định là Phú, phụ trách `[BE] Thuật toán sinh Menu AI` theo task sheet, và Long.

### 3.3 Món mới

Khoảng 20 đến 25 món. Con số cuối cùng chốt trong bảng đối chiếu, dựa theo bảng khả thi sau khi đã sửa dữ liệu cũ:

- Món chính 500 đến 800 kcal/khẩu phần: cơm, bún, mì hoặc xôi đi với đậu hũ, tàu hũ ky, các loại đậu, hạt và dầu.
- Khoảng 7 món sáng 400 đến 600 kcal/khẩu phần: xôi, bánh mì, cháo yến mạch, mì xào.

Món mới theo đúng format seed hiện có: `user_id = 1`, `servings = 2`, `PUBLISHED`, `image_url = NULL`, nguyên liệu đơn vị `GRAM`, có các bước nấu. Tất cả đều thuần chay. Nguyên liệu dễ gây dị ứng (đậu nành, đậu phộng, lúa mì, mè, hạt điều) giữ cờ `allergen` giống như ở nguyên liệu có sẵn.

### 3.4 Đưa vào `data.sql`

Mỗi loại thay đổi viết theo một cách. Mọi câu lệnh chạy lại bao nhiêu lần cũng cho cùng kết quả (`data.sql` chạy mỗi lần khởi động), DB tạo mới hay DB có sẵn cuối cùng đều ra cùng một dữ liệu, và cả MySQL lẫn H2 (chế độ MySQL) đều chạy được:

| Thay đổi | Cách viết |
|---|---|
| Sửa giá trị một nguyên liệu có sẵn | Câu `INSERT … WHERE NOT EXISTS` của nguyên liệu đó mang giá trị mới (DB tạo mới nhận luôn), ngay sau là `UPDATE ingredients SET description = '…', calories_per_100g = …, … WHERE normalized_name = '…'` (DB có sẵn). Thêm `name = '…'` khi đổi tên. Không cập nhật `updated_at` |
| Thêm nguyên liệu, món mới, dòng nguyên liệu mới (ví dụ dầu) | `INSERT … WHERE NOT EXISTS` như các câu hiện có; DB có sẵn cũng nhận được |
| Sửa khối lượng hoặc note của một dòng có sẵn | Câu `INSERT` của dòng mang giá trị mới, ngay sau là `UPDATE recipe_ingredients SET quantity = <số>, unit = '<đơn vị>', note = '<note>' WHERE recipe_id IN (SELECT id FROM recipes WHERE title = '…') AND ingredient_id = (SELECT id FROM ingredients WHERE normalized_name = '…')`. Dùng giá trị viết thẳng |
| Đổi một dòng sang nguyên liệu khác trạng thái | Sửa câu `INSERT` tại chỗ để trỏ tới nguyên liệu mới, và thêm `DELETE FROM recipe_ingredients WHERE recipe_id IN (SELECT id FROM recipes WHERE title = '…') AND ingredient_id = (SELECT id FROM ingredients WHERE normalized_name = '<cũ>')` cho dòng cũ trong mục cuối file `-- 6. DATA FIXES`. Dùng `IN` vì `recipes.title` không unique: người dùng có thể đăng món trùng tên, và `= (SELECT …)` khi đó làm `data.sql` lỗi lúc khởi động |

Phần đầu `data.sql` thêm vài dòng chú thích ghi lại quy ước ở §3.1 và bảng trên.

**Ghi chú (2026-10-22):** bản đầu của bảng trên dùng `INSERT … VALUES (…) AS new ON DUPLICATE KEY UPDATE` và `DELETE … JOIN`. Hai cú pháp này chỉ MySQL chạy được, còn test của recipe-service nạp `data.sql` trên H2 (`src/test/resources/application.properties`), nên Long chọn dạng `INSERT … WHERE NOT EXISTS` + `UPDATE` và `DELETE` dùng subquery. Kết quả trên MySQL không đổi.

**Đánh đổi:** câu `UPDATE` ghi đè giá trị mỗi lần khởi động. Nếu admin sửa một nguyên liệu seed qua `PUT /ingredients/{id}` thì lần khởi động sau giá trị sẽ quay về như trong `data.sql`, đúng như location-service đang chạy.

### 3.5 Bánh mì

Thêm `banh mi` vào `IngredientUnitConverter.PIECE_WEIGHTS` của ai-service, với trọng lượng một ổ lấy từ bảng đối chiếu (khoảng 80 đến 90 g).

## 4. Kiểm tra

| Kiểm tra | Cách làm | Bắt được lỗi gì |
|---|---|---|
| Bảng đối chiếu khớp DB | Sau khi sửa, chạy lại query tính kcal/khẩu phần (`sum(kcal_100g × gram ÷ 100) ÷ servings`) và so với cột "sau khi sửa" của bảng, lệch tối đa ±1 kcal | Gõ sai số trong SQL |
| SQL chạy được | `./mvnw test` trong recipe-service (H2: khởi động context, chạy `data.sql`, `RecipeSqlDataInitTest` đếm món và chạy lại `data.sql`); `RecipeServiceApplicationTests` trên MySQL bằng biến môi trường `SPRING_DATASOURCE_*`, `SPRING_JPA_DATABASE_PLATFORM` và `SPRING_JPA_HIBERNATE_DDL_AUTO=update` (cấu hình test mặc định là H2 với `create-drop`) | Lỗi cú pháp, vi phạm khóa |
| Hội tụ | Khởi động recipe-service trên DB có sẵn và trên một DB trống (`--spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_recipe_fresh?createDatabaseIfNotExist=true`). Chạy query trên cả hai thì kết quả phải giống hệt. Khởi động lần hai trên DB có sẵn thì kết quả không đổi. Xóa DB `_fresh` sau khi xong | Thiếu `UPDATE` hoặc `DELETE`, câu lệnh không chạy lại được |
| Đủ món | Mọi ô của bảng khả thi (§3.2) có ít nhất 7 món | Thiếu món cho một mức mục tiêu hoặc một bữa |
| Bánh mì | Một ca mới trong `IngredientUnitConverterTest`: 2 ổ bánh mì đổi được sang gram | Món 6, 7 tiếp tục bị loại |
| Dùng tiếp cho spec chọn món | Xuất `GET /recipes?size=200` thành `ai-service/src/test/resources/mealplan/recipes-seed.json`, chỉ giữ các field validator đọc | (Test chấp nhận nằm trong spec chọn món) |

## 5. Tài liệu

- Phần đầu `data.sql`: ghi quy ước (§3.1 và §3.4).
- `CLAUDE.md`, mục Database gotchas: thêm một gạch đầu dòng: `data.sql` của recipe-service chỉ thêm dòng mới; muốn sửa dòng có sẵn thì thêm `UPDATE` ngay sau câu `INSERT` của nó, muốn bỏ dòng thì dùng `DELETE` (dạng subquery) trong mục `DATA FIXES`; cú pháp phải chạy được trên cả MySQL và H2; mỗi nguyên liệu chỉ có một trạng thái.

## 6. Không làm

| Phương án | Lý do bỏ |
|---|---|
| Thêm cột `meal_types` (nhãn sáng, trưa, tối, phụ) | Ngân sách calo của spec chọn món đã tự loại món phụ, và LLM vẫn chọn kiểu món hợp với từng bữa. Thêm cột nghĩa là phải sửa schema, DTO và form công thức ở FE. Làm khi thực đơn bắt đầu xếp món sai bữa |
| Lưu sẵn kcal/khẩu phần trong bảng `recipes` | ai-service đã tính từ nguyên liệu; lưu thêm thì phải giữ hai nơi đồng bộ |
| Xóa món phụ (canh, rau xào) | Vẫn là công thức hợp lệ để người dùng xem, chỉ là không đủ làm một bữa chính |
| Đổi `servings` | Không phải nguyên nhân gây lệch |

## 7. Vấn đề còn mở

- ai-service đọc `/recipes?size=200`, và recipe-service giới hạn `size` tối đa 200. Khi số món `PUBLISHED` vượt 200 (tính cả món người dùng đăng), phần dư không bao giờ vào thực đơn. Sau spec này có 115 món.
- Món người dùng tự đăng có thể mắc lỗi sống/chín tương tự. Trạng thái ghi trong `description` giúp người dùng chọn đúng nguyên liệu, nhưng hệ thống không kiểm tra.
