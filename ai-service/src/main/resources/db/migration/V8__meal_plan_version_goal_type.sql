ALTER TABLE ai_meal_plan_versions ADD COLUMN goal_type VARCHAR(20) NULL;

-- goal_type has never been editable, so the current plan's value also belongs to its older versions.
UPDATE ai_meal_plan_versions
SET goal_type = (SELECT p.goal_type FROM ai_meal_plans p WHERE p.id = ai_meal_plan_versions.meal_plan_id);
