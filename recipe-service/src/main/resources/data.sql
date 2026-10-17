-- ============================================================
-- VEGGIEPAL RECIPE SERVICE - SAMPLE DATA
-- ============================================================


-- ============================================================
-- 1. INGREDIENTS
-- ============================================================

INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Đậu hũ',
    'dau hu',
    'Đậu hũ làm từ đậu nành',
    76,
    8.1,
    1.9,
    4.8,
    0.3,
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'dau hu'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Gạo lứt',
    'gao lut',
    'Gạo lứt nguyên cám',
    370,
    7.9,
    77.2,
    2.9,
    3.5,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'gao lut'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Cà chua',
    'ca chua',
    'Cà chua tươi giàu vitamin',
    18,
    0.9,
    3.9,
    0.2,
    1.2,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'ca chua'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Bông cải xanh',
    'bong cai xanh',
    'Bông cải xanh giàu chất xơ',
    34,
    2.8,
    6.6,
    0.4,
    2.6,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'bong cai xanh'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Cà rốt',
    'ca rot',
    'Cà rốt tươi',
    41,
    0.9,
    9.6,
    0.2,
    2.8,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'ca rot'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Đậu gà',
    'dau ga',
    'Đậu gà giàu protein thực vật',
    164,
    8.9,
    27.4,
    2.6,
    7.6,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'dau ga'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Bơ',
    'bo',
    'Quả bơ giàu chất béo không bão hòa',
    160,
    2.0,
    8.5,
    14.7,
    6.7,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'bo'
);


INSERT INTO ingredients
(
    name,
    normalized_name,
    description,
    calories_per_100g,
    protein_per_100g,
    carbs_per_100g,
    fat_per_100g,
    fiber_per_100g,
    vegan,
    allergen,
    active,
    created_at,
    updated_at
)
SELECT
    'Rau chân vịt',
    'rau chan vit',
    'Rau chân vịt giàu vitamin và khoáng chất',
    23,
    2.9,
    3.6,
    0.4,
    2.2,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM ingredients
    WHERE normalized_name = 'rau chan vit'
);


-- ============================================================
-- 2. RECIPE 1
-- CƠM GẠO LỨT ĐẬU HŨ
-- ============================================================

INSERT INTO recipes
(
    user_id,
    title,
    description,
    image_url,
    servings,
    prep_time,
    cook_time,
    status,
    created_at,
    updated_at,
    published_at
)
SELECT
    1,
    'Cơm gạo lứt đậu hũ',
    'Món cơm gạo lứt kết hợp đậu hũ và rau củ, phù hợp cho bữa ăn chay lành mạnh.',
    'https://images.unsplash.com/photo-1547592180-85f173990554',
    2,
    15,
    30,
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM recipes
    WHERE title = 'Cơm gạo lứt đậu hũ'
);


-- Đậu hũ 200g
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    200,
    'GRAM',
    'Cắt thành miếng vuông'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'dau hu'
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Gạo lứt 150g
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    150,
    'GRAM',
    'Vo sạch trước khi nấu'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'gao lut'
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Cà chua 100g
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    100,
    'GRAM',
    'Rửa sạch và cắt lát'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'ca chua'
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Bông cải xanh 100g
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    100,
    'GRAM',
    'Cắt miếng vừa ăn'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Cà rốt 80g
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    80,
    'GRAM',
    'Cắt lát mỏng'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- ============================================================
-- RECIPE 1 STEPS
-- ============================================================

INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    1,
    'Vo sạch gạo lứt và nấu chín.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 1
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    2,
    'Cắt đậu hũ thành miếng và áp chảo vàng hai mặt.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 2
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    3,
    'Hấp bông cải xanh và cà rốt cho vừa chín.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 3
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    4,
    'Sơ chế và cắt cà chua thành lát.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 4
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    5,
    'Cho cơm, đậu hũ và rau củ ra đĩa rồi thưởng thức.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt đậu hũ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 5
);


-- ============================================================
-- 3. RECIPE 2
-- SALAD ĐẬU GÀ VÀ BƠ
-- ============================================================

INSERT INTO recipes
(
    user_id,
    title,
    description,
    image_url,
    servings,
    prep_time,
    cook_time,
    status,
    created_at,
    updated_at,
    published_at
)
SELECT
    1,
    'Salad đậu gà và bơ',
    'Salad chay giàu protein và chất xơ từ đậu gà, bơ và rau xanh.',
    'https://images.unsplash.com/photo-1512621776951-a57141f2eefd',
    2,
    15,
    5,
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM recipes
    WHERE title = 'Salad đậu gà và bơ'
);


-- Đậu gà
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    200,
    'GRAM',
    'Luộc chín'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'dau ga'
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Cà chua
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    100,
    'GRAM',
    'Cắt nhỏ'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'ca chua'
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Bơ
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    100,
    'GRAM',
    'Cắt thành khối nhỏ'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'bo'
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- Rau chân vịt
INSERT INTO recipe_ingredients
(
    recipe_id,
    ingredient_id,
    quantity,
    unit,
    note
)
SELECT
    r.id,
    i.id,
    80,
    'GRAM',
    'Rửa sạch và để ráo'
FROM recipes r
         JOIN ingredients i
              ON i.normalized_name = 'rau chan vit'
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id
      AND ri.ingredient_id = i.id
);


-- ============================================================
-- RECIPE 2 STEPS
-- ============================================================

INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    1,
    'Rửa sạch rau chân vịt và để ráo.'
FROM recipes r
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 1
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    2,
    'Cho đậu gà đã luộc vào tô.'
FROM recipes r
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 2
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    3,
    'Thêm cà chua và bơ đã cắt vào tô.'
FROM recipes r
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 3
);


INSERT INTO recipe_steps
(
    recipe_id,
    step_number,
    instruction
)
SELECT
    r.id,
    4,
    'Thêm rau chân vịt và trộn đều tất cả nguyên liệu.'
FROM recipes r
WHERE r.title = 'Salad đậu gà và bơ'
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_steps rs
    WHERE rs.recipe_id = r.id
      AND rs.step_number = 4
);
-- ============================================================
-- 4. ADDITIONAL INGREDIENTS (PHASE 2 - 100 VEGAN RECIPES)
-- ============================================================

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Gạo thơm', 'gao thom', 'Gạo thơm hạt dài dẻo ngọt', 130, 2.7, 28.2, 0.3, 0.4, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'gao thom');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bún tươi', 'bun tuoi', 'Bún sợi tươi làm từ gạo', 110, 1.7, 25.7, 0.2, 0.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'bun tuoi');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bánh phở', 'banh pho', 'Bánh phở tươi mềm dai', 123, 2.2, 27.5, 0.3, 0.4, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'banh pho');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bánh mì lúa mì', 'banh mi', 'Bánh mì Việt Nam vỏ giòn ruột mềm', 265, 9, 49, 3.2, 2.7, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'banh mi');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Hủ tiếu', 'hu tieu', 'Sợi hủ tiếu dai ngon', 135, 1.5, 31, 0.2, 0.3, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'hu tieu');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Miến dong', 'mien dong', 'Miến dong nguyên chất dai giòn', 140, 0.7, 34, 0.1, 0.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'mien dong');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Mì sợi lúa mì', 'mi soi', 'Mì sợi chay làm từ bột mì', 138, 4.5, 28, 1.1, 1.2, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'mi soi');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nui gạo', 'nui', 'Nui gạo luộc mềm', 131, 2.5, 29, 0.3, 0.6, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nui');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Gạo nếp', 'gao nep', 'Gạo nếp cái hoa vàng dẻo thơm', 140, 3, 31.5, 0.3, 0.8, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'gao nep');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Yến mạch', 'yen mach', 'Yến mạch nguyên cám giàu xơ', 389, 16.9, 66.3, 6.9, 10.6, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'yen mach');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu hũ non', 'dau hu non', 'Đậu hũ non mịn mềm từ đậu nành', 55, 5.5, 1.5, 3, 0.2, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau hu non');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Tàu hũ ky đậu nành', 'tau hu ky', 'Váng đậu nành chiên giòn hoặc nấu canh', 400, 45, 15, 20, 2, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'tau hu ky');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu lăng đỏ', 'dau lang do', 'Đậu lăng đỏ giàu đạm thực vật', 116, 9, 20.1, 0.4, 7.9, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau lang do');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu xanh', 'dau xanh', 'Đậu xanh cà vỏ thanh nhiệt', 105, 7, 19, 0.4, 7.6, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau xanh');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu đỏ', 'dau do', 'Đậu đỏ bùi thơm', 128, 7.5, 22.8, 0.5, 7.3, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau do');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu phộng rang', 'dau phong', 'Lạc đậu phộng rang thơm', 567, 25.8, 16.1, 49.2, 8.5, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau phong');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu que', 'dau que', 'Đậu cô ve giòn ngọt', 31, 1.8, 7, 0.2, 2.7, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau que');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Đậu Hà Lan', 'dau ha lan', 'Đậu Hà Lan ngọt tươi', 81, 5.4, 14.5, 0.4, 5.7, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau ha lan');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Hạt điều', 'hat dieu', 'Hạt điều bùi béo giàu dinh dưỡng', 553, 18.2, 30.2, 43.8, 3.3, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'hat dieu');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Hạt sen tươi', 'hat sen', 'Hạt sen tươi thanh mát bổ dưỡng', 89, 4.1, 17.3, 0.5, 2.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'hat sen');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nấm rơm', 'nam rom', 'Nấm rơm tươi ngọt nước', 31, 3.8, 3.2, 0.7, 2.5, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nam rom');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nấm đùi gà', 'nam dui ga', 'Nấm đùi gà dày thịt dai ngon', 35, 3.3, 5, 0.8, 2.3, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nam dui ga');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nấm đông cô', 'nam dong co', 'Nấm đông cô thơm đậm đà', 34, 2.2, 6.8, 0.5, 2.5, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nam dong co');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nấm bào ngư', 'nam bao ngu', 'Nấm bào ngư xám mềm ngọt', 33, 3.3, 6.1, 0.4, 2.3, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nam bao ngu');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nấm kim châm', 'nam kim cham', 'Nấm kim châm giòn ngọt', 37, 2.7, 7.8, 0.3, 2.7, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nam kim cham');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nấm mèo mộc nhĩ', 'nam meo', 'Mộc nhĩ nấm mèo giòn giòn', 25, 0.5, 7, 0.1, 5, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nam meo');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Cà tím', 'ca tim', 'Cà tím mềm ngọt', 25, 1, 5.9, 0.2, 3, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'ca tim');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bắp ngô ngọt', 'bap ngot', 'Bắp ngọt tươi', 86, 3.2, 19, 1.2, 2.7, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'bap ngot');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bắp cải trắng', 'bap cai', 'Bắp cải tươi giòn', 25, 1.3, 5.8, 0.1, 2.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'bap cai');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bắp cải tím', 'bap cai tim', 'Bắp cải tím giàu chất chống oxy hóa', 31, 1.4, 7.4, 0.2, 2.1, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'bap cai tim');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bí đỏ', 'bi do', 'Bí đỏ ngọt bùi', 26, 1, 6.5, 0.1, 0.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'bi do');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Khoai tây', 'khoai tay', 'Khoai tây củ bùi béo', 77, 2, 17.5, 0.1, 2.2, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'khoai tay');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Khoai lang', 'khoai lang', 'Khoai lang ngọt dẻo', 86, 1.6, 20.1, 0.1, 3, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'khoai lang');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Củ cải trắng', 'cu cai trang', 'Củ cải trắng thanh mát', 18, 0.7, 4.1, 0.1, 1.6, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'cu cai trang');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Củ sen', 'cu sen', 'Củ sen giòn ngọt bổ máu', 74, 2.6, 17.2, 0.1, 4.9, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'cu sen');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Khổ qua', 'kho qua', 'Khổ qua mướp đắng mát gan', 17, 1, 3.7, 0.2, 2.8, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'kho qua');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Mướp hương', 'muop huong', 'Mướp hương thơm mát', 16, 0.9, 3.5, 0.1, 1.1, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'muop huong');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Rau muống', 'rau muong', 'Rau muống giòn ngọt', 19, 3, 2.1, 0.2, 1, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'rau muong');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Rau ngót', 'rau ngot', 'Rau ngót mát lành', 35, 5.3, 3.4, 0.5, 2.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'rau ngot');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Rau mồng tơi', 'rau mong toi', 'Rau mồng tơi tươi mát', 14, 1.4, 2.5, 0.2, 1.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'rau mong toi');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Cải thìa', 'cai thia', 'Cải thìa giòn ngọt', 13, 1.5, 2.2, 0.2, 1, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'cai thia');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Cải bẹ xanh', 'cai be xanh', 'Cải bẹ xanh cay nồng ấm bụng', 27, 2.7, 4.7, 0.4, 3.2, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'cai be xanh');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Dọc mùng bạc hà', 'doc mung', 'Dọc mùng tươi giòn cho canh chua', 14, 0.5, 3, 0.1, 1.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'doc mung');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Quả thơm dứa', 'thom', 'Thơm dứa chua ngọt đậm đà', 50, 0.5, 13.1, 0.1, 1.4, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'thom');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Bắp chuối', 'bap chuoi', 'Bắp chuối bào giòn thơm', 23, 1.5, 4, 0.3, 2.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'bap chuoi');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Ngó sen', 'ngo sen', 'Ngó sen giòn ngọt làm gỏi', 30, 1.2, 6, 0.1, 2, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'ngo sen');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Củ hũ dừa', 'cu hu dua', 'Củ hũ dừa trắng giòn', 36, 1.8, 6.5, 0.4, 2.3, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'cu hu dua');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Mít non', 'mit non', 'Mít non kho hoặc xào', 40, 1.5, 8.5, 0.2, 3.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'mit non');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Dưa leo', 'dua leo', 'Dưa leo tươi giòn', 15, 0.7, 3.6, 0.1, 0.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dua leo');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Ớt chuông', 'ot chuong', 'Ớt chuông đỏ vàng giòn ngọt', 26, 1, 6, 0.3, 2.1, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'ot chuong');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Cần tây', 'can tay', 'Cần tây thơm giòn', 16, 0.7, 3, 0.2, 1.6, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'can tay');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Rong biển', 'rong bien', 'Rong biển nấu canh bổ dưỡng', 45, 3, 9.1, 0.6, 0.5, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'rong bien');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Hành boa-rô', 'hanh paro', 'Hành poa-rô thơm dịu cho món chay', 32, 1.5, 7, 0.3, 1.8, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'hanh paro');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Hành tây', 'hanh tay', 'Hành tây củ ngọt dịu', 40, 1.1, 9.3, 0.1, 1.7, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'hanh tay');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Gừng tươi', 'gung', 'Gừng già cay ấm', 80, 1.8, 17.8, 0.8, 2, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'gung');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Sả cây', 'sa cay', 'Sả cây băm nhuyễn thơm nồng', 99, 1.8, 25, 0.5, 3, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'sa cay');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Ớt tươi', 'ot', 'Ớt tươi cay nồng', 40, 1.9, 8.8, 0.4, 1.5, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'ot');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Hạt mè vừng', 'me', 'Mè trắng rang thơm', 573, 17.7, 23.4, 49.7, 11.8, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'me');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nước cốt dừa', 'nuoc cot dua', 'Nước cốt dừa béo ngậy nguyên chất', 197, 2, 2.8, 21.3, 0, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nuoc cot dua');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Dầu thực vật', 'dau an', 'Dầu đậu nành hoặc dầu hạt cải', 884, 0, 0, 100, 0, TRUE, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'dau an');

INSERT INTO ingredients (name, normalized_name, description, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, vegan, allergen, active, created_at, updated_at)
SELECT 'Nước tương đậu nành', 'nuoc tuong', 'Nước tương lên men tự nhiên', 53, 5.7, 4.9, 0.6, 0.8, TRUE, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE normalized_name = 'nuoc tuong');


-- ============================================================
-- 5. 100 VIETNAMESE VEGAN RECIPES (PHASE 2)
-- ============================================================

-- ------------------------------------------------------------
-- RECIPE 3: Phở chay nấm đông cô
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Phở chay nấm đông cô', 'Món phở truyền thống với nước dùng thanh ngọt từ củ cải, nấm đông cô và đậu hũ.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Phở chay nấm đông cô');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Chần nước sôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'banh pho'
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Áp chảo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Hầm nước dùng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cu cai trang'
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nước dùng từ củ cải và cuống nấm cho ngọt nước.'
FROM recipes r
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Áp chảo đậu hũ và xào thơm nấm đông cô.'
FROM recipes r
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trụng bánh phở cho vào tô, xếp nấm, đậu hũ lên trên rồi chan nước dùng nóng.'
FROM recipes r
WHERE r.title = 'Phở chay nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 4: Bún bò Huế chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún bò Huế chay', 'Bún bò Huế chay đậm vị sả ớt, nấm bào ngư và đậu hũ chiên vàng giòn.', NULL, 2, 20, 35, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún bò Huế chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Trụng sơ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xé sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt vuông chiên'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'sa cay'
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Phi thơm sả băm với chút ớt bột tạo màu cay thơm.'
FROM recipes r
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào nấm bào ngư và đậu hũ với gia vị cho thấm.'
FROM recipes r
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nấu nước dùng sả, chan ngập tô bún và dùng nóng kèm rau sống.'
FROM recipes r
WHERE r.title = 'Bún bò Huế chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 5: Cháo nấm hạt sen
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cháo nấm hạt sen', 'Cháo gạo thơm sánh mịn nấu cùng hạt sen tươi bùi béo và nấm rơm ngọt tự nhiên.', NULL, 2, 10, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cháo nấm hạt sen');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Vo sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Bỏ tâm sen'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nhừ gạo thơm cùng hạt sen tươi trên lửa nhỏ.'
FROM recipes r
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào thơm nấm rơm và cà rốt với chút dầu ăn và gia vị.'
FROM recipes r
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho nấm vào nồi cháo, khuấy đều nấu thêm 5 phút rồi tắt bếp.'
FROM recipes r
WHERE r.title = 'Cháo nấm hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 6: Bánh mì bì chay thính gạo
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bánh mì bì chay thính gạo', 'Bánh mì giòn rụm kẹp bì chay làm từ đậu hũ sợi và miến trộn thính gạo thơm lừng.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bánh mì bì chay thính gạo');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 2, 'QUA', 'Bánh mì nướng giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'banh mi'
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Cắt sợi chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Trụng mềm cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mien dong'
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Chiên vàng sợi đậu hũ rồi trộn đều với miến dong và thính gạo rang.'
FROM recipes r
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xẻ dọc ổ bánh mì, quết nước tương và tương ớt.'
FROM recipes r
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Kẹp bì chay, dưa leo cùng đồ chua vào bánh mì và thưởng thức.'
FROM recipes r
WHERE r.title = 'Bánh mì bì chay thính gạo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 7: Bánh mì pate nấm đậu gà
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bánh mì pate nấm đậu gà', 'Bánh mì kẹp pate chay béo ngậy tự làm từ nấm đùi gà và đậu gà hấp chín.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bánh mì pate nấm đậu gà');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 2, 'QUA', 'Nướng giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'banh mi'
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Đã hấp mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ga'
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bo'
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào thơm nấm đùi gà với hành boa-rô cho chín mềm.'
FROM recipes r
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xay nhuyễn nấm cùng đậu gà chín và chút bơ thành hỗn hợp pate mịn.'
FROM recipes r
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Quết pate vào bánh mì, thêm lát bơ tươi và thưởng thức.'
FROM recipes r
WHERE r.title = 'Bánh mì pate nấm đậu gà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 8: Hủ tiếu Nam Vang chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Hủ tiếu Nam Vang chay', 'Hủ tiếu nước ngọt thanh với nấm đùi gà, tàu hũ ky và cải cúc.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Hủ tiếu Nam Vang chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 200, 'GRAM', 'Trụng chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hu tieu'
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Chiên giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'tau hu ky'
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Rửa sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Hầm củ cải và cà rốt lấy nước dùng ngọt thơm.'
FROM recipes r
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trụng sợi hủ tiếu cho vào tô, xếp nấm xào và tàu hũ ky chiên giòn.'
FROM recipes r
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Chan nước dùng sôi sùng sục vào tô, rắc tiêu thơm.'
FROM recipes r
WHERE r.title = 'Hủ tiếu Nam Vang chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 9: Miến dong xào rau củ chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Miến dong xào rau củ chay', 'Miến dong dai giòn xào cùng nấm bào ngư, cà rốt và bông cải xanh tươi non.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Miến dong xào rau củ chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Ngâm mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mien dong'
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xé sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào chín nấm bào ngư, cà rốt và bông cải xanh với chút dầu ăn.'
FROM recipes r
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho miến dong vào đảo đều tay trên lửa lớn với nước tương.'
FROM recipes r
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Tắt bếp khi sợi miến trong và thấm đều vị.'
FROM recipes r
WHERE r.title = 'Miến dong xào rau củ chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 10: Xôi gấc hạt sen đậu xanh
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Xôi gấc hạt sen đậu xanh', 'Món xôi đỏ tươi dẻo thơm nấu từ gạo nếp, hạt sen và đậu xanh bùi ngậy.', NULL, 2, 20, 40, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Xôi gấc hạt sen đậu xanh');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Ngâm nở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao nep'
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Hấp chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Nấu nhừ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau xanh'
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'ML', 'Rưới lên xôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nuoc cot dua'
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn gạo nếp với hạt sen và đồ xôi trong xửng hấp 30 phút.'
FROM recipes r
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đậu xanh nấu tán nhuyễn nắm thành viên hoặc rắc mặt xôi.'
FROM recipes r
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Rưới chút nước cốt dừa lên xôi đang nóng và xới đều.'
FROM recipes r
WHERE r.title = 'Xôi gấc hạt sen đậu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 11: Bún riêu cua chay đậu hũ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún riêu cua chay đậu hũ', 'Bún riêu chay thơm nồng vị cà chua, riêu làm từ đậu hũ non và nấm rơm tán nhuyễn.', NULL, 2, 20, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún riêu cua chay đậu hũ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Trụng nước sôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Tán nhuyễn làm riêu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt múi cau'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào cà chua chín mềm tạo màu đỏ tự nhiên cho nước dùng.'
FROM recipes r
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đậu hũ non xào cùng nấm rơm nêm gia vị để nổi váng riêu thơm.'
FROM recipes r
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trụng bún vào tô, múc riêu và chan nước dùng chua dịu.'
FROM recipes r
WHERE r.title = 'Bún riêu cua chay đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 12: Bún mọc nấm rơm chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún mọc nấm rơm chay', 'Tô bún mọc thanh đạm với viên mọc chay từ nấm và đậu hũ thơm mùi tiêu sọ.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún mọc nấm rơm chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Trụng mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Băm nhỏ làm mọc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Quết mịn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn đậu hũ tán nhuyễn với nấm mèo, nấm rơm và tiêu rồi vo viên mọc.'
FROM recipes r
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả viên mọc vào nồi nước hầm rau củ đun sôi cho mọc chín nổi lên.'
FROM recipes r
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xếp bún vào tô và chan nước dùng kèm viên mọc thanh ngọt.'
FROM recipes r
WHERE r.title = 'Bún mọc nấm rơm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 13: Cháo yến mạch bí đỏ đậu đỏ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cháo yến mạch bí đỏ đậu đỏ', 'Bữa sáng ấm bụng giàu chất xơ và chất chống oxy hóa từ yến mạch và đậu đỏ.', NULL, 2, 10, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cháo yến mạch bí đỏ đậu đỏ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Yến mạch cán'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'yen mach'
WHERE r.title = 'Cháo yến mạch bí đỏ đậu đỏ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bi do'
WHERE r.title = 'Cháo yến mạch bí đỏ đậu đỏ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Đã ninh mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau do'
WHERE r.title = 'Cháo yến mạch bí đỏ đậu đỏ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu bí đỏ với nước cho mềm nhừ rồi dùng muỗng dầm mịn.'
FROM recipes r
WHERE r.title = 'Cháo yến mạch bí đỏ đậu đỏ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho yến mạch và đậu đỏ đã ninh mềm vào khuấy đều trên lửa nhỏ 5 phút.'
FROM recipes r
WHERE r.title = 'Cháo yến mạch bí đỏ đậu đỏ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm chút muối hồng và thưởng thức cháo sánh mịn.'
FROM recipes r
WHERE r.title = 'Cháo yến mạch bí đỏ đậu đỏ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 14: Bún thang chay nấm bào ngư
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún thang chay nấm bào ngư', 'Bát bún thang cầu kỳ trang nhã với các sợi nấm bào ngư, đậu hũ chiên và cà rốt.', NULL, 2, 20, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún thang chay nấm bào ngư');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Trụng ráo nước'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 70, 'GRAM', 'Xé sợi xào thơm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt sợi chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nước dùng từ củ cải trắng và nấm hương cho trong vắt.'
FROM recipes r
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trình bày sợi bún, xếp từng góc nấm xé, đậu hũ chiên sợi và cà rốt.'
FROM recipes r
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Chan nước dùng nóng hổi và thưởng thức hương vị thanh tao.'
FROM recipes r
WHERE r.title = 'Bún thang chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 15: Bánh bao chay nhân nấm mộc nhĩ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bánh bao chay nhân nấm mộc nhĩ', 'Bánh bao chay mềm xốp nhân nấm đùi gà, mộc nhĩ và hạt sen bùi béo.', NULL, 2, 30, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bánh bao chay nhân nấm mộc nhĩ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Ngâm nở băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Hấp chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Xắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào thơm nhân nấm đùi gà, mộc nhĩ, cà rốt và hạt sen với dầu hào chay.'
FROM recipes r
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Gói nhân vào vỏ bánh bao bột mì và tạo hình múi hoa.'
FROM recipes r
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Hấp bánh chín trong xửng hơi nước 15-20 phút cho nở xốp.'
FROM recipes r
WHERE r.title = 'Bánh bao chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 16: Súp bắp non hạt sen nấm tuyết
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Súp bắp non hạt sen nấm tuyết', 'Món súp khai vị buổi sáng ngọt ngào, giàu vitamin từ bắp ngọt và hạt sen.', NULL, 2, 10, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Súp bắp non hạt sen nấm tuyết');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Tách hạt'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap ngot'
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Hấp mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Thái sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu bắp ngọt và hạt sen trong nước hầm rau củ cho chín mềm.'
FROM recipes r
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm nấm đông cô thái sợi và cà rốt hạt lựu vào nấu sôi.'
FROM recipes r
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Hòa bột năng khuấy đều tạo độ sánh sệt vừa vặn cho súp.'
FROM recipes r
WHERE r.title = 'Súp bắp non hạt sen nấm tuyết'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 17: Cháo bí đỏ đậu xanh hạt sen
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cháo bí đỏ đậu xanh hạt sen', 'Cháo bổ dưỡng giúp an thần, ngủ ngon với bí đỏ dẻo ngọt và đậu xanh bùi thơm.', NULL, 2, 10, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cháo bí đỏ đậu xanh hạt sen');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Nấu cháo nhừ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt miếng nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bi do'
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cà vỏ nấu mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau xanh'
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bỏ tâm sen'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cho gạo, bí đỏ, đậu xanh và hạt sen vào nồi ninh lửa nhỏ.'
FROM recipes r
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Khi cháo chín nhừ, khuấy nhẹ cho bí đỏ hòa quyện sánh vàng.'
FROM recipes r
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Múc ra bát thưởng thức khi còn nóng ấm.'
FROM recipes r
WHERE r.title = 'Cháo bí đỏ đậu xanh hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 18: Bánh cuốn chay nhân nấm mộc nhĩ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bánh cuốn chay nhân nấm mộc nhĩ', 'Bánh cuốn mỏng mềm cuộn nhân nấm rơm và mộc nhĩ ăn kèm nước mắm chay chua ngọt.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bánh cuốn chay nhân nấm mộc nhĩ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 70, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt sợi chiên giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Thái lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào chín nhân nấm rơm và mộc nhĩ nêm hạt nêm chay và tiêu.'
FROM recipes r
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Tráng bánh hoặc dùng bánh ướt cuộn đều phần nhân nấm thơm lừng.'
FROM recipes r
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cắt miếng vừa ăn, rắc hành phi chay và dùng cùng nước chấm pha chua ngọt.'
FROM recipes r
WHERE r.title = 'Bánh cuốn chay nhân nấm mộc nhĩ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 19: Miến măng khô nấm đùi gà chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Miến măng khô nấm đùi gà chay', 'Món miến nước ngọt ngào với măng khô nấu mềm và nấm đùi gà giòn dai.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Miến măng khô nấm đùi gà chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Ngâm nở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mien dong'
WHERE r.title = 'Miến măng khô nấm đùi gà chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xắt lát xéo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Miến măng khô nấm đùi gà chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Miến măng khô nấm đùi gà chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào thơm măng khô đã luộc kĩ với nấm đùi gà và gia vị.'
FROM recipes r
WHERE r.title = 'Miến măng khô nấm đùi gà chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nấu nước dùng thanh ngọt rồi trút măng nấm vào ninh sôi.'
FROM recipes r
WHERE r.title = 'Miến măng khô nấm đùi gà chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho miến ra tô, chan nước dùng và rắc rau răm, tiêu sọ.'
FROM recipes r
WHERE r.title = 'Miến măng khô nấm đùi gà chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 20: Bánh xèo chay nấm giá đỗ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bánh xèo chay nấm giá đỗ', 'Bánh xèo giòn rụm với nhân nấm bào ngư, đậu xanh và giá đỗ tươi giòn.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bánh xèo chay nấm giá đỗ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xé sợi xào chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Đã hấp chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau xanh'
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'ML', 'Pha vào bột'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nuoc cot dua'
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Pha bột bánh xèo với bột nghệ và chút nước cốt dừa cho thơm béo.'
FROM recipes r
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đổ bột vào chảo nóng, xếp nhân nấm, đậu xanh và giá đỗ rồi đậy nắp.'
FROM recipes r
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Khi bánh giòn vàng, gấp đôi lại và dùng kèm xà lách rau thơm.'
FROM recipes r
WHERE r.title = 'Bánh xèo chay nấm giá đỗ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 21: Nui xào rau củ sốt tương
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nui xào rau củ sốt tương', 'Món ăn sáng nhanh gọn giàu năng lượng từ nui gạo, cà rốt và cải thìa xào giòn.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nui xào rau củ sốt tương');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Luộc chín tới'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nui'
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt tỉa hoa'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Áp chảo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Luộc nui vừa chín tới rồi xả nước lạnh cho sợi nui săn lại.'
FROM recipes r
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào thơm cà rốt, cải thìa và đậu hũ trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trút nui vào đảo đều cùng nước tương hảo hạng và chút dầu mè.'
FROM recipes r
WHERE r.title = 'Nui xào rau củ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 22: Bún chả giò bắp chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún chả giò bắp chay', 'Bún tươi ăn kèm chả giò bắp vàng ươm, giòn rụm và rau sống thơm ngát.', NULL, 2, 20, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún chả giò bắp chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Bún tươi sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Bào hạt làm nhân'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap ngot'
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Nấu chín tán nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau xanh'
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn bắp bào với đậu xanh, nấm mèo và gia vị cuốn bánh tráng.'
FROM recipes r
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Chiên chả giò ngập dầu cho vỏ bánh vàng giòn rụm.'
FROM recipes r
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xếp bún vào tô cùng chả giò cắt khúc, chan nước mắm chua ngọt chay.'
FROM recipes r
WHERE r.title = 'Bún chả giò bắp chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 23: Cơm tấm sườn nấm chay nướng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm tấm sườn nấm chay nướng', 'Cơm tấm thơm dẻo ăn kèm sườn nấm đùi gà nướng sốt ngũ vị và chả trứng chay.', NULL, 2, 20, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm tấm sườn nấm chay nướng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Nấu dẻo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Ướp sốt nướng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Làm chả chay'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Ướp nấm đùi gà với sả băm, ngũ vị hương và mật mía rồi nướng vàng.'
FROM recipes r
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nướng hoặc hấp chả đậu hũ làm chả cơm tấm.'
FROM recipes r
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xới cơm tấm ra đĩa, xếp sườn nấm, chả chay và dưa leo ăn kèm.'
FROM recipes r
WHERE r.title = 'Cơm tấm sườn nấm chay nướng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 24: Cơm chiên Dương Châu chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm chiên Dương Châu chay', 'Cơm chiên hạt tơi xốp kết hợp màu sắc từ cà rốt, đậu Hà Lan, bắp ngọt và đậu hũ.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm chiên Dương Châu chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Cơm để nguội'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Luộc sơ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ha lan'
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Hạt bắp luộc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap ngot'
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt hạt lựu chiên'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào săn cà rốt, đậu Hà Lan, bắp ngọt và đậu hũ chiên.'
FROM recipes r
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đổ cơm nguội vào đảo đều tay trên lửa lớn cho hạt cơm săn giòn.'
FROM recipes r
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm gia vị chay vừa miệng, rắc hành boa-rô phi thơm lên đĩa cơm.'
FROM recipes r
WHERE r.title = 'Cơm chiên Dương Châu chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 25: Cơm hấp lá sen hạt sen chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm hấp lá sen hạt sen chay', 'Món cơm thanh tao gói trong lá sen thơm ngát với hạt sen bùi và nấm đông cô.', NULL, 2, 20, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm hấp lá sen hạt sen chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Gạo thơm dẻo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Hấp chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Xắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào sơ hạt sen, nấm đông cô và cà rốt với chút dầu mè.'
FROM recipes r
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trộn đều phần nhân xào với cơm trắng rồi bọc gọn trong lá sen tươi.'
FROM recipes r
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Hấp gói sen trong xửng 15 phút cho hương sen thấm đượm từng hạt cơm.'
FROM recipes r
WHERE r.title = 'Cơm hấp lá sen hạt sen chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 26: Mì Quảng chay nấm đậu hũ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Mì Quảng chay nấm đậu hũ', 'Sợi mì Quảng mềm dai vàng óng ngập tràn nhân nấm bào ngư, đậu hũ và bánh tráng mè nướng.', NULL, 2, 20, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Mì Quảng chay nấm đậu hũ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 200, 'GRAM', 'Trụng chín mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mi soi'
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xào thấm vị'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang giã dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào nấm bào ngư và đậu hũ với nghệ tươi và dầu củ nén cho dậy mùi.'
FROM recipes r
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nấu nước nhưn mì Quảng đậm đà ngọt từ nước dừa tươi.'
FROM recipes r
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Bày mì Quảng ra tô, chan nước nhưn xâm xấp, rắc đậu phộng rang giòn.'
FROM recipes r
WHERE r.title = 'Mì Quảng chay nấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 27: Bún chả chay Hà Nội
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún chả chay Hà Nội', 'Bún tươi ăn cùng chả nấm nướng thơm lừng trong chén nước chấm chua ngọt có đu đủ.', NULL, 2, 25, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún chả chay Hà Nội');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Bún sợi nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Băm nhỏ làm chả'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Tán nhuyễn nặn viên'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Làm đồ chua'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn nấm đùi gà băm, đậu hũ tán nhuyễn với tiêu sọ, nặn thành viên chả dẹt.'
FROM recipes r
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nướng chả trên than hoa hoặc nồi chiên không dầu cho vàng xém cạnh.'
FROM recipes r
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Pha nước mắm chay chua ngọt, thả chả nướng và cà rốt ngâm giấm vào chén.'
FROM recipes r
WHERE r.title = 'Bún chả chay Hà Nội'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 28: Nui sốt cà chua đậu hũ non
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nui sốt cà chua đậu hũ non', 'Nui xào mềm sốt đẫm cà chua tươi đỏ au và đậu hũ non mịn màng béo ngậy.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nui sốt cà chua đậu hũ non');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Luộc chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nui'
WHERE r.title = 'Nui sốt cà chua đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Băm nhỏ làm sốt'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Nui sốt cà chua đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt khối vuông nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Nui sốt cà chua đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào nhuyễn cà chua tươi với dầu thực vật tạo thành sốt sền sệt.'
FROM recipes r
WHERE r.title = 'Nui sốt cà chua đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho đậu hũ non và nui đã luộc vào đảo nhẹ tay kẻo nát đậu.'
FROM recipes r
WHERE r.title = 'Nui sốt cà chua đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm gia vị vừa ăn, rắc chút tiêu đen lên đĩa nui thơm lừng.'
FROM recipes r
WHERE r.title = 'Nui sốt cà chua đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 29: Cơm chiên rong biển mè rang
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm chiên rong biển mè rang', 'Cơm chiên giòn rụm phủ rong biển thơm giòn và mè trắng rang vàng béo bùi.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm chiên rong biển mè rang');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Cơm để ráo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Cắt sợi giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rong bien'
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'GRAM', 'Rang thơm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'me'
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Chiên cơm trên chảo nóng với chút cà rốt hạt lựu cho hạt cơm săn ráo.'
FROM recipes r
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm rong biển cắt sợi và nước tương nêm nếm vừa miệng.'
FROM recipes r
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Rắc mè rang vàng lên mặt cơm trước khi dọn ra bàn ăn.'
FROM recipes r
WHERE r.title = 'Cơm chiên rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 30: Bún đậu chả cốm chay mắm tương
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún đậu chả cốm chay mắm tương', 'Mẹt bún đậu thanh tịnh gồm bún lá, đậu hũ chiên giòn, chả cốm chay và tương bần pha tắc.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún đậu chả cốm chay mắm tương');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Bún lá cắt miếng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún đậu chả cốm chay mắm tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Chiên giòn phồng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún đậu chả cốm chay mắm tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Thái lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Bún đậu chả cốm chay mắm tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cắt đậu hũ thành khối vuông và chiên ngập dầu cho vỏ vàng ruộm.'
FROM recipes r
WHERE r.title = 'Bún đậu chả cốm chay mắm tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Pha nước tương bần với đường, ớt tươi và nước cốt tắc đánh sủi bọt.'
FROM recipes r
WHERE r.title = 'Bún đậu chả cốm chay mắm tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Bày bún lá, đậu chiên và dưa leo lên mẹt lá chuối rồi thưởng thức.'
FROM recipes r
WHERE r.title = 'Bún đậu chả cốm chay mắm tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 31: Miến xào nấm đùi gà cà rốt
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Miến xào nấm đùi gà cà rốt', 'Miến dong dai mềm xào chín tới cùng nấm đùi gà ngọt giòn và cà rốt bào sợi.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Miến xào nấm đùi gà cà rốt');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Ngâm nở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mien dong'
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Thái sợi mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Phi thơm hành boa-rô, xào nấm đùi gà và cà rốt cho chín tới.'
FROM recipes r
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho miến dong vào đảo đều với sốt xào dầu hào chay trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm cải thìa đảo nhanh tay 1 phút rồi tắt bếp dọn ra đĩa.'
FROM recipes r
WHERE r.title = 'Miến xào nấm đùi gà cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 32: Cơm niêu rau củ luộc kho quẹt chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm niêu rau củ luộc kho quẹt chay', 'Cơm niêu đáy cháy giòn chấm ngập nồi kho quẹt đậm đà từ nấm và đậu hũ.', NULL, 2, 20, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm niêu rau củ luộc kho quẹt chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Nấu cơm niêu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Xắt hạt lựu kho'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chiên giòn cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Luộc chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Luộc chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu cơm niêu trên lửa nhỏ để tạo lớp cháy đáy giòn rụm.'
FROM recipes r
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Kho nấm rơm băm, đậu hũ chiên cùng nước tương, tiêu sọ cho sánh sệt đặc quánh.'
FROM recipes r
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Luộc chín bông cải xanh và cà rốt chấm cùng kho quẹt thơm nồng.'
FROM recipes r
WHERE r.title = 'Cơm niêu rau củ luộc kho quẹt chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 33: Mì xào giòn rau củ thập cẩm chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Mì xào giòn rau củ thập cẩm chay', 'Vắt mì chiên giòn rụm chan nước sốt rau củ nấm sệt ngọt ngào nóng hổi.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Mì xào giòn rau củ thập cẩm chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Chiên giòn phồng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mi soi'
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Thái lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Tỉa hoa'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Chiên vắt mì sợi ngập dầu cho nở giòn rụm rồi vớt ra để ráo dầu.'
FROM recipes r
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào nấm đông cô, bông cải xanh và cà rốt rồi thêm nước dùng nêm vừa vị.'
FROM recipes r
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Hòa chút bột năng tạo độ sánh cho nước sốt rồi rưới ngập lên vắt mì giòn.'
FROM recipes r
WHERE r.title = 'Mì xào giòn rau củ thập cẩm chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 34: Bún mắm chay nấm rơm cà tím
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún mắm chay nấm rơm cà tím', 'Món bún mắm miền Tây thơm lừng mùi sả, nấm rơm và cà tím mềm ngọt đậm đà.', NULL, 2, 20, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún mắm chay nấm rơm cà tím');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Trụng nước sôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khúc vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca tim'
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Phi thơm sả băm rồi xào cà tím, nấm rơm và đậu hũ cho thấm vị tương.'
FROM recipes r
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nấu nước dùng từ mắm đậu chay ninh cùng thơm dứa và củ cải.'
FROM recipes r
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xếp bún vào tô, múc ngập nấm, cà tím rồi chan nước dùng nóng hổi.'
FROM recipes r
WHERE r.title = 'Bún mắm chay nấm rơm cà tím'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 35: Cơm chiên trái thơm kiểu Thái chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm chiên trái thơm kiểu Thái chay', 'Cơm chiên dứa thơm lừng với màu vàng nghệ óng ả, hạt điều bùi béo và rau củ.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm chiên trái thơm kiểu Thái chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Cơm nấu chín nguội'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Khoét ruột cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'thom'
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang vàng giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat dieu'
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Luộc sơ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ha lan'
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Khoét ruột quả thơm lấy phần thịt cắt hạt lựu ráo nước.'
FROM recipes r
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Chiên cơm với bột nghệ, đậu Hà Lan và thịt thơm trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trộn hạt điều rang giòn vào cơm và múc vào vỏ quả thơm bày biện đẹp mắt.'
FROM recipes r
WHERE r.title = 'Cơm chiên trái thơm kiểu Thái chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 36: Cơm gạo lứt cuộn rong biển kimbap chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm gạo lứt cuộn rong biển kimbap chay', 'Cuộn kimbap đầy màu sắc từ cơm gạo lứt, dưa leo, cà rốt và đậu hũ áp chảo.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm gạo lứt cuộn rong biển kimbap chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Nấu chín dẻo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao lut'
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 10, 'GRAM', 'Lá rong biển cuộn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rong bien'
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt thanh dài'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Luộc thanh dài'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt thanh dài'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn cơm gạo lứt ấm với chút dầu mè và muối hạt.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trải lá rong biển lên mành tre, dàn đều cơm và xếp thanh đậu hũ, cà rốt, dưa leo.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cuộn chặt tay, cắt khoanh vừa ăn và rắc mè rang lên mặt cuộn kimbap.'
FROM recipes r
WHERE r.title = 'Cơm gạo lứt cuộn rong biển kimbap chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 37: Hủ tiếu xào mềm rau cải nấm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Hủ tiếu xào mềm rau cải nấm', 'Hủ tiếu dai mềm xào chín tới cùng nấm đông cô đậm vị và cải thìa tươi ngọt.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Hủ tiếu xào mềm rau cải nấm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Áp chảo mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hu tieu'
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Áp chảo sợi hủ tiếu với chút hắc xì dầu cho thơm và có màu nâu đẹp.'
FROM recipes r
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào chín nấm đông cô, cải thìa và đậu hũ với gia vị sốt chay.'
FROM recipes r
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trút hủ tiếu vào đảo nhanh tay trên lửa lớn cho hòa quyện rồi tắt bếp.'
FROM recipes r
WHERE r.title = 'Hủ tiếu xào mềm rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 38: Bún gạo lứt xào cải thìa đậu hũ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún gạo lứt xào cải thìa đậu hũ', 'Món bún gạo lứt thanh nhẹ giàu chất xơ xào cùng cải thìa xanh mướt và đậu hũ mềm.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún gạo lứt xào cải thìa đậu hũ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 200, 'GRAM', 'Bún gạo lứt ngâm mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún gạo lứt xào cải thìa đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt khúc vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Bún gạo lứt xào cải thìa đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Thái sợi chiên sơ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún gạo lứt xào cải thìa đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Chần bún gạo lứt qua nước sôi rồi xả nước lạnh để ráo.'
FROM recipes r
WHERE r.title = 'Bún gạo lứt xào cải thìa đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào chín cải thìa và đậu hũ với dầu thực vật và nước tương.'
FROM recipes r
WHERE r.title = 'Bún gạo lứt xào cải thìa đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho bún vào đảo nhẹ tay cho ngấm đều gia vị rồi múc ra đĩa.'
FROM recipes r
WHERE r.title = 'Bún gạo lứt xào cải thìa đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 39: Cơm sốt cà ri rau củ nước cốt dừa
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm sốt cà ri rau củ nước cốt dừa', 'Cơm trắng ăn cùng sốt cà ri rau củ béo ngậy từ khoai tây, cà rốt và nước cốt dừa.', NULL, 2, 20, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm sốt cà ri rau củ nước cốt dừa');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Nấu dẻo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'khoai tay'
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Đã hấp mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ga'
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'ML', 'Tạo độ béo ngậy'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nuoc cot dua'
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào săn khoai tây, cà rốt và đậu gà với bột cà ri và sả băm.'
FROM recipes r
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm nước hầm rau củ đun sôi cho khoai và cà rốt chín mềm.'
FROM recipes r
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Đổ nước cốt dừa vào khuấy đều sôi lăn tăn rồi chan cùng cơm nóng.'
FROM recipes r
WHERE r.title = 'Cơm sốt cà ri rau củ nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 40: Mì cay Hàn Quốc chay rau nấm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Mì cay Hàn Quốc chay rau nấm', 'Tô mì cay nồng đậm vị ớt bột Hàn Quốc với nấm kim châm, bông cải và đậu hũ non.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Mì cay Hàn Quốc chay rau nấm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Vắt mì chay'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mi soi'
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt gốc tách khóm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam kim cham'
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khối vuông'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nước lẩu cay thơm từ ớt bột, tương ớt và nước hầm củ cải ngọt mát.'
FROM recipes r
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho vắt mì, bông cải xanh và nấm kim châm vào nấu sôi 3 phút.'
FROM recipes r
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm đậu hũ non lên trên, tắt bếp và ăn ngay khi còn sôi sùng sục.'
FROM recipes r
WHERE r.title = 'Mì cay Hàn Quốc chay rau nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 41: Bún gỏi già chay nấm bào ngư
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún gỏi già chay nấm bào ngư', 'Món bún gỏi già trứ danh với vị chua ngọt dịu của me chín, nấm bào ngư và rau sống.', NULL, 2, 20, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún gỏi già chay nấm bào ngư');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Trụng ráo nước'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xé sợi xào chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chiên giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bào sợi giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap chuoi'
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nước dùng chua ngọt từ nước cốt me chín và nước hầm củ cải ngọt mát.'
FROM recipes r
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào chín nấm bào ngư và đậu hũ nêm hạt nêm chay thơm.'
FROM recipes r
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho bún, bắp chuối vào tô, múc nấm đậu hũ và chan nước dùng thanh ngọt.'
FROM recipes r
WHERE r.title = 'Bún gỏi già chay nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 42: Cơm cháy sốt nấm kho quẹt nấm rơm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm cháy sốt nấm kho quẹt nấm rơm', 'Miếng cơm cháy vàng giòn rụm quết cùng sốt kho quẹt nấm rơm đậm đà khó cưỡng.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm cháy sốt nấm kho quẹt nấm rơm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Ép chảo làm cơm cháy'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm cháy sốt nấm kho quẹt nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Băm nhỏ kho kẹo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Cơm cháy sốt nấm kho quẹt nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Chiên giòn cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Cơm cháy sốt nấm kho quẹt nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cơm nguội dàn mỏng lên chảo chống dính áp chảo lửa nhỏ cho vàng giòn.'
FROM recipes r
WHERE r.title = 'Cơm cháy sốt nấm kho quẹt nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nấu nấm rơm băm cùng nước tương, ớt tươi và đường thốt nốt cho cô đặc thành kho quẹt.'
FROM recipes r
WHERE r.title = 'Cơm cháy sốt nấm kho quẹt nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Bẻ từng miếng cơm cháy giòn rụm chấm ngập chén kho quẹt nấm nóng hổi.'
FROM recipes r
WHERE r.title = 'Cơm cháy sốt nấm kho quẹt nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 43: Bún măng le nấm rơm đậu hũ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún măng le nấm rơm đậu hũ', 'Tô bún măng le Tây Nguyên giòn ngọt nấu cùng nấm rơm và đậu hũ chiên mềm thơm.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún măng le nấm rơm đậu hũ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 250, 'GRAM', 'Bún sợi tươi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt vuông chiên'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Luộc măng le cho hết vị đắng rồi xào thơm với nấm rơm và gia vị.'
FROM recipes r
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nấu nước hầm măng cùng cà rốt cho nước ngọt đậm đà tự nhiên.'
FROM recipes r
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xếp bún vào tô, múc măng, nấm rơm và chan nước dùng nóng hổi.'
FROM recipes r
WHERE r.title = 'Bún măng le nấm rơm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 44: Cơm hấp nấm đông cô hạt sen
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cơm hấp nấm đông cô hạt sen', 'Cơm hạt mềm thơm hấp cùng nấm đông cô thái hạt lựu và hạt sen chín bùi ngọt dịu.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cơm hấp nấm đông cô hạt sen');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Nấu dẻo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gao thom'
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Ngâm nở xắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Bỏ tâm hấp mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Xắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào săn nấm đông cô, hạt sen và cà rốt với dầu mè thơm lừng.'
FROM recipes r
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trộn nhân xào vào gạo thơm đã vo sạch và nấu cơm như bình thường.'
FROM recipes r
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Khi cơm chín xới đều tay cho hạt sen và nấm quyện đều vào cơm thơm.'
FROM recipes r
WHERE r.title = 'Cơm hấp nấm đông cô hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 45: Phở xào áp chảo rau cải nấm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Phở xào áp chảo rau cải nấm', 'Bánh phở áp chảo cháy cạnh thơm phức phủ sốt nấm đùi gà và cải ngọt giòn sần sật.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Phở xào áp chảo rau cải nấm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 200, 'GRAM', 'Áp chảo cháy cạnh'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'banh pho'
WHERE r.title = 'Phở xào áp chảo rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Thái lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Phở xào áp chảo rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khúc vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Phở xào áp chảo rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Áp chảo bánh phở trên chảo gang lửa lớn cho mặt bánh cháy cạnh thơm giòn.'
FROM recipes r
WHERE r.title = 'Phở xào áp chảo rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào chín nấm đùi gà và cải thìa với nước tương và dầu hào chay sệt sánh.'
FROM recipes r
WHERE r.title = 'Phở xào áp chảo rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trút sốt nấm rau củ lên đĩa bánh phở áp chảo nóng hổi thưởng thức ngay.'
FROM recipes r
WHERE r.title = 'Phở xào áp chảo rau cải nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 46: Mì hoành thánh chay nhân nấm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Mì hoành thánh chay nhân nấm', 'Tô mì sợi dai ngon ăn cùng những viên hoành thánh nhân nấm mèo mộc nhĩ giòn ngọt.', NULL, 2, 25, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Mì hoành thánh chay nhân nấm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Trụng mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mi soi'
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Băm nhỏ làm nhân'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Rau ăn kèm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai thia'
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn nấm đùi gà, nấm mèo và gia vị gói vào vỏ hoành thánh thành viên xinh xắn.'
FROM recipes r
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Luộc chín hoành thánh trong nồi nước sôi rồi vớt ra để ráo.'
FROM recipes r
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xếp mì sợi, hoành thánh và cải thìa vào tô rồi chan nước súp thanh trong.'
FROM recipes r
WHERE r.title = 'Mì hoành thánh chay nhân nấm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 47: Bún gạo lứt trộn sốt mè đậu phộng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bún gạo lứt trộn sốt mè đậu phộng', 'Bún gạo lứt trộn khô cùng rau củ giòn mát và sốt bơ mè đậu phộng bùi béo.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bún gạo lứt trộn sốt mè đậu phộng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 200, 'GRAM', 'Bún gạo lứt trụng chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Áp chảo cắt sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang giã dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trụng chín bún gạo lứt xả nước lạnh cho sợi bún tơi xốp.'
FROM recipes r
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Pha sốt mè rang với nước tương, nước cốt chanh và chút mật mía sánh mịn.'
FROM recipes r
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trộn bún cùng dưa leo, cà rốt, đậu hũ và rưới sốt mè rắc đậu phộng lên đĩa.'
FROM recipes r
WHERE r.title = 'Bún gạo lứt trộn sốt mè đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 48: Đậu hũ sốt cà chua hành boa-rô
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ sốt cà chua hành boa-rô', 'Miếng đậu hũ chiên vàng sốt đẫm cà chua tươi sánh mịn thơm mùi hành boa-rô phi.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ sốt cà chua hành boa-rô');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Cắt miếng chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu hũ sốt cà chua hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Đậu hũ sốt cà chua hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Thái nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hanh paro'
WHERE r.title = 'Đậu hũ sốt cà chua hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Chiên đậu hũ vàng đều hai mặt rồi vớt ra đĩa có lót giấy thấm dầu.'
FROM recipes r
WHERE r.title = 'Đậu hũ sốt cà chua hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Phi thơm hành boa-rô xào cà chua cho nhuyễn mịn thành nước sốt sánh đỏ.'
FROM recipes r
WHERE r.title = 'Đậu hũ sốt cà chua hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho đậu hũ vào om lửa nhỏ 5 phút cho thấm đều sốt cà chua rồi tắt bếp.'
FROM recipes r
WHERE r.title = 'Đậu hũ sốt cà chua hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 49: Đậu hũ chiên sả ớt giòn rụm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ chiên sả ớt giòn rụm', 'Đậu hũ vàng ươm thơm nức mũi với lớp sả ớt băm chiên giòn cay cay đậm đà.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ chiên sả ớt giòn rụm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Cắt miếng vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu hũ chiên sả ớt giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'sa cay'
WHERE r.title = 'Đậu hũ chiên sả ớt giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 10, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ot'
WHERE r.title = 'Đậu hũ chiên sả ớt giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Khía nhẹ trên mặt miếng đậu hũ để khi chiên sả ớt dễ bám vào.'
FROM recipes r
WHERE r.title = 'Đậu hũ chiên sả ớt giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Phi thơm sả băm và ớt tươi trên chảo dầu nóng cho vàng giòn rồi vớt ra bát riêng.'
FROM recipes r
WHERE r.title = 'Đậu hũ chiên sả ớt giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Chiên đậu hũ vàng giòn hai mặt rồi trút sả ớt chiên lên mặt miếng đậu xóc đều.'
FROM recipes r
WHERE r.title = 'Đậu hũ chiên sả ớt giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 50: Đậu hũ non sốt nấm đông cô
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ non sốt nấm đông cô', 'Đậu hũ non mịn mượt như lụa phủ sốt nấm đông cô nâu óng ả thơm lừng dầu mè.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ non sốt nấm đông cô');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Hấp nóng nguyên khối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Đậu hũ non sốt nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Đậu hũ non sốt nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Hấp cách thủy cây đậu hũ non trong 5 phút cho nóng ấm rồi trút ra đĩa sâu lòng.'
FROM recipes r
WHERE r.title = 'Đậu hũ non sốt nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào thơm nấm đông cô với nước tương, dầu hào chay và chút nước dùng rau củ.'
FROM recipes r
WHERE r.title = 'Đậu hũ non sốt nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Hòa chút bột năng tạo độ sánh cho sốt nấm rồi rưới nhẹ nhàng lên đĩa đậu hũ non.'
FROM recipes r
WHERE r.title = 'Đậu hũ non sốt nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 51: Đậu hũ kho nấm rơm tiêu xanh
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ kho nấm rơm tiêu xanh', 'Món kho đậm vị truyền thống từ đậu hũ chiên vàng và nấm rơm ngọt nước thơm nồng tiêu xanh.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ kho nấm rơm tiêu xanh');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Chiên vàng cắt khối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu hũ kho nấm rơm tiêu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Rửa sạch chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Đậu hũ kho nấm rơm tiêu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Chiên sơ đậu hũ và nấm rơm cho săn mặt ngoài.'
FROM recipes r
WHERE r.title = 'Đậu hũ kho nấm rơm tiêu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thắng nước màu đường thốt nốt rồi trút đậu hũ và nấm rơm vào đảo đều với nước tương.'
FROM recipes r
WHERE r.title = 'Đậu hũ kho nấm rơm tiêu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm tiêu sọ xanh đập dập, đậy nắp kho liu riu 15 phút cho nước kho keo lại đậm đà.'
FROM recipes r
WHERE r.title = 'Đậu hũ kho nấm rơm tiêu xanh'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 52: Chả đậu xanh hấp lá chuối
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Chả đậu xanh hấp lá chuối', 'Miếng chả chay dẻo bùi thơm lừng mùi lá chuối làm từ đậu xanh nguyên chất và nấm mèo.', NULL, 2, 20, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Chả đậu xanh hấp lá chuối');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Đãi vỏ ngâm mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau xanh'
WHERE r.title = 'Chả đậu xanh hấp lá chuối'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Ngâm nở băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Chả đậu xanh hấp lá chuối'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Hấp chín đậu xanh cà vỏ rồi giã nhuyễn khi còn nóng ấm.'
FROM recipes r
WHERE r.title = 'Chả đậu xanh hấp lá chuối'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trộn đậu xanh mịn cùng nấm mèo băm nhỏ, tiêu sọ và hạt nêm chay cho dẻo quánh.'
FROM recipes r
WHERE r.title = 'Chả đậu xanh hấp lá chuối'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Gói chặt trong lá chuối tươi và hấp chín 20 phút rồi cắt khoanh bày ra đĩa.'
FROM recipes r
WHERE r.title = 'Chả đậu xanh hấp lá chuối'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 53: Chả giò bắp đậu hũ giòn tan
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Chả giò bắp đậu hũ giòn tan', 'Cuộn chả giò nhân bắp ngọt, đậu hũ và nấm mèo chiên vàng ươm giòn rụm khó quên.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Chả giò bắp đậu hũ giòn tan');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Bào mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap ngot'
WHERE r.title = 'Chả giò bắp đậu hũ giòn tan'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Bóp nhuyễn vắt ráo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Chả giò bắp đậu hũ giòn tan'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Chả giò bắp đậu hũ giòn tan'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn bắp ngọt bào nhuyễn cùng đậu hũ ráo nước và nấm mèo băm nhỏ.'
FROM recipes r
WHERE r.title = 'Chả giò bắp đậu hũ giòn tan'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cuộn nhân vào bánh tráng xốp thành những cuốn chả giò thon dài đều đặn.'
FROM recipes r
WHERE r.title = 'Chả giò bắp đậu hũ giòn tan'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Chiên chả giò trong dầu sôi ngập cuốn cho đến khi vỏ bánh vàng giòn rụm.'
FROM recipes r
WHERE r.title = 'Chả giò bắp đậu hũ giòn tan'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 54: Đậu gà sốt cà ri nước cốt dừa
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu gà sốt cà ri nước cốt dừa', 'Hạt đậu gà bở bùi ngập trong sốt cà ri vàng óng thơm phức nước cốt dừa béo thơm.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu gà sốt cà ri nước cốt dừa');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Đã hấp mềm bở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ga'
WHERE r.title = 'Đậu gà sốt cà ri nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'ML', 'Tạo sốt béo ngậy'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nuoc cot dua'
WHERE r.title = 'Đậu gà sốt cà ri nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Đậu gà sốt cà ri nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Phi thơm sả băm và bột cà ri cho dậy màu vàng óng ả thơm lừng.'
FROM recipes r
WHERE r.title = 'Đậu gà sốt cà ri nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trút đậu gà chín và cà rốt vào xào săn rồi cho nước hầm rau củ đun sôi 10 phút.'
FROM recipes r
WHERE r.title = 'Đậu gà sốt cà ri nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm nước cốt dừa vào khuấy đều cho sốt sánh ngậy rồi tắt bếp ăn kèm cơm trắng.'
FROM recipes r
WHERE r.title = 'Đậu gà sốt cà ri nước cốt dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 55: Đậu gà rang muối ớt thảo mộc
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu gà rang muối ớt thảo mộc', 'Món ăn vặt hoặc khai vị giòn bùi mặn mà từ hạt đậu gà rang lắc muối ớt cay tê.', NULL, 2, 10, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu gà rang muối ớt thảo mộc');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Luộc chín để ráo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ga'
WHERE r.title = 'Đậu gà rang muối ớt thảo mộc'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 10, 'GRAM', 'Ớt bột hoặc băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ot'
WHERE r.title = 'Đậu gà rang muối ớt thảo mộc'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'ML', 'Quét mặt đậu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau an'
WHERE r.title = 'Đậu gà rang muối ớt thảo mộc'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Luộc đậu gà chín mềm rồi thấm khô hoàn toàn bằng khăn sạch.'
FROM recipes r
WHERE r.title = 'Đậu gà rang muối ớt thảo mộc'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trộn đậu gà với chút dầu ăn, ớt tươi băm và muối hồng cho áo đều mặt hạt đậu.'
FROM recipes r
WHERE r.title = 'Đậu gà rang muối ớt thảo mộc'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nướng trong lò hoặc nồi chiên không dầu ở 180 độ C trong 15 phút cho giòn rụm.'
FROM recipes r
WHERE r.title = 'Đậu gà rang muối ớt thảo mộc'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 56: Đậu lăng hầm khoai tây cà rốt
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu lăng hầm khoai tây cà rốt', 'Món hầm sánh sệt giàu sắt và protein từ đậu lăng đỏ hòa quyện cùng khoai tây bùi béo.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu lăng hầm khoai tây cà rốt');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Rửa sạch ráo nước'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau lang do'
WHERE r.title = 'Đậu lăng hầm khoai tây cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt khối vuông'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'khoai tay'
WHERE r.title = 'Đậu lăng hầm khoai tây cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt khoanh tròn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Đậu lăng hầm khoai tây cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào thơm hành boa-rô rồi trút khoai tây và cà rốt vào đảo săn.'
FROM recipes r
WHERE r.title = 'Đậu lăng hầm khoai tây cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho đậu lăng đỏ và nước lọc vào đun sôi rồi hạ lửa nhỏ hầm liu riu 20 phút.'
FROM recipes r
WHERE r.title = 'Đậu lăng hầm khoai tây cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Khi đậu lăng nhừ sánh mịn và khoai mềm bở, nêm hạt nêm chay rồi múc ra bát.'
FROM recipes r
WHERE r.title = 'Đậu lăng hầm khoai tây cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 57: Súp đậu lăng bí đỏ hạt sen
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Súp đậu lăng bí đỏ hạt sen', 'Súp bí đỏ sánh mịn vàng ruộm kết hợp đậu lăng giàu đạm và hạt sen bổ dưỡng.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Súp đậu lăng bí đỏ hạt sen');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Nấu nhừ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau lang do'
WHERE r.title = 'Súp đậu lăng bí đỏ hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Cắt lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bi do'
WHERE r.title = 'Súp đậu lăng bí đỏ hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bỏ tâm hấp mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Súp đậu lăng bí đỏ hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu bí đỏ, đậu lăng đỏ và hạt sen trong nước sôi đến khi các nguyên liệu chín nhừ.'
FROM recipes r
WHERE r.title = 'Súp đậu lăng bí đỏ hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho toàn bộ hỗn hợp vào cối xay mịn thành chất súp sánh mượt màu vàng óng.'
FROM recipes r
WHERE r.title = 'Súp đậu lăng bí đỏ hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Đun sôi lại súp, nêm muối tiêu vừa ăn và trang trí chút hạt mè rang lên mặt.'
FROM recipes r
WHERE r.title = 'Súp đậu lăng bí đỏ hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 58: Tàu hũ ky cuộn nấm sốt dầu hào chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Tàu hũ ky cuộn nấm sốt dầu hào chay', 'Tàu hũ ky giòn dai cuộn nhân nấm kim châm óng ả sốt dầu hào chay thơm lừng.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Lá ngâm mềm ráo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'tau hu ky'
WHERE r.title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt gốc rửa sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam kim cham'
WHERE r.title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Cắt sợi nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trải lá tàu hũ ky, xếp nấm kim châm và nấm đông cô vào giữa rồi cuộn chặt lại.'
FROM recipes r
WHERE r.title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Chiên áp chảo cuộn tàu hũ ky cho vàng thơm hai mặt giòn rụm.'
FROM recipes r
WHERE r.title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Đun sôi sốt dầu hào chay với nước tương rồi cho cuộn tàu hũ ky vào rim 5 phút.'
FROM recipes r
WHERE r.title = 'Tàu hũ ky cuộn nấm sốt dầu hào chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 59: Đậu hũ chiên giòn sốt mè rang
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ chiên giòn sốt mè rang', 'Đậu hũ chiên vàng ươm phủ lớp sốt mè rang bùi ngậy chuẩn phong cách ẩm thực thanh tịnh.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ chiên giòn sốt mè rang');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Cắt khối chiên giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu hũ chiên giòn sốt mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 25, 'GRAM', 'Rang vàng thơm lừng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'me'
WHERE r.title = 'Đậu hũ chiên giòn sốt mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cắt đậu hũ thành khối vuông vừa ăn rồi chiên ngập dầu cho vỏ ngoài vàng giòn.'
FROM recipes r
WHERE r.title = 'Đậu hũ chiên giòn sốt mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xay nhuyễn mè rang cùng nước tương, chút giấm táo và đường thốt nốt thành sốt sánh.'
FROM recipes r
WHERE r.title = 'Đậu hũ chiên giòn sốt mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Bày đậu hũ chiên ra đĩa rồi rưới ngập lớp sốt mè rang béo bùi lên trên.'
FROM recipes r
WHERE r.title = 'Đậu hũ chiên giòn sốt mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 60: Đậu hũ kho nước dừa xiêm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ kho nước dừa xiêm', 'Món kho đậm đà màu cánh gián tự nhiên từ nước dừa tươi thấm đượm vào từng thớ đậu hũ.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ kho nước dừa xiêm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu hũ kho nước dừa xiêm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Đậu hũ kho nước dừa xiêm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Đậu hũ cắt miếng vuông dày chiên vàng đều bốn mặt cho dai ngon.'
FROM recipes r
WHERE r.title = 'Đậu hũ kho nước dừa xiêm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xếp đậu hũ và nấm rơm vào nồi đất, đổ nước dừa tươi ngập xâm xấp mặt đậu.'
FROM recipes r
WHERE r.title = 'Đậu hũ kho nước dừa xiêm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm nước tương, tiêu sọ và kho trên lửa nhỏ liu riu cho nước dừa cạn sệt óng ánh.'
FROM recipes r
WHERE r.title = 'Đậu hũ kho nước dừa xiêm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 61: Nấm đùi gà áp chảo sốt tiêu đen
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nấm đùi gà áp chảo sốt tiêu đen', 'Cây nấm đùi gà ngọt giòn như thịt nấm áp chảo vàng phủ đẫm sốt tiêu đen cay nồng.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nấm đùi gà áp chảo sốt tiêu đen');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Khía ca-rô áp chảo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Nấm đùi gà áp chảo sốt tiêu đen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Rửa sạch nấm đùi gà, cắt khoanh dày 2cm và khía vảy rồng hai mặt.'
FROM recipes r
WHERE r.title = 'Nấm đùi gà áp chảo sốt tiêu đen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Áp chảo nấm với chút dầu thực vật cho hai mặt xém vàng nâu thơm phức.'
FROM recipes r
WHERE r.title = 'Nấm đùi gà áp chảo sốt tiêu đen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nấu sốt tiêu đen với nước tương, ớt và chút bột năng rồi đổ vào nấm đảo đều 2 phút.'
FROM recipes r
WHERE r.title = 'Nấm đùi gà áp chảo sốt tiêu đen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 62: Chả nấm thì là rán giòn
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Chả nấm thì là rán giòn', 'Miếng chả chay thơm nức mũi mùi rau thì là tươi quyện cùng đậu hũ và nấm mèo giòn sần sật.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Chả nấm thì là rán giòn');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Tán nhuyễn quết mịn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Chả nấm thì là rán giòn'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Chả nấm thì là rán giòn'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Chả nấm thì là rán giòn'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn đều đậu hũ tán nhuyễn, nấm mèo, nấm bào ngư và thì là thái nhỏ.'
FROM recipes r
WHERE r.title = 'Chả nấm thì là rán giòn'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm bột bắp, hạt nêm chay và tiêu sọ rồi nặn thành từng viên chả dẹt tròn.'
FROM recipes r
WHERE r.title = 'Chả nấm thì là rán giòn'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Chiên chả trong chảo dầu nóng cho vàng ruộm hai mặt rồi vớt ra thấm ráo dầu.'
FROM recipes r
WHERE r.title = 'Chả nấm thì là rán giòn'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 63: Đậu lăng đỏ nấu súp rau củ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu lăng đỏ nấu súp rau củ', 'Món súp bổ sung đạm và sắt tuyệt vời từ đậu lăng đỏ ninh mềm cùng cà rốt và cần tây.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu lăng đỏ nấu súp rau củ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Đậu lăng rửa sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau lang do'
WHERE r.title = 'Đậu lăng đỏ nấu súp rau củ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Xắt hạt lựu'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Đậu lăng đỏ nấu súp rau củ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'can tay'
WHERE r.title = 'Đậu lăng đỏ nấu súp rau củ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào thơm cần tây và cà rốt với chút dầu ăn cho dậy mùi thơm ngọt.'
FROM recipes r
WHERE r.title = 'Đậu lăng đỏ nấu súp rau củ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm đậu lăng đỏ và nước lọc đun sôi rồi hạ lửa hầm mềm trong 20 phút.'
FROM recipes r
WHERE r.title = 'Đậu lăng đỏ nấu súp rau củ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm chút muối tiêu vừa khẩu vị, dùng nóng như món khai vị ấm áp giàu năng lượng.'
FROM recipes r
WHERE r.title = 'Đậu lăng đỏ nấu súp rau củ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 64: Đậu gà kho tiêu hạt nước tương
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu gà kho tiêu hạt nước tương', 'Hạt đậu gà bở tơi ngấm trọn vị nước tương truyền thống thơm nồng từng hạt tiêu sọ.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu gà kho tiêu hạt nước tương');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Ngâm nở luộc chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ga'
WHERE r.title = 'Đậu gà kho tiêu hạt nước tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Luộc đậu gà chín bở mềm rồi vớt ra để ráo nước.'
FROM recipes r
WHERE r.title = 'Đậu gà kho tiêu hạt nước tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho đậu gà vào nồi đất cùng nước tương, chút mật mía và tiêu hạt đập dập.'
FROM recipes r
WHERE r.title = 'Đậu gà kho tiêu hạt nước tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Kho lửa nhỏ cho nước sốt ngấm sâu vào từng hạt đậu gà sánh keo óng ả.'
FROM recipes r
WHERE r.title = 'Đậu gà kho tiêu hạt nước tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 65: Đậu nành lông luộc muối biển
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu nành lông luộc muối biển', 'Món đậu nành lông tươi xanh ngọt mát luộc cùng muối biển giữ trọn vị thanh thuần.', NULL, 2, 5, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu nành lông luộc muối biển');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Đậu nành lông nguyên quả luộc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu nành lông luộc muối biển'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Rửa sạch quả đậu nành lông với nước muối loãng rồi để ráo.'
FROM recipes r
WHERE r.title = 'Đậu nành lông luộc muối biển'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đun sôi nồi nước với chút muối biển hạt to rồi thả đậu nành vào luộc 5 phút.'
FROM recipes r
WHERE r.title = 'Đậu nành lông luộc muối biển'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Vớt ra xả nước đá lạnh để giữ màu xanh mướt và thưởng thức hạt đậu bùi ngọt.'
FROM recipes r
WHERE r.title = 'Đậu nành lông luộc muối biển'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 66: Đậu hũ nhồi nấm mộc nhĩ sốt tương
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu hũ nhồi nấm mộc nhĩ sốt tương', 'Miếng đậu hũ vàng ươm nhồi đẫm nhân nấm rơm, mộc nhĩ om trong sốt nước tương sánh ngọt.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Khoét ruột chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Băm nhỏ làm nhân'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam meo'
WHERE r.title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn ruột đậu hũ cùng nấm rơm, nấm mèo và gia vị tiêu sọ làm nhân.'
FROM recipes r
WHERE r.title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nhồi nhân đầy đặn vào từng miếng đậu hũ chiên rồi chiên lại cho se mặt.'
FROM recipes r
WHERE r.title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xào sốt nước tương với hành boa-rô rồi cho đậu nhồi vào rim nhỏ lửa 10 phút.'
FROM recipes r
WHERE r.title = 'Đậu hũ nhồi nấm mộc nhĩ sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 67: Chả ram bắp đậu xanh giòn rụm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Chả ram bắp đậu xanh giòn rụm', 'Những cuốn chả ram bắp giòn tan rụm vỏ ngoài, bùi ngọt hạt bắp non bên trong.', NULL, 2, 20, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Chả ram bắp đậu xanh giòn rụm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Bào hạt nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap ngot'
WHERE r.title = 'Chả ram bắp đậu xanh giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Hấp chín tán nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau xanh'
WHERE r.title = 'Chả ram bắp đậu xanh giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn bắp non bào nhuyễn cùng đậu xanh hấp chín, tiêu sọ và chút đường.'
FROM recipes r
WHERE r.title = 'Chả ram bắp đậu xanh giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cuộn nhân thật chặt tay vào từng miếng bánh tráng gạo nhỏ nhắn.'
FROM recipes r
WHERE r.title = 'Chả ram bắp đậu xanh giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Chiên ngập dầu trên lửa vừa cho cuốn chả ram chín vàng ruộm đều các mặt.'
FROM recipes r
WHERE r.title = 'Chả ram bắp đậu xanh giòn rụm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 68: Canh chua rau muống bắp chuối chay
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh chua rau muống bắp chuối chay', 'Bát canh chua dân dã thanh mát miền Tây với rau muống giòn, bắp chuối và me chua.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh chua rau muống bắp chuối chay');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Nhặt ngọn non'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rau muong'
WHERE r.title = 'Canh chua rau muống bắp chuối chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Bào mỏng ngâm chanh'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap chuoi'
WHERE r.title = 'Canh chua rau muống bắp chuối chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt múi cau'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Canh chua rau muống bắp chuối chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Đun sôi nước me lọc lấy nước cốt chua thanh tự nhiên cho vào nồi canh.'
FROM recipes r
WHERE r.title = 'Canh chua rau muống bắp chuối chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả cà chua, bắp chuối và rau muống vào nấu sôi bùng trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Canh chua rau muống bắp chuối chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm gia vị chay vừa vặn, rắc ngò gai rau ngổ thơm lừng rồi tắt bếp ngay.'
FROM recipes r
WHERE r.title = 'Canh chua rau muống bắp chuối chay'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 69: Canh chua nấm dọc mùng thơm cà
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh chua nấm dọc mùng thơm cà', 'Món canh chua trứ danh với dọc mùng giòn xốp, thơm ngọt chua dịu và nấm rơm.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh chua nấm dọc mùng thơm cà');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Tước vỏ bóp muối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'doc mung'
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Thái lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'thom'
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt múi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào sơ cà chua và thơm với chút dầu ăn tạo màu đỏ cam bắt mắt.'
FROM recipes r
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đổ nước dùng vào đun sôi cùng nước cốt me chua và nấm rơm ngọt nước.'
FROM recipes r
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho dọc mùng đã bóp muối rửa sạch vào đun sôi lại 1 phút rồi múc ra bát.'
FROM recipes r
WHERE r.title = 'Canh chua nấm dọc mùng thơm cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 70: Canh khổ qua nhồi đậu hũ nấm rơm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh khổ qua nhồi đậu hũ nấm rơm', 'Món canh thanh nhiệt giải độc với quả khổ qua nhồi nhân đậu hũ nấm rơm ngọt mát.', NULL, 2, 20, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh khổ qua nhồi đậu hũ nấm rơm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Khoét ruột cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'kho qua'
WHERE r.title = 'Canh khổ qua nhồi đậu hũ nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Tán nhuyễn nhồi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Canh khổ qua nhồi đậu hũ nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Canh khổ qua nhồi đậu hũ nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Trộn đậu hũ tán nhuyễn cùng nấm rơm băm nhỏ, nấm mèo và tiêu sọ.'
FROM recipes r
WHERE r.title = 'Canh khổ qua nhồi đậu hũ nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Nhồi nhân vào từng khúc khổ qua rồi miết phẳng mặt hai đầu miếng khổ qua.'
FROM recipes r
WHERE r.title = 'Canh khổ qua nhồi đậu hũ nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thả khổ qua vào nồi nước dùng hầm củ cải đun sôi lửa nhỏ đến khi khổ qua mềm ngọt.'
FROM recipes r
WHERE r.title = 'Canh khổ qua nhồi đậu hũ nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 71: Canh rong biển đậu hũ non hạt sen
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh rong biển đậu hũ non hạt sen', 'Bát canh thanh mát bổ dưỡng phong cách thực dưỡng với rong biển và đậu hũ non mềm tan.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh rong biển đậu hũ non hạt sen');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Ngâm nở rửa sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rong bien'
WHERE r.title = 'Canh rong biển đậu hũ non hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt khối vuông'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Canh rong biển đậu hũ non hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Hấp chín bở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Canh rong biển đậu hũ non hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nước hầm rau củ cùng hạt sen tươi cho chín bở ngọt nước.'
FROM recipes r
WHERE r.title = 'Canh rong biển đậu hũ non hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả đậu hũ non và rong biển vào nấu sôi lăn tăn trên lửa nhỏ 2 phút.'
FROM recipes r
WHERE r.title = 'Canh rong biển đậu hũ non hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm vài lát gừng tươi thái sợi ấm áp rồi tắt bếp múc ra tô thưởng thức.'
FROM recipes r
WHERE r.title = 'Canh rong biển đậu hũ non hạt sen'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 72: Canh rau ngót nấu nấm rơm ngọt nước
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh rau ngót nấu nấm rơm ngọt nước', 'Canh rau ngót xanh mướt vò mềm nấu cùng nấm rơm ngọt thanh tự nhiên bồi bổ sức khỏe.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh rau ngót nấu nấm rơm ngọt nước');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Tuốt lá vò hơi dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rau ngot'
WHERE r.title = 'Canh rau ngót nấu nấm rơm ngọt nước'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Chẻ đôi xào thơm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Canh rau ngót nấu nấm rơm ngọt nước'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào thơm nấm rơm với chút dầu ăn và hạt nêm chay cho dậy mùi ngọt.'
FROM recipes r
WHERE r.title = 'Canh rau ngót nấu nấm rơm ngọt nước'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đổ nước vào đun sôi bùng rồi cho rau ngót đã vò dập vào nấu nhanh trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Canh rau ngót nấu nấm rơm ngọt nước'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nếm lại vừa khẩu vị, khi canh vừa sôi lại thì tắt bếp ngay để rau giữ màu xanh tươi.'
FROM recipes r
WHERE r.title = 'Canh rau ngót nấu nấm rơm ngọt nước'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 73: Canh khoai mỡ tím nấm bào ngư
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh khoai mỡ tím nấm bào ngư', 'Bát canh khoai mỡ tím dẻo quánh bùi béo kết hợp nấm bào ngư xé sợi ngọt lành.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh khoai mỡ tím nấm bào ngư');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Nạo mịn làm canh dẻo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'khoai lang'
WHERE r.title = 'Canh khoai mỡ tím nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Xé sợi nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Canh khoai mỡ tím nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Dùng muỗng nạo khoai mỡ thành hỗn hợp nhuyễn mịn dẻo quánh.'
FROM recipes r
WHERE r.title = 'Canh khoai mỡ tím nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào sơ nấm bào ngư với chút hành boa-rô cho thơm rồi trút nước vào nấu sôi.'
FROM recipes r
WHERE r.title = 'Canh khoai mỡ tím nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Hạ lửa từ từ múc từng muỗng khoai mỡ vào khuấy đều tay cho canh sánh dẻo thơm nồng.'
FROM recipes r
WHERE r.title = 'Canh khoai mỡ tím nấm bào ngư'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 74: Canh bí đỏ đậu phộng bùi béo
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh bí đỏ đậu phộng bùi béo', 'Món canh dân dã đậm đà với bí đỏ dẻo quánh nấu cùng đậu phộng giã dập thơm béo.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh bí đỏ đậu phộng bùi béo');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Cắt miếng vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bi do'
WHERE r.title = 'Canh bí đỏ đậu phộng bùi béo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Ngâm mềm giã dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Canh bí đỏ đậu phộng bùi béo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Đậu phộng ngâm nước ấm rồi giã dập cho tiết vị sữa béo ngậy.'
FROM recipes r
WHERE r.title = 'Canh bí đỏ đậu phộng bùi béo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đun sôi nước với đậu phộng trên lửa vừa trong 10 phút cho đậu phộng chín bùi.'
FROM recipes r
WHERE r.title = 'Canh bí đỏ đậu phộng bùi béo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thả bí đỏ vào nấu đến khi bí chín mềm dẻo, rắc ngò gai thái nhỏ lên mặt canh.'
FROM recipes r
WHERE r.title = 'Canh bí đỏ đậu phộng bùi béo'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 75: Canh cải bẹ xanh gừng non ấm bụng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh cải bẹ xanh gừng non ấm bụng', 'Canh cải bẹ xanh tươi ngon thoảng hương gừng cay nhẹ giúp ấm bụng và dễ tiêu hóa.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh cải bẹ xanh gừng non ấm bụng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Cắt khúc rửa sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cai be xanh'
WHERE r.title = 'Canh cải bẹ xanh gừng non ấm bụng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'GRAM', 'Thái sợi nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gung'
WHERE r.title = 'Canh cải bẹ xanh gừng non ấm bụng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Đun sôi nồi nước dùng rau củ với vài lát gừng tươi thái chỉ.'
FROM recipes r
WHERE r.title = 'Canh cải bẹ xanh gừng non ấm bụng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả cải bẹ xanh vào nấu trên lửa lớn cho canh sôi bùng giữ màu xanh non.'
FROM recipes r
WHERE r.title = 'Canh cải bẹ xanh gừng non ấm bụng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm chút muối hạt và hạt nêm chay vừa miệng rồi tắt bếp múc ra tô ngay.'
FROM recipes r
WHERE r.title = 'Canh cải bẹ xanh gừng non ấm bụng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 76: Canh chua bắp cải cà chua đậu hũ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh chua bắp cải cà chua đậu hũ', 'Canh bắp cải chua dịu nhẹ nhàng từ cà chua và giấm bỗng, kết hợp đậu hũ thanh mát.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh chua bắp cải cà chua đậu hũ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt miếng vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap cai'
WHERE r.title = 'Canh chua bắp cải cà chua đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt múi cau'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Canh chua bắp cải cà chua đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khối nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Canh chua bắp cải cà chua đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào mềm cà chua tạo nước canh màu đỏ đẹp rồi châm nước đun sôi.'
FROM recipes r
WHERE r.title = 'Canh chua bắp cải cà chua đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả bắp cải và đậu hũ vào nấu sôi chín tới cho bắp cải giữ độ giòn ngọt.'
FROM recipes r
WHERE r.title = 'Canh chua bắp cải cà chua đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm gia vị chua ngọt thanh dịu, rắc chút hành hoa chay lên bát canh.'
FROM recipes r
WHERE r.title = 'Canh chua bắp cải cà chua đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 77: Súp bí đỏ đậu gà hạt sen kem dừa
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Súp bí đỏ đậu gà hạt sen kem dừa', 'Món súp kem mịn màng như nhung kết hợp bí đỏ ngọt, đậu gà giàu đạm và nước cốt dừa.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Súp bí đỏ đậu gà hạt sen kem dừa');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Hấp chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bi do'
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Đã hấp mềm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau ga'
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Hấp chín'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'ML', 'Tạo vị béo thơm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nuoc cot dua'
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xay nhuyễn bí đỏ chín cùng đậu gà, hạt sen và chút nước hầm rau củ.'
FROM recipes r
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Đổ hỗn hợp súp ra nồi đun nóng nhẹ trên lửa nhỏ liu riu.'
FROM recipes r
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Rưới nước cốt dừa tạo vân kem đẹp mắt và thưởng thức khi súp còn ấm áp.'
FROM recipes r
WHERE r.title = 'Súp bí đỏ đậu gà hạt sen kem dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 78: Canh mướp hương mồng tơi nấm rơm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh mướp hương mồng tơi nấm rơm', 'Bát canh thanh mát giải nhiệt mùa hè với mướp hương thơm ngát và rau mồng tơi tươi non.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh mướp hương mồng tơi nấm rơm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Gọt vỏ cắt xéo'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'muop huong'
WHERE r.title = 'Canh mướp hương mồng tơi nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Rửa sạch ngắt lá'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rau mong toi'
WHERE r.title = 'Canh mướp hương mồng tơi nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Canh mướp hương mồng tơi nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Xào nấm rơm với chút dầu ăn cho thơm ngọt rồi thêm nước đun sôi.'
FROM recipes r
WHERE r.title = 'Canh mướp hương mồng tơi nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả mướp hương và rau mồng tơi vào nấu nhanh trên lửa lớn 2 phút.'
FROM recipes r
WHERE r.title = 'Canh mướp hương mồng tơi nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Khi mướp vừa chín tới tỏa hương ngào ngạt thì nêm nếm gia vị rồi tắt bếp.'
FROM recipes r
WHERE r.title = 'Canh mướp hương mồng tơi nấm rơm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 79: Canh củ sen táo đỏ nấm đông cô
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh củ sen táo đỏ nấm đông cô', 'Món canh dưỡng sinh bổ khí huyết với củ sen giòn sần sật và nấm đông cô thơm quý.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh củ sen táo đỏ nấm đông cô');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt khoanh tròn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cu sen'
WHERE r.title = 'Canh củ sen táo đỏ nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Ngâm nở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Canh củ sen táo đỏ nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Tỉa hoa'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Canh củ sen táo đỏ nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu củ sen cùng cà rốt và nấm đông cô trong nồi nước dùng trên lửa vừa.'
FROM recipes r
WHERE r.title = 'Canh củ sen táo đỏ nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Hầm liu riu 20 phút cho củ sen bở bùi và tiết vị ngọt đậm đà thanh mát.'
FROM recipes r
WHERE r.title = 'Canh củ sen táo đỏ nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm hạt nêm chay và rắc chút tiêu trắng thơm dịu trước khi múc ra bát.'
FROM recipes r
WHERE r.title = 'Canh củ sen táo đỏ nấm đông cô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 80: Súp măng tây bắp non ngọt dịu
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Súp măng tây bắp non ngọt dịu', 'Món súp thanh tao màu xanh ngọc bích từ măng tây tươi non quyện cùng hạt bắp giòn ngọt.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Súp măng tây bắp non ngọt dịu');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Tách hạt luộc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap ngot'
WHERE r.title = 'Súp măng tây bắp non ngọt dịu'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Súp măng tây bắp non ngọt dịu'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam kim cham'
WHERE r.title = 'Súp măng tây bắp non ngọt dịu'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu bắp ngọt và bông cải xanh trong nước hầm rau củ cho chín mềm.'
FROM recipes r
WHERE r.title = 'Súp măng tây bắp non ngọt dịu'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm nấm kim châm cắt khúc ngắn vào nấu sôi lăn tăn.'
FROM recipes r
WHERE r.title = 'Súp măng tây bắp non ngọt dịu'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Khuấy nhẹ chút bột bắp tạo độ sánh trong veo cho súp rồi múc ra bát dùng nóng.'
FROM recipes r
WHERE r.title = 'Súp măng tây bắp non ngọt dịu'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 81: Canh nấm hạt sen hoài sơn bồi bổ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh nấm hạt sen hoài sơn bồi bổ', 'Món canh bồi bổ sức khỏe tuyệt hảo từ hạt sen bùi béo và nấm bào ngư thanh nhẹ.', NULL, 2, 15, 25, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh nấm hạt sen hoài sơn bồi bổ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Hấp chín bở'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hat sen'
WHERE r.title = 'Canh nấm hạt sen hoài sơn bồi bổ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 70, 'GRAM', 'Xé miếng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Canh nấm hạt sen hoài sơn bồi bổ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt khoanh'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Canh nấm hạt sen hoài sơn bồi bổ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Hầm hạt sen tươi cùng cà rốt trong nước dùng ngọt thơm 15 phút.'
FROM recipes r
WHERE r.title = 'Canh nấm hạt sen hoài sơn bồi bổ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thả nấm bào ngư vào nấu thêm 5 phút cho nấm chín mềm ngọt nước.'
FROM recipes r
WHERE r.title = 'Canh nấm hạt sen hoài sơn bồi bổ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm gia vị vừa ăn, múc canh nóng ra tô dùng bồi bổ cơ thể.'
FROM recipes r
WHERE r.title = 'Canh nấm hạt sen hoài sơn bồi bổ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 82: Canh chua me đất đậu hũ cà chua
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Canh chua me đất đậu hũ cà chua', 'Bát canh chua dân dã thanh cảnh với vị chua mát từ me quả tươi, cà chua và đậu hũ non.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Canh chua me đất đậu hũ cà chua');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt khối vuông'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Canh chua me đất đậu hũ cà chua'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt múi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Canh chua me đất đậu hũ cà chua'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Ngắt ngọn non'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rau muong'
WHERE r.title = 'Canh chua me đất đậu hũ cà chua'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nấu nước sôi dầm me quả lấy nước chua thanh lọc bỏ hạt.'
FROM recipes r
WHERE r.title = 'Canh chua me đất đậu hũ cà chua'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho cà chua, đậu hũ non và rau muống vào nấu sôi bùng trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Canh chua me đất đậu hũ cà chua'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nếm vị chua dịu ngọt thanh vừa miệng rồi múc ra bát ăn cùng cơm trắng.'
FROM recipes r
WHERE r.title = 'Canh chua me đất đậu hũ cà chua'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 83: Nộm ngó sen nấm tuyết chua ngọt
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nộm ngó sen nấm tuyết chua ngọt', 'Đĩa nộm giòn sần sật từ ngó sen tươi, cà rốt bào sợi và nấm tuyết ngấm sốt chua cay.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nộm ngó sen nấm tuyết chua ngọt');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Chẻ sợi ngâm giấm đường'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ngo sen'
WHERE r.title = 'Nộm ngó sen nấm tuyết chua ngọt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Nộm ngó sen nấm tuyết chua ngọt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang giã dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Nộm ngó sen nấm tuyết chua ngọt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Ngó sen và cà rốt bào sợi vắt ráo nước trộn cùng sốt chanh đường ớt.'
FROM recipes r
WHERE r.title = 'Nộm ngó sen nấm tuyết chua ngọt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm nấm tuyết đã chần sơ giòn dai vào trộn đều tay cho ngấm vị.'
FROM recipes r
WHERE r.title = 'Nộm ngó sen nấm tuyết chua ngọt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Bày nộm ra đĩa, rắc đậu phộng rang vàng và rau răm thơm ngát lên trên.'
FROM recipes r
WHERE r.title = 'Nộm ngó sen nấm tuyết chua ngọt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 84: Gỏi củ hũ dừa đậu hũ đậu phộng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Gỏi củ hũ dừa đậu hũ đậu phộng', 'Món gỏi thượng hạng Bến Tre với củ hũ dừa giòn ngọt trắng nõn và đậu hũ chiên sợi.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Gỏi củ hũ dừa đậu hũ đậu phộng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Bào mỏng ngâm chanh'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'cu hu dua'
WHERE r.title = 'Gỏi củ hũ dừa đậu hũ đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt sợi chiên vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Gỏi củ hũ dừa đậu hũ đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang thơm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Gỏi củ hũ dừa đậu hũ đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Củ hũ dừa bào lát mỏng ngâm nước đá chanh cho giữ độ giòn trắng nõn.'
FROM recipes r
WHERE r.title = 'Gỏi củ hũ dừa đậu hũ đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Pha nước sốt trộn gỏi từ nước tắc, đường thốt nốt, ớt băm và nước mắm chay.'
FROM recipes r
WHERE r.title = 'Gỏi củ hũ dừa đậu hũ đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trộn củ hũ dừa, đậu hũ chiên sợi với sốt rồi rắc đậu phộng rang bùi béo lên đĩa.'
FROM recipes r
WHERE r.title = 'Gỏi củ hũ dừa đậu hũ đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 85: Gỏi xoài xanh bắp chuối đậu phộng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Gỏi xoài xanh bắp chuối đậu phộng', 'Đĩa gỏi chua cay kích thích vị giác với xoài xanh giòn tan và hoa chuối thái mỏng.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Gỏi xoài xanh bắp chuối đậu phộng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Thái mỏng ngâm nước muối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap chuoi'
WHERE r.title = 'Gỏi xoài xanh bắp chuối đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Gỏi xoài xanh bắp chuối đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang giã dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Gỏi xoài xanh bắp chuối đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Bắp chuối bào mỏng ngâm nước chanh loãng vớt ra vắt thật ráo nước.'
FROM recipes r
WHERE r.title = 'Gỏi xoài xanh bắp chuối đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Trộn xoài xanh, bắp chuối và cà rốt cùng nước mắm chua cay ngọt đậm đà.'
FROM recipes r
WHERE r.title = 'Gỏi xoài xanh bắp chuối đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Dọn gỏi ra đĩa sâu lòng, rắc thật nhiều đậu phộng rang giòn thơm lừng.'
FROM recipes r
WHERE r.title = 'Gỏi xoài xanh bắp chuối đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 86: Nộm đu đủ bò khô chay đậu phộng
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nộm đu đủ bò khô chay đậu phộng', 'Món nộm đường phố quen thuộc với đu đủ bào giòn tan, bò khô chay từ sườn đậu nành.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nộm đu đủ bò khô chay đậu phộng');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Chiên giòn làm bò khô chay'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Nộm đu đủ bò khô chay đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Bào sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Nộm đu đủ bò khô chay đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Nộm đu đủ bò khô chay đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Đu đủ xanh và cà rốt bào sợi ngâm nước đá cho giòn rụm rồi vắt ráo.'
FROM recipes r
WHERE r.title = 'Nộm đu đủ bò khô chay đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Làm bò khô chay từ sườn đậu nành xé sợi tẩm ướp ngũ vị chiên giòn.'
FROM recipes r
WHERE r.title = 'Nộm đu đủ bò khô chay đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trộn đu đủ với nước giấm đường ớt, bày bò khô chay và đậu phộng lên mặt.'
FROM recipes r
WHERE r.title = 'Nộm đu đủ bò khô chay đậu phộng'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 87: Rau củ luộc thập cẩm sốt chao mè
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Rau củ luộc thập cẩm sốt chao mè', 'Đĩa rau củ hấp chín tới xanh mướt chấm cùng nước sốt chao béo ngậy thơm lừng mè rang.', NULL, 2, 15, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Rau củ luộc thập cẩm sốt chao mè');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt miếng luộc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bong cai xanh'
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Tỉa hoa luộc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Luộc giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau que'
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'GRAM', 'Rang thơm'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'me'
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Luộc bông cải xanh, cà rốt và đậu que trong nước sôi có chút muối hạt cho giữ màu.'
FROM recipes r
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Vớt rau củ ra ngâm nước đá lạnh 2 phút rồi bày biện đẹp mắt lên đĩa.'
FROM recipes r
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Tán nhuyễn viên chao với đường, ớt tươi và mè rang làm sốt chấm béo bùi.'
FROM recipes r
WHERE r.title = 'Rau củ luộc thập cẩm sốt chao mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 88: Gỏi cuốn bún đậu hũ rau sống sốt tương
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Gỏi cuốn bún đậu hũ rau sống sốt tương', 'Cuốn gỏi tươi mát cuốn tròn bún tươi, đậu hũ chiên vàng và xà lách chấm tương đen.', NULL, 2, 20, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Bún tươi sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bun tuoi'
WHERE r.title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 100, 'GRAM', 'Cắt thanh dài chiên'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 50, 'GRAM', 'Cắt thanh dài'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Làm ướt bánh tráng gỏi cuốn đặt lên mặt đĩa sạch.'
FROM recipes r
WHERE r.title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xếp rau xà lách, dưa leo, bún tươi và thanh đậu hũ chiên vào cuộn thật chặt tay.'
FROM recipes r
WHERE r.title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Pha tương đen xào cùng bơ đậu phộng béo ngậy chấm cùng gỏi cuốn thanh mát.'
FROM recipes r
WHERE r.title = 'Gỏi cuốn bún đậu hũ rau sống sốt tương'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 89: Gỏi bắp cải tím đậu phộng rau răm
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Gỏi bắp cải tím đậu phộng rau răm', 'Đĩa gỏi tím biếc bắt mắt giòn tan hòa quyện cùng rau răm thơm nồng và đậu phộng rang.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Gỏi bắp cải tím đậu phộng rau răm');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Bào mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap cai tim'
WHERE r.title = 'Gỏi bắp cải tím đậu phộng rau răm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Chiên sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Gỏi bắp cải tím đậu phộng rau răm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 30, 'GRAM', 'Rang giã dập'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau phong'
WHERE r.title = 'Gỏi bắp cải tím đậu phộng rau răm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Bắp cải tím bào sợi mỏng ngâm nước muối loãng rồi rửa sạch vắt ráo.'
FROM recipes r
WHERE r.title = 'Gỏi bắp cải tím đậu phộng rau răm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Pha nước sốt giấm đường, tỏi ớt băm nhuyễn và hạt nêm chay vừa ăn.'
FROM recipes r
WHERE r.title = 'Gỏi bắp cải tím đậu phộng rau răm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Bóp nhẹ bắp cải với nước sốt, trộn thêm đậu hũ chiên sợi và rắc đậu phộng thơm.'
FROM recipes r
WHERE r.title = 'Gỏi bắp cải tím đậu phộng rau răm'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 90: Salad dầu giấm xà lách cà chua bơ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Salad dầu giấm xà lách cà chua bơ', 'Món salad thanh nhẹ giàu chất béo tốt từ quả bơ chín ngậy và cà chua bi chua ngọt.', NULL, 2, 10, 5, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Salad dầu giấm xà lách cà chua bơ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khối vuông'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bo'
WHERE r.title = 'Salad dầu giấm xà lách cà chua bơ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt múi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Salad dầu giấm xà lách cà chua bơ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dua leo'
WHERE r.title = 'Salad dầu giấm xà lách cà chua bơ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cắt khối quả bơ chín mềm, cà chua và dưa leo cho vào âu trộn lớn.'
FROM recipes r
WHERE r.title = 'Salad dầu giấm xà lách cà chua bơ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Pha sốt dầu giấm từ dầu oliu, giấm táo, chút muối tiêu và mật mía.'
FROM recipes r
WHERE r.title = 'Salad dầu giấm xà lách cà chua bơ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Rưới đều sốt dầu giấm lên âu salad, đảo thật nhẹ tay tránh làm nát bơ.'
FROM recipes r
WHERE r.title = 'Salad dầu giấm xà lách cà chua bơ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 91: Gỏi bưởi rong biển mè rang
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Gỏi bưởi rong biển mè rang', 'Tép bưởi chua ngọt mọng nước trộn cùng rong biển giòn dai và mè trắng rang thơm bùi.', NULL, 2, 15, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Gỏi bưởi rong biển mè rang');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Ngâm nở cắt sợi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rong bien'
WHERE r.title = 'Gỏi bưởi rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt sợi chiên'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Gỏi bưởi rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'GRAM', 'Rang vàng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'me'
WHERE r.title = 'Gỏi bưởi rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Tách từng tép bưởi hồng mọng nước cho vào tô lớn tránh làm dập tép bưởi.'
FROM recipes r
WHERE r.title = 'Gỏi bưởi rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Thêm rong biển đã chần sơ cắt sợi và đậu hũ chiên sợi vào tô.'
FROM recipes r
WHERE r.title = 'Gỏi bưởi rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trộn nhẹ cùng nước cốt tắc pha chua ngọt và rắc mè rang vàng lên đĩa gỏi.'
FROM recipes r
WHERE r.title = 'Gỏi bưởi rong biển mè rang'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 92: Rau càng cua trộn dầu giấm đậu hũ
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Rau càng cua trộn dầu giấm đậu hũ', 'Rau càng cua giòn rụm thanh mát mọc tự nhiên trộn sốt chua ngọt và đậu hũ chiên.', NULL, 2, 10, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Rau càng cua trộn dầu giấm đậu hũ');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt hạt lựu chiên giòn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Rau càng cua trộn dầu giấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Cắt lát mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Rau càng cua trộn dầu giấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Rau càng cua nhặt sạch ngâm nước muối rồi vớt ra để thật ráo nước.'
FROM recipes r
WHERE r.title = 'Rau càng cua trộn dầu giấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Chiên vàng giòn các viên đậu hũ cắt hạt lựu rồi để ráo dầu.'
FROM recipes r
WHERE r.title = 'Rau càng cua trộn dầu giấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trộn nhanh tay rau càng cua với dầu giấm chua ngọt, xếp đậu hũ chiên giòn lên trên.'
FROM recipes r
WHERE r.title = 'Rau càng cua trộn dầu giấm đậu hũ'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 93: Nấm rơm kho tiêu xanh đậm đà
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nấm rơm kho tiêu xanh đậm đà', 'Nấm rơm tròn mọng nước kho trong nồi đất đẫm vị nước tương và tiêu xanh cay nồng.', NULL, 2, 10, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nấm rơm kho tiêu xanh đậm đà');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 160, 'GRAM', 'Cắt chân chẻ đôi'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam rom'
WHERE r.title = 'Nấm rơm kho tiêu xanh đậm đà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Rửa sạch nấm rơm, ngâm nước muối loãng rồi vớt ra để ráo nước hoàn toàn.'
FROM recipes r
WHERE r.title = 'Nấm rơm kho tiêu xanh đậm đà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Phi thơm hành boa-rô, cho nấm rơm vào xào săn trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Nấm rơm kho tiêu xanh đậm đà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm nước tương, đường thốt nốt và tiêu xanh đập dập kho liu riu cho nước keo đặc.'
FROM recipes r
WHERE r.title = 'Nấm rơm kho tiêu xanh đậm đà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 94: Cà tím kho tộ sốt tương bần
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Cà tím kho tộ sốt tương bần', 'Cà tím mềm ngọt tan trong miệng thấm đượm hương vị tương bần truyền thống đậm đà.', NULL, 2, 10, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Cà tím kho tộ sốt tương bần');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 160, 'GRAM', 'Cắt khúc ngâm nước muối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca tim'
WHERE r.title = 'Cà tím kho tộ sốt tương bần'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Cắt cà tím thành khúc vừa ăn, ngâm nước muối loãng để không bị thâm đen.'
FROM recipes r
WHERE r.title = 'Cà tím kho tộ sốt tương bần'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Áp chảo sơ các mặt cà tím cho vàng xém thơm dịu.'
FROM recipes r
WHERE r.title = 'Cà tím kho tộ sốt tương bần'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Xếp cà tím vào tộ đất, rưới sốt tương bần pha gừng ớt rồi kho nhỏ lửa 15 phút.'
FROM recipes r
WHERE r.title = 'Cà tím kho tộ sốt tương bần'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 95: Nấm đùi gà kho gừng sả ớt
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nấm đùi gà kho gừng sả ớt', 'Nấm đùi gà dai ngọt giòn sần sật kho cùng gừng non thái sợi và sả băm ấm nồng.', NULL, 2, 15, 20, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nấm đùi gà kho gừng sả ớt');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 160, 'GRAM', 'Cắt khoanh khía nhẹ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dui ga'
WHERE r.title = 'Nấm đùi gà kho gừng sả ớt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 15, 'GRAM', 'Thái chỉ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'gung'
WHERE r.title = 'Nấm đùi gà kho gừng sả ớt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'sa cay'
WHERE r.title = 'Nấm đùi gà kho gừng sả ớt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Khía mặt nấm đùi gà rồi chiên sơ trên chảo cho vàng mặt ngoài.'
FROM recipes r
WHERE r.title = 'Nấm đùi gà kho gừng sả ớt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Phi thơm sả băm, gừng thái chỉ với chút dầu ăn cho bốc mùi thơm ấm áp.'
FROM recipes r
WHERE r.title = 'Nấm đùi gà kho gừng sả ớt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho nấm vào nồi đất cùng nước tương, ớt tươi kho lửa nhỏ cho nước sốt cạn sệt.'
FROM recipes r
WHERE r.title = 'Nấm đùi gà kho gừng sả ớt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 96: Rau muống xào tỏi hành boa-rô
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Rau muống xào tỏi hành boa-rô', 'Rau muống xào lửa lớn xanh mướt giòn sần sật thơm nức mũi mùi hành boa-rô phi vàng.', NULL, 2, 10, 10, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Rau muống xào tỏi hành boa-rô');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 180, 'GRAM', 'Nhặt khúc rửa sạch'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'rau muong'
WHERE r.title = 'Rau muống xào tỏi hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Băm nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'hanh paro'
WHERE r.title = 'Rau muống xào tỏi hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Nhặt rau muống lấy ngọn non, chần sơ qua nước sôi ngập muối rồi xả nước lạnh.'
FROM recipes r
WHERE r.title = 'Rau muống xào tỏi hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Phi vàng thơm hành boa-rô trên chảo dầu nóng lửa lớn.'
FROM recipes r
WHERE r.title = 'Rau muống xào tỏi hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trút rau muống vào xào nhanh tay trên lửa lớn với hạt nêm chay trong 2 phút rồi tắt bếp.'
FROM recipes r
WHERE r.title = 'Rau muống xào tỏi hành boa-rô'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 97: Khổ qua xào đậu hũ non
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Khổ qua xào đậu hũ non', 'Khổ qua giòn giòn đắng dịu hòa cùng vị béo mềm ngọt ngào của đậu hũ non.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Khổ qua xào đậu hũ non');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Bỏ ruột thái mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'kho qua'
WHERE r.title = 'Khổ qua xào đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt khối nhỏ'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu non'
WHERE r.title = 'Khổ qua xào đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Khổ qua bổ đôi nạo sạch ruột trắng bên trong rồi thái lát mỏng ngâm nước đá.'
FROM recipes r
WHERE r.title = 'Khổ qua xào đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào khổ qua trên lửa lớn với chút dầu ăn cho chín tái giữ độ giòn.'
FROM recipes r
WHERE r.title = 'Khổ qua xào đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nhẹ nhàng trút đậu hũ non vào đảo cùng gia vị nêm chay cho ngấm rồi dọn ra đĩa.'
FROM recipes r
WHERE r.title = 'Khổ qua xào đậu hũ non'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 98: Đậu que xào nấm đông cô dầu mè
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Đậu que xào nấm đông cô dầu mè', 'Đậu que xanh ngọt giòn sần sật xào cùng nấm đông cô thơm lừng hương dầu mè nguyên chất.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Đậu que xào nấm đông cô dầu mè');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 140, 'GRAM', 'Tước xơ cắt khúc'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau que'
WHERE r.title = 'Đậu que xào nấm đông cô dầu mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Ngâm mềm thái lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam dong co'
WHERE r.title = 'Đậu que xào nấm đông cô dầu mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Tước xơ đậu que, rửa sạch rồi chần sơ qua nước sôi để đậu giữ màu xanh biếc.'
FROM recipes r
WHERE r.title = 'Đậu que xào nấm đông cô dầu mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào thơm nấm đông cô với hành boa-rô cho chín mềm thơm phức.'
FROM recipes r
WHERE r.title = 'Đậu que xào nấm đông cô dầu mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Trút đậu que vào xào lửa lớn với nước tương, trước khi tắt bếp rưới 1 muỗng dầu mè.'
FROM recipes r
WHERE r.title = 'Đậu que xào nấm đông cô dầu mè'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 99: Bắp cải xào nấm bào ngư cà rốt
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Bắp cải xào nấm bào ngư cà rốt', 'Món xào thanh ngọt đủ màu sắc từ bắp cải trắng giòn, nấm bào ngư dai ngọt và cà rốt.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bắp cải xào nấm bào ngư cà rốt');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Cắt miếng vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'bap cai'
WHERE r.title = 'Bắp cải xào nấm bào ngư cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 60, 'GRAM', 'Xé miếng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Bắp cải xào nấm bào ngư cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 40, 'GRAM', 'Tỉa hoa thái mỏng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca rot'
WHERE r.title = 'Bắp cải xào nấm bào ngư cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Phi thơm hành boa-rô xào chín nấm bào ngư và cà rốt trên chảo nóng.'
FROM recipes r
WHERE r.title = 'Bắp cải xào nấm bào ngư cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Cho bắp cải vào xào cùng trên lửa lớn cho bắp cải chín tới giữ độ giòn ngọt.'
FROM recipes r
WHERE r.title = 'Bắp cải xào nấm bào ngư cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Nêm hạt nêm chay và chút tiêu sọ xay rồi múc ra đĩa sâu lòng dùng với cơm.'
FROM recipes r
WHERE r.title = 'Bắp cải xào nấm bào ngư cà rốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 100: Mít non kho nghệ sả nước dừa
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Mít non kho nghệ sả nước dừa', 'Mít non bùi dẻo màu vàng nghệ tươi bắt mắt ngấm vị ngọt thanh béo ngậy từ nước dừa tươi.', NULL, 2, 15, 30, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Mít non kho nghệ sả nước dừa');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 150, 'GRAM', 'Luộc chín cắt khối'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'mit non'
WHERE r.title = 'Mít non kho nghệ sả nước dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 20, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'sa cay'
WHERE r.title = 'Mít non kho nghệ sả nước dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Mít non luộc chín gọt bỏ gai và cùi, cắt thành từng miếng tam giác vừa ăn.'
FROM recipes r
WHERE r.title = 'Mít non kho nghệ sả nước dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Chiên sơ miếng mít non cho săn mặt rồi ướp với nghệ tươi, sả băm và nước tương.'
FROM recipes r
WHERE r.title = 'Mít non kho nghệ sả nước dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho mít vào nồi kho cùng nước dừa tươi trên lửa nhỏ đến khi nước kho sánh vàng óng.'
FROM recipes r
WHERE r.title = 'Mít non kho nghệ sả nước dừa'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 101: Dưa chua xào đậu hũ sốt cà
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Dưa chua xào đậu hũ sốt cà', 'Vị chua dịu kích thích tiêu hóa của cải chua xào hòa quyện cùng đậu hũ mềm và cà chua.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Dưa chua xào đậu hũ sốt cà');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 120, 'GRAM', 'Chiên vàng cắt miếng'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'dau hu'
WHERE r.title = 'Dưa chua xào đậu hũ sốt cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 80, 'GRAM', 'Cắt múi cau'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ca chua'
WHERE r.title = 'Dưa chua xào đậu hũ sốt cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Rửa sơ cải chua vắt ráo nước để giảm độ mặn và độ chua gắt.'
FROM recipes r
WHERE r.title = 'Dưa chua xào đậu hũ sốt cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Xào cà chua mềm tạo sốt sánh đỏ rồi cho cải chua vào đảo đều trên lửa lớn.'
FROM recipes r
WHERE r.title = 'Dưa chua xào đậu hũ sốt cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Thêm đậu hũ chiên vào xào chung nêm đường và hạt nêm chay cho hài hòa vị chua ngọt.'
FROM recipes r
WHERE r.title = 'Dưa chua xào đậu hũ sốt cà'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

-- ------------------------------------------------------------
-- RECIPE 102: Nấm bào ngư xào sả ớt lá lốt
-- ------------------------------------------------------------
INSERT INTO recipes (user_id, title, description, image_url, servings, prep_time, cook_time, status, created_at, updated_at, published_at)
SELECT 1, 'Nấm bào ngư xào sả ớt lá lốt', 'Nấm bào ngư dai giòn thơm nức mũi mùi sả ớt và lá lốt thái chỉ ấm nồng đặc sắc.', NULL, 2, 10, 15, 'PUBLISHED', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Nấm bào ngư xào sả ớt lá lốt');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 160, 'GRAM', 'Xé miếng vừa ăn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'nam bao ngu'
WHERE r.title = 'Nấm bào ngư xào sả ớt lá lốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 25, 'GRAM', 'Băm nhuyễn'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'sa cay'
WHERE r.title = 'Nấm bào ngư xào sả ớt lá lốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, note)
SELECT r.id, i.id, 10, 'GRAM', 'Thái lát'
FROM recipes r
JOIN ingredients i ON i.normalized_name = 'ot'
WHERE r.title = 'Nấm bào ngư xào sả ớt lá lốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_ingredients ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_id = i.id
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 1, 'Rửa sạch nấm bào ngư, vắt nhẹ ráo nước rồi xé thành từng tai vừa ăn.'
FROM recipes r
WHERE r.title = 'Nấm bào ngư xào sả ớt lá lốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 1
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 2, 'Phi thơm sả băm và ớt trên chảo dầu nóng cho vàng giòn dậy mùi thơm.'
FROM recipes r
WHERE r.title = 'Nấm bào ngư xào sả ớt lá lốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 2
);

INSERT INTO recipe_steps (recipe_id, step_number, instruction)
SELECT r.id, 3, 'Cho nấm vào xào nhanh tay trên lửa lớn, rắc lá lốt thái chỉ đảo đều 30 giây rồi tắt bếp.'
FROM recipes r
WHERE r.title = 'Nấm bào ngư xào sả ớt lá lốt'
  AND NOT EXISTS (
    SELECT 1 FROM recipe_steps rs
    WHERE rs.recipe_id = r.id AND rs.step_number = 3
);

