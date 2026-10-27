-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `user_subscriptions` (`id`, `user_id`, `plan_id`, `started_at`, `expires_at`, `updated_at`, `version`) VALUES (1, 1, 1, '2026-10-01 09:00:00', NULL, '2026-10-01 09:00:00', 0);
INSERT INTO `user_subscriptions` (`id`, `user_id`, `plan_id`, `started_at`, `expires_at`, `updated_at`, `version`) VALUES (2, 2, 2, '2026-10-01 09:00:00', '2026-10-31 09:00:00', '2026-10-01 09:00:00', 0);
INSERT INTO `user_subscriptions` (`id`, `user_id`, `plan_id`, `started_at`, `expires_at`, `updated_at`, `version`) VALUES (3, 3, 3, '2026-10-01 09:00:00', '2026-10-31 09:00:00', '2026-10-01 09:00:00', 0);
