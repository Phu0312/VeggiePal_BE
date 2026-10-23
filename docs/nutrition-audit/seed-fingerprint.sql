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
