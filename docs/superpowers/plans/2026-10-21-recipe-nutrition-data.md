# Dữ liệu dinh dưỡng món ăn Implementation Plan

> **Ghi chú (2026-10-22):** các dạng câu lệnh SQL trong plan này đã đổi sang dạng chạy được cả trên H2 lẫn MySQL: `INSERT … WHERE NOT EXISTS` + `UPDATE` (thay cho `ON DUPLICATE KEY UPDATE`), và `DELETE` dùng subquery `IN` (thay cho `DELETE … JOIN`). Xem spec §3.4.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sửa số liệu dinh dưỡng trong seed của recipe-service cho đúng thực tế và thêm món, để mỗi bữa ở mỗi mức 1.500 / 2.000 / 2.500 / 3.000 kcal có ít nhất 7 món chạm được ngân sách.

**Architecture:** Làm toàn bộ việc đối chiếu trên một DB nháp `veggiepal_recipe_audit`, chép từ DB dev:
- Mọi thay đổi được viết ngay ở dạng câu lệnh seed cuối cùng vào `docs/nutrition-audit/audit-changes.sql`. File bảng đối chiếu (`2026-10-21-recipe-nutrition-audit.md`) lấy số từ hai query cố định (`recipe-kcal.sql`).
- Phú và Long duyệt bảng xong thì các câu lệnh được chuyển vào `recipe-service/src/main/resources/data.sql`.
- Cuối cùng kiểm tra ba DB ra cùng một "dấu vân tay" (`seed-fingerprint.sql`): DB dev có sẵn, DB trống mới tạo và DB nháp.

Riêng ai-service thêm trọng lượng một ổ bánh mì vào `IngredientUnitConverter`.

**Tech Stack:** MySQL 8.4 (Docker `veggiepal-mysql`), Spring Boot SQL init (`data.sql`, `spring.sql.init.mode=always`), Java 21 + JUnit 5 (ai-service), USDA FoodData Central API.

**Spec:** [`docs/superpowers/specs/2026-10-21-recipe-nutrition-data-design.md`](../specs/2026-10-21-recipe-nutrition-data-design.md)

## Global Constraints

**Git và môi trường**
- **Không commit, không push.** Long tự review diff và tự commit. Step cuối mỗi task chỉ dừng lại và bàn giao.
- **Chạy Maven bằng Git Bash** (`./mvnw`) từ trong thư mục service. PowerShell làm hỏng tham số `-Dtest=...`.
- **MySQL:** container `veggiepal-mysql`, host port `3307`, `root`/`12345`. Test của recipe-service cần `DB_PORT=3307 DB_USERNAME=root DB_PASSWORD=12345`. Không đọc và không sửa `.env`.

**Ba database**
- `veggiepal_recipe`: DB dev, đóng vai "DB có sẵn". **Không được thay đổi trước Task 6.**
- `veggiepal_recipe_audit`: DB nháp. Được dựng lại từ `veggiepal_recipe` trong Task 3–5; từ Task 6 thì giữ nguyên, không dựng lại.
- `veggiepal_recipe_fresh`: chỉ dùng trong Task 6.
- Hai DB nháp bị xóa ở cuối Task 6.

**Tham số cố định** (lấy từ spec, phải giống nhau ở mọi chỗ)
- Ngân sách bữa: sáng 0,25, trưa 0,40, tối 0,35.
- Hệ số khẩu phần trong `[0,75; 2,0]`.
- Các mức mục tiêu: 1.500 / 2.000 / 2.500 / 3.000 kcal. Mỗi ô (mức × bữa) phải có **≥ 7** món.
- Một ổ bánh mì = **80 g**, giống nhau ở `IngredientUnitConverter` và `recipe-kcal.sql`.

**Quy ước dữ liệu**
- Mỗi nguyên liệu có đúng một trạng thái (sống, khô, tươi, chín), ghi trong `description`.
- Mỗi dòng nguyên liệu của món trỏ tới nguyên liệu đúng trạng thái lúc cân. Không quy đổi khối lượng sang lượng tương đương.
- Dầu: món xào, áp chảo, rang, phi có `dau an` 15 g cho 2 khẩu phần. Món chiên ngập dầu tính 10% khối lượng phần được chiên, làm tròn tới 5 g.
- Chỉ thêm nguyên liệu khác khi bước nấu có dùng và phần đó góp ≥ 20 kcal/khẩu phần.

**Nguồn số liệu**
- Bảng thành phần thực phẩm Việt Nam (Viện Dinh dưỡng, 2007) nếu Long cung cấp; không thì USDA FoodData Central (SR Legacy hoặc Foundation). Ghi `fdcId` cho mọi giá trị.
- Chỉ đổi giá trị khi lệch nguồn quá 5% hoặc sai trạng thái. **Không bao giờ chỉnh số để đạt mục tiêu.**

**Câu lệnh SQL**
- Mọi câu lệnh trong `data.sql` phải chạy lại nhiều lần mà vẫn ra cùng kết quả.
- Sửa nguyên liệu có sẵn: `INSERT … VALUES (…) AS new ON DUPLICATE KEY UPDATE`.
- Sửa dòng có sẵn: `INSERT … SELECT … ON DUPLICATE KEY UPDATE`, dùng giá trị viết thẳng, không dùng `VALUES()`.
- Bỏ dòng: `DELETE` trong mục `-- 6. DATA FIXES`.
- Thêm mới: `INSERT … WHERE NOT EXISTS`.

**Món mới:** `user_id = 1`, `servings = 2`, `'PUBLISHED'`, `image_url = NULL`, mọi nguyên liệu đơn vị `'GRAM'`, thuần chay, có 3–5 bước nấu.

---

## File Structure

| File | Trách nhiệm |
|---|---|
| `docs/nutrition-audit/recipe-kcal.sql` | Hai query: kcal/khẩu phần từng món (tính giống ai-service) và bảng khả thi |
| `docs/nutrition-audit/seed-fingerprint.sql` | Ba query liệt kê nội dung seed theo tên (không theo id) để so hai DB bằng `diff` |
| `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md` | Bảng đối chiếu (4 bảng của spec §3.2) để Phú và Long duyệt |
| `docs/nutrition-audit/audit-changes.sql` | **Tạm thời.** Mọi thay đổi viết ở dạng seed cuối cùng; bị xóa ở Task 6 sau khi đã chuyển vào `data.sql` |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/IngredientUnitConverter.java` | Thêm trọng lượng ổ bánh mì |
| `ai-service/src/test/java/com/veggiepal/ai/mealplan/IngredientUnitConverterTest.java` | Ca test cho bánh mì |
| `recipe-service/src/main/resources/data.sql` | Seed: upsert nguyên liệu, sửa dòng, thêm dầu, món mới, mục `DATA FIXES` |
| `ai-service/src/test/resources/mealplan/recipes-seed.json` | Ảnh chụp danh mục món sau khi sửa, cho spec chọn món dùng |
| `CLAUDE.md` | Thêm một gạch đầu dòng về seed của recipe-service |

**Lệnh dùng lại nhiều lần** (Git Bash, chạy từ thư mục gốc repo):

```bash
# Chạy một file SQL trên một DB, in dạng bảng
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table <DB> < docs/nutrition-audit/recipe-kcal.sql

# Dựng lại DB nháp = DB dev + audit-changes.sql (chỉ dùng trong Task 3–5)
docker exec veggiepal-mysql mysql -uroot -p12345 -e "DROP DATABASE IF EXISTS veggiepal_recipe_audit; CREATE DATABASE veggiepal_recipe_audit CHARACTER SET utf8mb4"
docker exec veggiepal-mysql sh -c 'mysqldump -uroot -p12345 --default-character-set=utf8mb4 veggiepal_recipe | mysql -uroot -p12345 --default-character-set=utf8mb4 veggiepal_recipe_audit'
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 veggiepal_recipe_audit < docs/nutrition-audit/audit-changes.sql
```

---

### Task 1: Query đo lường và số liệu gốc

**Files:**
- Create: `docs/nutrition-audit/recipe-kcal.sql`
- Create: `docs/nutrition-audit/seed-fingerprint.sql`
- Create: `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md`
- Create: `docs/nutrition-audit/audit-changes.sql` (khung rỗng)

**Interfaces:**
- Produces:
  - `recipe-kcal.sql` in hai bảng:
    - Bảng 1, cột `id`, `title`, `kcal_per_serving`, `protein_per_serving`, `unconvertible_rows`.
    - Bảng 2, cột `target`, `meal`, `budget`, `reachable`.
  - `seed-fingerprint.sql` in ba bảng: nguyên liệu, dòng nguyên liệu của món, các bước.
  - `audit-changes.sql` có ba mục `-- A.`, `-- B.`, `-- C.`, các task sau ghi tiếp vào đây.

- [ ] **Step 1: Viết `recipe-kcal.sql`**

Quy đổi đơn vị phải giống `IngredientUnitConverter`: `GRAM` giữ nguyên; `ML` nhân 0,92 với dầu (`dau an`) và 1,0 với chất lỏng khác; `QUA` của `banh mi` là 80 g. Dòng nào không quy đổi được thì ai-service loại cả món, nên query đếm các dòng đó và bỏ món ra khỏi bảng khả thi.

```sql
-- Kcal/khẩu phần của từng món, tính giống ai-service
-- (MealPlanValidator.tryCalculateMacro + IngredientUnitConverter).
-- Dòng không quy đổi được (grams NULL) làm ai-service loại cả món.
WITH g AS (
    SELECT ri.recipe_id, i.calories_per_100g, i.protein_per_100g,
           CASE
               WHEN ri.unit = 'GRAM' THEN ri.quantity
               WHEN ri.unit = 'ML' AND i.normalized_name = 'dau an' THEN ri.quantity * 0.92
               WHEN ri.unit = 'ML' THEN ri.quantity
               WHEN ri.unit = 'QUA' AND i.normalized_name = 'banh mi' THEN ri.quantity * 80
           END AS grams
    FROM recipe_ingredients ri
    JOIN ingredients i ON i.id = ri.ingredient_id
)
SELECT r.id, r.title,
       ROUND(SUM(g.calories_per_100g * g.grams / 100) / r.servings) AS kcal_per_serving,
       ROUND(SUM(g.protein_per_100g * g.grams / 100) / r.servings, 1) AS protein_per_serving,
       SUM(g.grams IS NULL) AS unconvertible_rows
FROM recipes r
JOIN g ON g.recipe_id = r.id
WHERE r.status = 'PUBLISHED'
GROUP BY r.id, r.title, r.servings
ORDER BY r.id;

-- Bảng khả thi: số món chạm được ngân sách của từng bữa ở từng mức mục tiêu.
-- Chạm được = ngân sách / kcal của món nằm trong [0,75; 2,0]. Spec yêu cầu mỗi ô >= 7.
WITH g AS (
    SELECT ri.recipe_id, i.calories_per_100g,
           CASE
               WHEN ri.unit = 'GRAM' THEN ri.quantity
               WHEN ri.unit = 'ML' AND i.normalized_name = 'dau an' THEN ri.quantity * 0.92
               WHEN ri.unit = 'ML' THEN ri.quantity
               WHEN ri.unit = 'QUA' AND i.normalized_name = 'banh mi' THEN ri.quantity * 80
           END AS grams
    FROM recipe_ingredients ri
    JOIN ingredients i ON i.id = ri.ingredient_id
),
k AS (
    SELECT r.id, SUM(g.calories_per_100g * g.grams / 100) / r.servings AS kcal
    FROM recipes r
    JOIN g ON g.recipe_id = r.id
    WHERE r.status = 'PUBLISHED'
    GROUP BY r.id, r.servings
    HAVING SUM(g.grams IS NULL) = 0
),
t AS (SELECT 1500 AS target UNION ALL SELECT 2000 UNION ALL SELECT 2500 UNION ALL SELECT 3000),
s AS (SELECT 'BREAKFAST' AS meal, 0.25 AS share, 1 AS ord
      UNION ALL SELECT 'LUNCH', 0.40, 2
      UNION ALL SELECT 'DINNER', 0.35, 3)
SELECT t.target, s.meal, ROUND(t.target * s.share) AS budget,
       SUM(t.target * s.share / k.kcal BETWEEN 0.75 AND 2.0) AS reachable
FROM t CROSS JOIN s CROSS JOIN k
GROUP BY t.target, s.meal, s.share, s.ord
ORDER BY t.target, s.ord;
```

- [ ] **Step 2: Chạy trên DB dev và đối chiếu với số liệu gốc**

```bash
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe < docs/nutrition-audit/recipe-kcal.sql
```

Kỳ vọng (đã đo ngày 2026-10-21):
- Bảng 1 có 102 dòng, `unconvertible_rows` đều bằng 0.
- Món 1 "Cơm gạo lứt đậu hũ" = 396; món 5 "Cháo nấm hạt sen" = 114; món 6 = 296; món 7 = 340.
- 89 món dưới 200 kcal. Spec ghi 91 vì ai-service hiện loại món 6 và 7; query này đã tính bánh mì theo 80 g/ổ.
- Bảng 2:

| target | BREAKFAST | LUNCH | DINNER |
|---|---|---|---|
| 1500 | 22 | 3 | 5 |
| 2000 | 6 | 0 | 1 |
| 2500 | 2 | 0 | 0 |
| 3000 | 1 | 0 | 0 |

Số khác thì query sai. Sửa query, không sửa kỳ vọng.

- [ ] **Step 3: Viết `seed-fingerprint.sql`**

So theo tên chứ không theo id, vì nguyên liệu và món mới nhận id tự tăng khác nhau giữa các DB.

```sql
-- Nội dung seed, sắp theo tên (không theo id) để so hai DB bằng diff.
SELECT normalized_name, name, description, calories_per_100g, protein_per_100g,
       carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active
FROM ingredients
ORDER BY normalized_name;

SELECT r.title, r.servings, r.status, i.normalized_name, ri.quantity, ri.unit, ri.note
FROM recipe_ingredients ri
JOIN recipes r ON r.id = ri.recipe_id
JOIN ingredients i ON i.id = ri.ingredient_id
ORDER BY r.title, i.normalized_name;

SELECT r.title, rs.step_number, rs.instruction
FROM recipe_steps rs
JOIN recipes r ON r.id = rs.recipe_id
ORDER BY r.title, rs.step_number;
```

Thử chạy một lần: `docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 -N -B veggiepal_recipe < docs/nutrition-audit/seed-fingerprint.sql | wc -l`. Kỳ vọng: một số dương, không có lỗi SQL.

- [ ] **Step 4: Tạo khung `audit-changes.sql`**

```sql
-- TẠM THỜI: thay đổi đã qua đối chiếu, viết sẵn ở dạng seed cuối cùng.
-- Task 6 chuyển từng câu vào recipe-service/src/main/resources/data.sql rồi xóa file này.
-- Áp lên DB nháp veggiepal_recipe_audit (bản chép của veggiepal_recipe), theo đúng thứ tự A -> B -> C.

-- A. NGUYÊN LIỆU (upsert nguyên liệu có sẵn, INSERT nguyên liệu mới)

-- B. MÓN CÓ SẴN (đổi dòng, sửa khối lượng, thêm dầu; DELETE dòng cũ ở cuối mục)

-- C. MÓN MỚI
```

- [ ] **Step 5: Tạo khung bảng đối chiếu**

`docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md`:

```markdown
# Bảng đối chiếu dinh dưỡng món ăn

Spec: [2026-10-21-recipe-nutrition-data-design.md](../superpowers/specs/2026-10-21-recipe-nutrition-data-design.md).
Số kcal lấy từ `recipe-kcal.sql`: "trước" chạy trên `veggiepal_recipe`, "sau" chạy trên `veggiepal_recipe_audit`.

Trạng thái: Đang đối chiếu

## 1. Nguyên liệu

| normalized_name | Tên | Trạng thái | Hiện tại (kcal / đạm / carb / béo / xơ) | Đề xuất | Nguồn | Ghi chú |
|---|---|---|---|---|---|---|

## 2. Món có sẵn

| id | Món | kcal trước | Vấn đề | Thay đổi | kcal sau |
|---|---|---|---|---|---|

## 3. Món mới

| Món | Bữa | Nguyên liệu (g, cho 2 khẩu phần) | Các bước | kcal/khẩu phần |
|---|---|---|---|---|

## 4. Bảng khả thi

| Mục tiêu | Sáng (trước → sau) | Trưa (trước → sau) | Tối (trước → sau) |
|---|---|---|---|
```

Điền ngay bảng 4 cột "trước" bằng kết quả Step 2 (ví dụ `22 → ?`), và cột "kcal trước" của bảng 2 cho đủ 102 món.

- [ ] **Step 6: Dừng, bàn giao cho Long review**

---

### Task 2: Trọng lượng ổ bánh mì trong ai-service

**Files:**
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/IngredientUnitConverter.java:21-41` (danh sách `PIECE_WEIGHTS`)
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/IngredientUnitConverterTest.java`

**Interfaces:**
- Consumes: `IngredientUnitConverter.toGrams(BigDecimal quantity, String rawUnit, String ingredientName, String normalizedName): Optional<BigDecimal>` (đã có).
- Produces: `toGrams(2, "QUA", "Bánh mì lúa mì", "banh mi")` trả `Optional.of(160.00)`.

- [ ] **Step 1: Viết test fail**

Thêm vào `IngredientUnitConverterTest`:

```java
    @Test
    void convertsBanhMiLoafCount() {
        // Seed recipes 6 and 7 use "2 QUA" of bread; without a loaf weight they were silently excluded.
        Optional<BigDecimal> grams = IngredientUnitConverter.toGrams(new BigDecimal("2"), "QUA", "Bánh mì lúa mì", "banh mi");
        assertTrue(grams.isPresent());
        assertEquals(new BigDecimal("160.00"), grams.get());
    }
```

- [ ] **Step 2: Chạy, xác nhận fail**

```bash
cd ai-service && ./mvnw test -Dtest=IngredientUnitConverterTest
```
Kỳ vọng: `convertsBanhMiLoafCount` FAIL ở `assertTrue(grams.isPresent())`, vì `toGrams` trả `Optional.empty()`.

- [ ] **Step 3: Thêm trọng lượng**

Trong `PIECE_WEIGHTS`, thêm ngay sau dòng `Map.entry("nam dui ga", new BigDecimal("80")),`, giữ quy tắc cụm nhiều chữ đứng trước:

```java
            Map.entry("banh mi", new BigDecimal("80")),
```

- [ ] **Step 4: Chạy lại toàn bộ unit test của meal plan**

```bash
cd ai-service && ./mvnw test -Dtest='IngredientUnitConverterTest,MealPlanValidatorTest,MealPlanServiceTest,MealPlanUpgradeServiceTest'
```
Kỳ vọng: tất cả PASS.

- [ ] **Step 5: Dừng, bàn giao cho Long review**

---

### Task 3: Đối chiếu nguyên liệu (bảng 1, mục A)

**Files:**
- Modify: `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md` (bảng 1)
- Modify: `docs/nutrition-audit/audit-changes.sql` (mục A)

**Interfaces:**
- Consumes: `recipe-kcal.sql` và lệnh dựng lại DB nháp (Task 1).
- Produces:
  - Trạng thái cuối cùng của 69 nguyên liệu có sẵn.
  - Các nguyên liệu mới, mỗi cái có `normalized_name` cố định để Task 4–5 dùng. Ít nhất có `com trang` (Cơm trắng, đã nấu). Mỗi nguyên liệu bị tách theo trạng thái thì thêm một tên mới theo mẫu `<tên cũ> kho` hoặc `<tên cũ> chin`, ví dụ `dau lang do kho`.

- [ ] **Step 1: Liệt kê các dòng đang dùng mỗi nguyên liệu**

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe -e "
SELECT i.normalized_name, i.calories_per_100g AS kcal, r.id, r.title, ri.quantity, ri.unit, ri.note
FROM recipe_ingredients ri
JOIN ingredients i ON i.id = ri.ingredient_id
JOIN recipes r ON r.id = ri.recipe_id
ORDER BY i.normalized_name, r.id;"
```

- [ ] **Step 2: Chốt trạng thái cho từng nguyên liệu**

Mỗi nguyên liệu nhận một trạng thái, theo đa số các dòng dùng nó (đọc `note`, cần thì đọc thêm các bước). Nguyên liệu nào có dòng thuộc hai trạng thái thì tách: giữ trạng thái chiếm đa số cho `normalized_name` cũ, thêm nguyên liệu mới cho trạng thái còn lại.

Những quyết định đã rõ từ note (đã kiểm ngày 2026-10-21):

| normalized_name | Note của các dòng | Quyết định |
|---|---|---|
| `gao thom` | "Vo sạch", "Nấu cháo nhừ", "Nấu dẻo", "Nấu cơm niêu", "Gạo thơm dẻo" = gạo sống; "Cơm để nguội", "Cơm để ráo", "Cơm nấu chín nguội", "Ép chảo làm cơm cháy" = cơm đã nấu | `gao thom` thành **sống**; thêm `com trang` (Cơm trắng, **đã nấu**) cho các dòng cơm |
| `gao nep` | 150 g "Ngâm nở" | **sống** |
| `mien dong` | 50 g "Ngâm nở", "Ngâm mềm", "Trụng mềm cắt khúc" | **khô** (miến được cân lúc chưa ngâm) |
| `dau lang do`, `dau xanh`, `dau ga`, `dau do` | Đa số "Đã hấp mềm", "Luộc chín", "Nấu nhừ" = chín; một vài dòng "Rửa sạch ráo nước", "Đãi vỏ ngâm mềm" = khô | Giữ **chín**; tách loại **khô** cho những đậu có dòng khô |
| `mi soi` | 138 kcal là giá trị USDA của **mì trứng** đã nấu, trong khi món là thuần chay | Chọn giá trị mì lúa mì không trứng, đúng trạng thái theo note |
| `banh mi` | 2 `QUA` | Kiểm tra trọng lượng 80 g/ổ bằng một nguồn. Nếu nguồn ra số khác thì sửa cùng lúc ở `IngredientUnitConverter`, test của Task 2 và `recipe-kcal.sql` |

- [ ] **Step 3: Tra giá trị tham chiếu**

Mỗi nguyên liệu tra USDA FoodData Central:

```bash
curl -s "https://api.nal.usda.gov/fdc/v1/foods/search?api_key=${FDC_API_KEY:-DEMO_KEY}&dataType=SR%20Legacy,Foundation&pageSize=5&query=rice%20white%20long-grain%20raw" \
  | python -I -c "import sys,json
for f in json.load(sys.stdin)['foods']:
    n={x['nutrientId']:x.get('value') for x in f['foodNutrients']}
    print(f['fdcId'], f['description'], 'kcal',n.get(1008),'P',n.get(1003),'C',n.get(1005),'F',n.get(1004),'fiber',n.get(1079))"
```

- **Mã nutrient:** 1008 = năng lượng (kcal), 1003 = đạm, 1005 = carb, 1004 = béo, 1079 = xơ.
- **Chọn mục đúng trạng thái:** mô tả có "raw"/"dry" cho đồ sống/khô, "cooked"/"boiled" cho đồ chín. Mục chín thì lấy loại không thêm muối, không thêm dầu.
- **Gặp HTTP 429 (DEMO_KEY bị giới hạn):** dừng lại, nhờ Long đăng ký key miễn phí ở https://api.data.gov/signup/ và tự đặt biến môi trường `FDC_API_KEY`. Không ghi key vào file nào.
- **Nguyên liệu Việt Nam mà USDA không có** (rau ngót, mồng tơi, củ hũ dừa, bắp chuối, dọc mùng, mít non…):
  - Nếu Long cung cấp Bảng thành phần thực phẩm Việt Nam thì lấy số từ đó.
  - Nếu không, và là rau dưới 50 kcal/100 g: giữ giá trị hiện tại, ghi "chưa đối chiếu, rau ít năng lượng".

- [ ] **Step 4: Điền bảng 1**

Mỗi nguyên liệu (69 cái có sẵn và các cái mới) một dòng:
- `normalized_name`, tên, trạng thái.
- Giá trị hiện tại và giá trị đề xuất (kcal / đạm / carb / béo / xơ).
- Nguồn dạng `USDA fdcId <số>: <mô tả>`.
- Ghi chú lý do đổi, hoặc "giữ nguyên, lệch ≤ 5%".

- [ ] **Step 5: Viết mục A của `audit-changes.sql`**

**Nguyên liệu có sẵn bị đổi giá trị hoặc mô tả:** mỗi cái một câu upsert, ghi đầy đủ các cột. Ví dụ, thay số bằng giá trị đã tra ở Step 3:

```sql
INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
VALUES ('Gạo thơm', 'gao thom', 'Gạo thơm hạt dài, sống (chưa nấu)', 365, 7.1, 80.0, 0.7, 1.3, TRUE, FALSE, TRUE, NOW(), NOW()) AS new
ON DUPLICATE KEY UPDATE
    description = new.description,
    calories_per_100g = new.calories_per_100g,
    protein_per_100g = new.protein_per_100g,
    carbs_per_100g = new.carbs_per_100g,
    fat_per_100g = new.fat_per_100g,
    fiber_per_100g = new.fiber_per_100g;
```

**Nguyên liệu mới:** viết giống format seed hiện có. `vegan`, `allergen` và `active` lấy theo nguyên liệu gốc khi được tách ra.

```sql
INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Cơm trắng', 'com trang', 'Cơm gạo trắng đã nấu chín', 130, 2.7, 28.2, 0.3, 0.4, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'com trang');
```

Nguyên liệu nào chỉ đổi trạng thái trong mô tả mà số liệu vẫn đúng thì vẫn upsert, để `description` mới đi vào DB.

- [ ] **Step 6: Dựng lại DB nháp và xác nhận SQL chạy được**

Chạy ba lệnh "dựng lại DB nháp" ở đầu plan, rồi:

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe_audit -e "SELECT normalized_name, description, calories_per_100g FROM ingredients WHERE normalized_name IN ('gao thom','com trang','gao nep','mien dong');"
```
Kỳ vọng: không có lỗi SQL; `gao thom` mang giá trị gạo sống (khoảng 365 kcal); có dòng `com trang` 130 kcal.

- [ ] **Step 7: Dừng, bàn giao cho Long review**

---

### Task 4: Đối chiếu 102 món có sẵn (bảng 2, mục B)

**Files:**
- Modify: `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md` (bảng 2)
- Modify: `docs/nutrition-audit/audit-changes.sql` (mục B)

**Interfaces:**
- Consumes: các nguyên liệu tạo ở Task 3 (`com trang` và các loại đậu khô/chín đã tách). Dầu dùng `dau an` (Dầu thực vật, 884 kcal, đã có sẵn).
- Produces: cột "kcal sau" của bảng 2, đo trên DB nháp.

- [ ] **Step 1: Xem từng món kèm nguyên liệu và các bước**

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe -e "
SELECT r.id, r.title, i.normalized_name, ri.quantity, ri.unit, ri.note,
       (SELECT GROUP_CONCAT(rs.instruction ORDER BY rs.step_number SEPARATOR ' / ')
          FROM recipe_steps rs WHERE rs.recipe_id = r.id) AS steps
FROM recipes r
JOIN recipe_ingredients ri ON ri.recipe_id = r.id
JOIN ingredients i ON i.id = ri.ingredient_id
WHERE r.id BETWEEN 1 AND 20
ORDER BY r.id, i.normalized_name;"
```
Làm lần lượt theo từng nhóm 20 món (`BETWEEN 21 AND 40`, …) cho tới món 102.

- [ ] **Step 2: Áp quy tắc cho từng món, ghi vào bảng 2**

Với mỗi món:
1. **Sai trạng thái:** dòng nào trỏ tới nguyên liệu khác trạng thái lúc cân (theo kết quả Task 3) thì đổi sang nguyên liệu đúng. Ví dụ: dòng `gao thom` có note "Cơm để nguội" thì đổi sang `com trang`.
2. **Thiếu dầu:** tên hoặc các bước có xào, áp chảo, rang, phi, "chảo dầu" mà món chưa có dòng `dau an` thì thêm `dau an` 15 g, note "Dầu để xào". Món chiên ngập dầu thì tính 10% khối lượng phần được chiên, làm tròn tới 5 g, note "Dầu chiên thấm vào món".
3. **Thiếu nguyên liệu nhiều năng lượng:** bước nấu có dùng một nguyên liệu (đường cho món kho/rim/chè, nước cốt dừa, đậu phộng, mè) mà phần đó góp ≥ 20 kcal/khẩu phần thì thêm dòng. Lượng lấy theo bước nấu; bước không ghi lượng thì dùng 1 muỗng canh (đường 12,5 g; mè 9 g; đậu phộng 15 g). Đường cần nguyên liệu `duong`; nếu Task 3 chưa có thì quay lại Task 3 thêm, kèm nguồn.
4. **Khối lượng:** chỉ sửa khi rõ ràng là gõ nhầm (ví dụ 2 g cho một nguyên liệu chính). Không chỉnh khối lượng để tăng calo.

Bảng 2 ghi: id, tên món, kcal trước, vấn đề (ví dụ "gạo tính giá trị cơm chín; thiếu dầu"), thay đổi (ví dụ "`gao thom` → giá trị sống (mục A); +`dau an` 15 g"), kcal sau (điền ở Step 4). Món không đổi gì thì ghi "Không đổi".

- [ ] **Step 3: Viết mục B của `audit-changes.sql`**

Mỗi loại thay đổi theo đúng một mẫu:

```sql
-- Đổi dòng sang nguyên liệu khác trạng thái: chèn dòng mới (mẫu INSERT của seed),
-- dòng cũ bị xóa ở cuối mục B.
INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 300, 'GRAM', 'Cơm để nguội'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'com trang'
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

-- Thêm dầu (hoặc nguyên liệu khác): cùng mẫu INSERT … WHERE NOT EXISTS.
INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'GRAM', 'Dầu để xào'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau an'
WHERE r.title = 'Rau muống xào tỏi hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

-- Sửa khối lượng/note của dòng có sẵn: upsert theo uk_recipe_ingredient, giá trị viết thẳng.
INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Ngâm nở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mien dong'
WHERE r.title = '<tên món>'
ON DUPLICATE KEY UPDATE quantity = 50, unit = 'GRAM', note = 'Ngâm nở';

-- Cuối mục B: xóa dòng cũ của mọi dòng đã đổi nguyên liệu (mỗi món một câu).
DELETE ri FROM recipe_ingredients ri
JOIN recipes r ON r.id = ri.recipe_id
JOIN ingredients i ON i.id = ri.ingredient_id
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay' AND i.normalized_name = 'gao thom';
```

Câu "Đổi dòng" giữ đúng khối lượng và note của dòng cũ (300 g và "Cơm để nguội" chỉ là ví dụ; lấy số thật từ Step 1).

- [ ] **Step 4: Dựng lại DB nháp, đo và điền "kcal sau"**

Chạy ba lệnh dựng lại DB nháp, rồi:

```bash
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe_audit < docs/nutrition-audit/recipe-kcal.sql
```

Kỳ vọng:
- Không có lỗi SQL; vẫn đúng 102 món; `unconvertible_rows` đều bằng 0.
- "Cháo nấm hạt sen" (món 5) nằm trong khoảng 200–260 kcal (gạo đổi sang giá trị sống).
- Điền "kcal sau" cho đủ 102 dòng của bảng 2.

- [ ] **Step 5: Áp `audit-changes.sql` thêm một lần nữa lên DB nháp**

```bash
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 veggiepal_recipe_audit < docs/nutrition-audit/audit-changes.sql
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe_audit < docs/nutrition-audit/recipe-kcal.sql
```
Kỳ vọng: không có lỗi; kcal giống hệt lần đo ở Step 4. Lệch là có câu chạy lại không ra cùng kết quả; sửa câu đó.

- [ ] **Step 6: Dừng, bàn giao cho Long review**

---

### Task 5: Món mới và bảng khả thi (bảng 3–4, mục C), rồi cổng duyệt

**Files:**
- Modify: `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md` (bảng 3, 4; dòng "Trạng thái")
- Modify: `docs/nutrition-audit/audit-changes.sql` (mục C)

**Interfaces:**
- Consumes: DB nháp sau Task 4; bảng khả thi của `recipe-kcal.sql`.
- Produces: các món mới ở dạng seed, tiêu đề duy nhất (Task 6 dùng tiêu đề làm khóa). Mọi ô của bảng khả thi ≥ 7.

- [ ] **Step 1: Đo bảng khả thi trên DB nháp**

```bash
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe_audit < docs/nutrition-audit/recipe-kcal.sql
```
Ghi lại những ô còn dưới 7.

- [ ] **Step 2: Thiết kế món theo khoảng kcal phục vụ được cả 4 mức**

Món chạm được ngân sách `B` khi kcal của món nằm trong `[B / 2,0; B / 0,75]`. Lấy giao của 4 mức mục tiêu thì một món nằm trong các khoảng sau sẽ chạm được ngân sách bữa đó ở **cả 4 mức**:

| Bữa | Khoảng kcal/khẩu phần phục vụ cả 4 mức |
|---|---|
| Sáng | 375–500 |
| Trưa | 600–800 |
| Tối | 525–700 |

Một món 600–700 kcal phục vụ được cả trưa lẫn tối.

- **Số lượng:** chỉ thêm đủ để mọi ô đạt ≥ 7 (spec ước khoảng 20–25). Món sẵn có đã nằm trong khoảng thì không phải thêm thay.
- **Kiểu món:**
  - Món sáng: xôi, bánh mì, cháo yến mạch, mì xào.
  - Món chính: cơm, bún, mì hoặc xôi đi với đậu hũ, tàu hũ ky, đậu, hạt, có dầu.
- **Nguyên liệu:** ưu tiên dùng nguyên liệu đã đối chiếu ở Task 3. Nguyên liệu mới thì quay lại Task 3 (bảng 1, mục A) kèm nguồn.
- **Calo phải đến từ khẩu phần thật:** lượng tinh bột và đạm như một suất ăn bình thường của người lớn. Không nhồi dầu hay hạt cho đủ số.

- [ ] **Step 3: Viết mục C của `audit-changes.sql`**

Mỗi món theo đúng format khối "RECIPE 3" trong `data.sql`: một `INSERT INTO recipes … WHERE NOT EXISTS`, mỗi nguyên liệu một `INSERT INTO recipe_ingredients … AND NOT EXISTS`, mỗi bước một `INSERT INTO recipe_steps … AND NOT EXISTS`. Ví dụ phần đầu một món:

```sql
-- ------------------------------------------------------------
-- RECIPE 103: Cơm đậu hũ sốt cà chua đậu phộng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm đậu hũ sốt cà chua đậu phộng', 'Cơm trắng với đậu hũ chiên sốt cà chua, rắc đậu phộng rang, đủ năng lượng cho bữa trưa.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm đậu hũ sốt cà chua đậu phộng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 400, 'GRAM', 'Cơm nấu chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'com trang'
WHERE r.title = 'Cơm đậu hũ sốt cà chua đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cắt đậu hũ miếng vuông, chiên vàng các mặt rồi vớt ra để ráo dầu.'
FROM recipes r
WHERE r.title = 'Cơm đậu hũ sốt cà chua đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);
```

Đánh số tiếp từ "RECIPE 103". Tiêu đề phải khác mọi món đã có:

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 veggiepal_recipe_audit -e "SELECT title FROM recipes GROUP BY title HAVING COUNT(*) > 1;"
```
Kỳ vọng: không trả dòng nào.

- [ ] **Step 4: Dựng lại DB nháp, đo, lặp tới khi đạt**

Chạy ba lệnh dựng lại DB nháp, rồi chạy `recipe-kcal.sql`. Ô nào còn dưới 7 thì quay lại Step 2. Khi đạt:
- Điền bảng 3: món, bữa, nguyên liệu, các bước, kcal/khẩu phần lấy từ bảng 1 của query.
- Điền cột "sau" của bảng 4.

Kỳ vọng: mọi ô của bảng 4 ≥ 7; bảng 1 của query có `unconvertible_rows` đều bằng 0.

- [ ] **Step 5: Cổng duyệt**

Đổi dòng "Trạng thái" của bảng đối chiếu thành `Chờ Phú và Long duyệt`. **Dừng hẳn.** Báo Long rằng bảng đối chiếu và `audit-changes.sql` đã sẵn sàng để nhóm duyệt.

Task 6 chỉ bắt đầu khi Long xác nhận nhóm đã duyệt. Nếu nhóm yêu cầu sửa: sửa `audit-changes.sql` và các bảng, dựng lại DB nháp, đo lại, rồi duyệt lại.

---

### Task 6: Đưa vào `data.sql` và kiểm tra hội tụ

**Files:**
- Modify: `recipe-service/src/main/resources/data.sql`
- Modify: `CLAUDE.md` (mục Database gotchas)
- Create: `ai-service/src/test/resources/mealplan/recipes-seed.json`
- Modify: `docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md` (dòng "Trạng thái")
- Delete: `docs/nutrition-audit/audit-changes.sql`

**Interfaces:**
- Consumes: `audit-changes.sql` đã duyệt; `veggiepal_recipe_audit` từ đây giữ nguyên, **không dựng lại** (từ Step 6, DB dev đã chạy `data.sql` mới).
- Produces: `recipes-seed.json`, là mảng JSON các món. Mỗi món có `id`, `title`, `servings`, `ingredients[]`; mỗi nguyên liệu có `ingredientName`, `normalizedName`, `quantity`, `unit`, `caloriesPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`, `vegan`, `allergen`. Spec chọn món dùng file này.

- [ ] **Step 1: Ghi quy ước vào đầu `data.sql`**

Thay khối chú thích đầu file bằng:

```sql
-- ============================================================
-- VEGGIEPAL RECIPE SERVICE - SAMPLE DATA
-- ============================================================
-- Chạy mỗi lần khởi động; mọi câu lệnh chạy lại nhiều lần vẫn ra cùng kết quả.
-- Mỗi nguyên liệu có đúng một trạng thái (sống / khô / tươi / chín), ghi trong description,
-- và dòng nguyên liệu của món trỏ tới nguyên liệu đúng trạng thái lúc cân.
-- Sửa giá trị nguyên liệu có sẵn: INSERT ... VALUES (...) AS new ON DUPLICATE KEY UPDATE.
-- Sửa khối lượng/note dòng có sẵn: INSERT ... SELECT ... ON DUPLICATE KEY UPDATE (giá trị viết thẳng).
-- Bỏ một dòng: DELETE trong mục 6. DATA FIXES. Thêm mới: INSERT ... WHERE NOT EXISTS.
-- Số liệu đối chiếu: docs/nutrition-audit/2026-10-21-recipe-nutrition-audit.md
```

- [ ] **Step 2: Chuyển mục A**

- **Nguyên liệu có sẵn:** mỗi câu upsert của mục A **thay thế** câu `INSERT INTO ingredients … WHERE NOT EXISTS` của chính nguyên liệu đó, ở đúng vị trí cũ (mục 1 hoặc mục 4).
- **Nguyên liệu mới:** đặt ở cuối mục 4, ngay trước dòng `-- 5. 100 VIETNAMESE VEGAN RECIPES (PHASE 2)`.

- [ ] **Step 3: Chuyển mục B**

- **Đổi dòng sang nguyên liệu khác trạng thái:** sửa **ngay câu `INSERT` cũ** của dòng đó trong khối món (đổi `normalized_name` trong `JOIN`, giữ khối lượng và note).
- **Sửa khối lượng/note:** thay câu `INSERT` cũ của dòng đó bằng câu upsert.
- **Dòng mới (dầu, đường…):** đặt ngay sau câu `INSERT INTO recipe_ingredients` cuối cùng của món đó.
- **Các câu `DELETE`:** chuyển vào mục mới ở cuối file:

```sql
-- ============================================================
-- 6. DATA FIXES (2026-10, đối chiếu dinh dưỡng)
-- Bỏ các dòng mà DB tạo trước đợt đối chiếu còn giữ.
-- ============================================================
```

- [ ] **Step 4: Chuyển mục C**

Dán nguyên mục C ngay sau khối của món 102 ("Nấm bào ngư xào sả ớt lá lốt") và **trước** mục `6. DATA FIXES`. Mở đầu bằng:

```sql
-- ============================================================
-- 5b. MÓN ĐỦ NĂNG LƯỢNG (2026-10, đối chiếu dinh dưỡng)
-- ============================================================
```

- [ ] **Step 5: DB trống: chạy seed mới từ đầu**

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 -e "DROP DATABASE IF EXISTS veggiepal_recipe_fresh"
cd recipe-service && SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3307/veggiepal_recipe_fresh?createDatabaseIfNotExist=true' DB_USERNAME=root DB_PASSWORD=12345 ./mvnw test -Dtest=RecipeServiceApplicationTests
```
Kỳ vọng: `BUILD SUCCESS`. Context khởi động được nghĩa là Hibernate đã tạo bảng và `data.sql` chạy hết, không có lỗi SQL.

- [ ] **Step 6: DB có sẵn: chạy seed mới hai lần**

```bash
cd recipe-service && DB_PORT=3307 DB_USERNAME=root DB_PASSWORD=12345 ./mvnw test -Dtest=RecipeServiceApplicationTests
cd recipe-service && DB_PORT=3307 DB_USERNAME=root DB_PASSWORD=12345 ./mvnw test -Dtest=RecipeServiceApplicationTests
```
Kỳ vọng: cả hai lần đều `BUILD SUCCESS`.

- [ ] **Step 7: So dấu vân tay của ba DB**

Đặt `FP` là thư mục nháp của phiên làm việc (scratchpad), không phải thư mục trong repo:

```bash
for db in veggiepal_recipe veggiepal_recipe_fresh veggiepal_recipe_audit; do
  docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 -N -B $db < docs/nutrition-audit/seed-fingerprint.sql > "$FP/fp_$db.tsv"
done
diff "$FP/fp_veggiepal_recipe.tsv" "$FP/fp_veggiepal_recipe_fresh.tsv" && diff "$FP/fp_veggiepal_recipe.tsv" "$FP/fp_veggiepal_recipe_audit.tsv" && echo CONVERGED
```

Kỳ vọng: in ra `CONVERGED`.
- **Lệch giữa DB dev và DB trống:** thiếu một câu upsert hoặc `DELETE`.
- **Lệch với DB nháp:** chuyển sai một câu từ `audit-changes.sql`.

Nếu DB dev có món do người dùng tự tạo qua API thì những món đó chỉ có ở DB dev. Khi đó so riêng các tiêu đề có mặt ở cả ba DB.

- [ ] **Step 8: Đối chiếu số với bảng đối chiếu**

```bash
docker exec -i veggiepal-mysql mysql -uroot -p12345 --default-character-set=utf8mb4 --table veggiepal_recipe_fresh < docs/nutrition-audit/recipe-kcal.sql
```
Kỳ vọng:
- kcal của từng món khớp cột "kcal sau" (bảng 2) và bảng 3, lệch tối đa ±1.
- Bảng khả thi khớp cột "sau" của bảng 4, mọi ô ≥ 7.
- `unconvertible_rows` đều bằng 0.

- [ ] **Step 9: Xuất `recipes-seed.json`**

Chạy recipe-service trên DB dev ở nền: `cd recipe-service && DB_PORT=3307 DB_USERNAME=root DB_PASSWORD=12345 ./mvnw spring-boot:run` (dùng `run_in_background`). Chờ log có `Started RecipeServiceApplication`, rồi chạy từ thư mục gốc repo:

```bash
mkdir -p ai-service/src/test/resources/mealplan
curl -s "http://localhost:8084/recipes?size=200" | python -I -c "import sys,json
keep=('ingredientName','normalizedName','quantity','unit','caloriesPer100g','proteinPer100g','carbsPer100g','fatPer100g','vegan','allergen')
rs=json.load(sys.stdin)['result']['content']
out=[{'id':r['id'],'title':r['title'],'servings':r['servings'],'ingredients':[{k:i[k] for k in keep} for i in r['ingredients']]} for r in rs]
json.dump(out,sys.stdout,ensure_ascii=False,indent=1)" > ai-service/src/test/resources/mealplan/recipes-seed.json
python -I -c "import json;d=json.load(open('ai-service/src/test/resources/mealplan/recipes-seed.json',encoding='utf-8'));print(len(d), sum(len(r['ingredients']) for r in d))"
```

Kỳ vọng: số món bằng số món `PUBLISHED` trong DB dev (102 cộng số món mới); món nào cũng có `ingredients`. Xong thì dừng recipe-service.

Nếu `RecipeResponse` đặt tên field khác (ví dụ `KeyError: 'normalizedName'`): đọc `recipe-service/src/main/java/com/veggiepal/recipe/dto/response/RecipeIngredientResponse.java`, rồi sửa danh sách `keep` cho đúng tên field mà `MealPlanValidator` đọc.

- [ ] **Step 10: Cập nhật `CLAUDE.md`**

Thêm vào cuối danh sách "Database gotchas":

```markdown
- recipe-service's `data.sql` runs on every start and is mostly `INSERT … WHERE NOT EXISTS`, so editing a value in place never reaches an existing database. Change an existing ingredient with `INSERT … VALUES (…) AS new ON DUPLICATE KEY UPDATE`, an existing recipe row with `INSERT … SELECT … ON DUPLICATE KEY UPDATE` (literal values), and drop rows with a `DELETE` in its `6. DATA FIXES` section. Each ingredient has exactly one state (raw, dry, fresh, cooked) stated in its `description`; a recipe row must point at the ingredient in the state it was weighed in. Nutrition sources are in `docs/nutrition-audit/`.
```

- [ ] **Step 11: Dọn dẹp**

```bash
rm docs/nutrition-audit/audit-changes.sql
docker exec veggiepal-mysql mysql -uroot -p12345 -e "DROP DATABASE IF EXISTS veggiepal_recipe_audit; DROP DATABASE IF EXISTS veggiepal_recipe_fresh"
```

Đổi dòng "Trạng thái" của bảng đối chiếu thành `Đã duyệt và đưa vào data.sql (2026-10)`.

- [ ] **Step 12: Dừng, bàn giao cho Long review**

Báo cho Long:
- Danh sách file đã đổi.
- Kết quả Step 5–8: `BUILD SUCCESS` ba lần, `CONVERGED`, bảng khả thi.
- `recipes-seed.json` đã sẵn cho spec chọn món.
