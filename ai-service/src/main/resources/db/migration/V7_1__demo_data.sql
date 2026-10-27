-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `ai_conversations` (`id`, `user_id`, `title`, `created_at`, `updated_at`) VALUES (1, 2, 'Gợi ý bữa chay đơn giản (demo)', '2026-10-01 09:00:00', '2026-10-01 09:00:00');

INSERT INTO `ai_chat_messages` (`id`, `conversation_id`, `principal_key`, `role`, `content`, `prompt_tokens`, `completion_tokens`, `created_at`) VALUES (1, 1, 'user:2', 'user', 'Gợi ý cho mình một bữa chay dễ nấu.', 0, 0, '2026-10-01 09:00:00');
INSERT INTO `ai_chat_messages` (`id`, `conversation_id`, `principal_key`, `role`, `content`, `prompt_tokens`, `completion_tokens`, `created_at`) VALUES (2, 1, 'user:2', 'assistant', 'Bạn có thể thử cơm, đậu hũ sốt cà chua và rau luộc. Đây là hội thoại mẫu để giới thiệu giao diện VeggiePal.', 0, 0, '2026-10-01 09:00:01');
