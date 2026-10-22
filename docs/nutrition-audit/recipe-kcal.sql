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
