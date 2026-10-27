-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `health_records` (`id`, `user_id`, `height_cm`, `weight_kg`, `bmi`, `recorded_at`, `created_at`, `updated_at`) VALUES (1, 1, 165, 55, 20.2, '2026-09-20 08:00:00', '2026-09-20 08:00:00', '2026-09-20 08:00:00');
INSERT INTO `health_records` (`id`, `user_id`, `height_cm`, `weight_kg`, `bmi`, `recorded_at`, `created_at`, `updated_at`) VALUES (2, 1, 165, 54.5, 20.0, '2026-09-27 08:00:00', '2026-09-27 08:00:00', '2026-09-27 08:00:00');
INSERT INTO `health_records` (`id`, `user_id`, `height_cm`, `weight_kg`, `bmi`, `recorded_at`, `created_at`, `updated_at`) VALUES (3, 2, 172, 65, 22.0, '2026-09-20 08:00:00', '2026-09-20 08:00:00', '2026-09-20 08:00:00');
INSERT INTO `health_records` (`id`, `user_id`, `height_cm`, `weight_kg`, `bmi`, `recorded_at`, `created_at`, `updated_at`) VALUES (4, 2, 172, 64.5, 21.8, '2026-09-27 08:00:00', '2026-09-27 08:00:00', '2026-09-27 08:00:00');
INSERT INTO `health_records` (`id`, `user_id`, `height_cm`, `weight_kg`, `bmi`, `recorded_at`, `created_at`, `updated_at`) VALUES (5, 3, 160, 52, 20.3, '2026-09-20 08:00:00', '2026-09-20 08:00:00', '2026-09-20 08:00:00');
INSERT INTO `health_records` (`id`, `user_id`, `height_cm`, `weight_kg`, `bmi`, `recorded_at`, `created_at`, `updated_at`) VALUES (6, 3, 160, 51.5, 20.1, '2026-09-27 08:00:00', '2026-09-27 08:00:00', '2026-09-27 08:00:00');

INSERT INTO `user_allergies` (`id`, `user_id`, `allergen_id`, `created_at`) VALUES (1, 1, 4, '2026-10-01 09:00:00');
INSERT INTO `user_allergies` (`id`, `user_id`, `allergen_id`, `created_at`) VALUES (2, 2, 5, '2026-10-01 09:00:00');
INSERT INTO `user_allergies` (`id`, `user_id`, `allergen_id`, `created_at`) VALUES (3, 3, 9, '2026-10-01 09:00:00');
