-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `subscription_plans` (`id`, `code`, `price`, `duration_days`, `chat_per_day`, `meal_plans_per_month`, `videos_per_month`, `active`) VALUES (1, 'FREE', 0, 0, 5, 2, 0, TRUE);
INSERT INTO `subscription_plans` (`id`, `code`, `price`, `duration_days`, `chat_per_day`, `meal_plans_per_month`, `videos_per_month`, `active`) VALUES (2, 'PLUS', 29000, 30, 20, 8, 3, TRUE);
INSERT INTO `subscription_plans` (`id`, `code`, `price`, `duration_days`, `chat_per_day`, `meal_plans_per_month`, `videos_per_month`, `active`) VALUES (3, 'PRO', 59000, 30, 50, 20, 8, TRUE);
