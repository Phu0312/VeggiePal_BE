-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `blogs` (`id`, `author_id`, `category_id`, `title`, `content`, `thumbnail_url`, `status`, `view_count`, `vote_score`, `published_at`, `created_at`, `updated_at`) VALUES (1, 1, 1, '5 món chay đơn giản cho người mới bắt đầu', 'Ăn chay không cần phải quá phức tạp. Với những nguyên liệu quen thuộc như đậu hũ, rau xanh, nấm và gạo lứt, bạn hoàn toàn có thể chuẩn bị những bữa ăn vừa ngon vừa đầy đủ dinh dưỡng.

Trong bài viết này, VeggiePal gợi ý 5 món chay đơn giản gồm đậu hũ sốt cà chua, cơm gạo lứt rau củ, canh nấm, salad đậu gà và mì rau củ.

Điều quan trọng nhất khi mới bắt đầu ăn chay là duy trì sự đa dạng trong thực phẩm và chú ý bổ sung đủ protein thực vật.', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd', 'PUBLISHED', 128, 2, '2026-09-21 09:00:00.0', '2026-09-21 09:00:00.0', '2026-09-21 09:00:00.0');
INSERT INTO `blogs` (`id`, `author_id`, `category_id`, `title`, `content`, `thumbnail_url`, `status`, `view_count`, `vote_score`, `published_at`, `created_at`, `updated_at`) VALUES (2, 2, 2, 'Bữa sáng chay giàu protein nên ăn gì?', 'Protein là một trong những chất dinh dưỡng được quan tâm nhiều nhất khi ăn chay.

Một bữa sáng chay có thể kết hợp yến mạch, sữa đậu nành, hạt chia, bơ đậu phộng và trái cây. Ngoài ra, đậu hũ và các loại đậu cũng là nguồn protein thực vật rất tốt.

Việc phân bổ protein đều trong ngày giúp cơ thể hấp thu tốt hơn và duy trì cảm giác no lâu.', 'https://images.unsplash.com/photo-1498837167922-ddd27525d352', 'PUBLISHED', 85, 0, '2026-09-24 09:00:00.0', '2026-09-24 09:00:00.0', '2026-09-24 09:00:00.0');
INSERT INTO `blogs` (`id`, `author_id`, `category_id`, `title`, `content`, `thumbnail_url`, `status`, `view_count`, `vote_score`, `published_at`, `created_at`, `updated_at`) VALUES (3, 2, 3, 'Cách làm salad đậu gà và bơ nhanh trong 15 phút', 'Salad đậu gà và bơ là lựa chọn phù hợp cho những ngày bận rộn.

Bạn chỉ cần chuẩn bị đậu gà đã luộc, bơ, cà chua, rau xanh và một chút nước cốt chanh. Trộn đều tất cả nguyên liệu và nêm gia vị vừa ăn.

Món ăn này cung cấp protein thực vật, chất xơ và chất béo không bão hòa.', 'https://images.unsplash.com/photo-1546793665-c74683f339c1', 'PUBLISHED', 203, 3, '2026-09-27 09:00:00.0', '2026-09-27 09:00:00.0', '2026-09-27 09:00:00.0');
INSERT INTO `blogs` (`id`, `author_id`, `category_id`, `title`, `content`, `thumbnail_url`, `status`, `view_count`, `vote_score`, `published_at`, `created_at`, `updated_at`) VALUES (4, 3, 4, 'Phở chay nấu tại nhà có khó không?', 'Phở chay có thể nấu tại nhà với những nguyên liệu khá đơn giản.

Nước dùng có thể được tạo vị ngọt tự nhiên từ củ cải trắng, cà rốt, hành tây và nấm. Các loại gia vị như quế, hồi và gừng giúp tạo mùi thơm đặc trưng cho món phở.

Khi ăn có thể dùng thêm đậu hũ, nấm, rau thơm và giá.', 'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43', 'PUBLISHED', 156, 1, '2026-09-29 09:00:00.0', '2026-09-29 09:00:00.0', '2026-09-29 09:00:00.0');
INSERT INTO `blogs` (`id`, `author_id`, `category_id`, `title`, `content`, `thumbnail_url`, `status`, `view_count`, `vote_score`, `published_at`, `created_at`, `updated_at`) VALUES (5, 2, 6, 'Thực đơn chay 7 ngày cho người bận rộn', 'Đây là bản nháp cho bài viết về thực đơn chay trong một tuần.

Bài viết sẽ giới thiệu các món ăn đơn giản, dễ chuẩn bị và có thể meal prep trước.', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd', 'DRAFT', 0, 0, NULL, '2026-10-01 09:00:00.0', '2026-10-01 09:00:00.0');

INSERT INTO `comments` (`id`, `author_id`, `target_type`, `target_id`, `parent_comment_id`, `content`, `status`, `created_at`, `updated_at`) VALUES (1, 2, 'BLOG', 1, NULL, 'Bài viết rất hữu ích cho người mới bắt đầu ăn chay.', 'VISIBLE', '2026-09-22 09:00:00.0', '2026-09-22 09:00:00.0');
INSERT INTO `comments` (`id`, `author_id`, `target_type`, `target_id`, `parent_comment_id`, `content`, `status`, `created_at`, `updated_at`) VALUES (2, 3, 'BLOG', 1, NULL, 'Mình sẽ thử món cơm gạo lứt rau củ.', 'VISIBLE', '2026-09-23 09:00:00.0', '2026-09-23 09:00:00.0');
INSERT INTO `comments` (`id`, `author_id`, `target_type`, `target_id`, `parent_comment_id`, `content`, `status`, `created_at`, `updated_at`) VALUES (3, 1, 'BLOG', 1, 2, 'Bạn có thể thêm đậu hũ để tăng lượng protein cho món nhé.', 'VISIBLE', '2026-09-23 09:00:00.0', '2026-09-23 09:00:00.0');
INSERT INTO `comments` (`id`, `author_id`, `target_type`, `target_id`, `parent_comment_id`, `content`, `status`, `created_at`, `updated_at`) VALUES (4, 3, 'BLOG', 2, NULL, 'Sữa đậu nành và yến mạch ăn sáng khá tiện.', 'VISIBLE', '2026-09-25 09:00:00.0', '2026-09-25 09:00:00.0');
INSERT INTO `comments` (`id`, `author_id`, `target_type`, `target_id`, `parent_comment_id`, `content`, `status`, `created_at`, `updated_at`) VALUES (5, 1, 'BLOG', 3, NULL, 'Salad này nhìn đơn giản mà đủ chất.', 'VISIBLE', '2026-09-28 09:00:00.0', '2026-09-28 09:00:00.0');

INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (1, 2, 'BLOG', 1, 1, '2026-09-22 09:00:00.0', '2026-09-22 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (2, 3, 'BLOG', 1, 1, '2026-09-23 09:00:00.0', '2026-09-23 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (3, 1, 'BLOG', 2, 1, '2026-09-25 09:00:00.0', '2026-09-25 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (4, 3, 'BLOG', 2, -1, '2026-09-26 09:00:00.0', '2026-09-26 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (5, 1, 'BLOG', 3, 1, '2026-09-28 09:00:00.0', '2026-09-28 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (6, 2, 'BLOG', 3, 1, '2026-09-29 09:00:00.0', '2026-09-29 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (7, 3, 'BLOG', 3, 1, '2026-09-30 09:00:00.0', '2026-09-30 09:00:00.0');
INSERT INTO `content_votes` (`id`, `user_id`, `target_type`, `target_id`, `value`, `created_at`, `updated_at`) VALUES (8, 2, 'BLOG', 4, 1, '2026-09-30 09:00:00.0', '2026-09-30 09:00:00.0');
