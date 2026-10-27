-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `categories` (`id`, `parent_id`, `type`, `name`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES (1, NULL, 'FOOD_TYPE', 'Món chính', 1, TRUE, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');
INSERT INTO `categories` (`id`, `parent_id`, `type`, `name`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES (2, NULL, 'FOOD_TYPE', 'Món ăn sáng', 2, TRUE, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');
INSERT INTO `categories` (`id`, `parent_id`, `type`, `name`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES (3, NULL, 'FOOD_TYPE', 'Salad', 3, TRUE, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');
INSERT INTO `categories` (`id`, `parent_id`, `type`, `name`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES (4, NULL, 'FOOD_TYPE', 'Món nước', 4, TRUE, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');
INSERT INTO `categories` (`id`, `parent_id`, `type`, `name`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES (5, NULL, 'RECIPE_TYPE', 'Việt Nam', 1, TRUE, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');
INSERT INTO `categories` (`id`, `parent_id`, `type`, `name`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES (6, NULL, 'RECIPE_TYPE', 'Healthy', 2, TRUE, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');
