-- Initial fixture snapshot. Source: data/reference.json or data/demo.json.
-- Never edit an applied migration: add a new version for later changes.

INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (1, 'GLUTEN', 'Gluten (lúa mì, lúa mạch)', 'GRAIN');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (2, 'BUCKWHEAT', 'Kiều mạch', 'GRAIN');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (3, 'CORN', 'Bắp (ngô)', 'GRAIN');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (4, 'PEANUT', 'Đậu phộng', 'LEGUME');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (5, 'SOY', 'Đậu nành', 'LEGUME');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (6, 'MUNG_BEAN', 'Đậu xanh', 'LEGUME');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (7, 'CASHEW', 'Hạt điều', 'NUT_SEED');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (8, 'TREE_NUT', 'Hạt cây khác (hạnh nhân, óc chó, mắc ca)', 'NUT_SEED');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (9, 'SESAME', 'Mè (vừng)', 'NUT_SEED');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (10, 'COCONUT', 'Dừa', 'NUT_SEED');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (11, 'TOMATO', 'Cà chua', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (12, 'EGGPLANT', 'Cà tím', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (13, 'TARO', 'Khoai môn', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (14, 'CASSAVA', 'Khoai mì (sắn)', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (15, 'BAMBOO_SHOOT', 'Măng', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (16, 'CELERY', 'Cần tây', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (17, 'CORIANDER', 'Rau mùi (ngò)', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (18, 'GARLIC', 'Tỏi', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (19, 'ONION', 'Hành', 'VEGETABLE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (20, 'MANGO', 'Xoài', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (21, 'PINEAPPLE', 'Dứa (thơm)', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (22, 'PAPAYA', 'Đu đủ', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (23, 'AVOCADO', 'Bơ', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (24, 'DURIAN', 'Sầu riêng', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (25, 'JACKFRUIT', 'Mít', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (26, 'LYCHEE', 'Vải', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (27, 'STRAWBERRY', 'Dâu tây', 'FRUIT');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (28, 'MUSHROOM', 'Nấm (các loại)', 'MUSHROOM');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (29, 'CHILI', 'Ớt', 'SPICE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (30, 'MUSTARD', 'Mù tạt', 'SPICE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (31, 'SULPHITE', 'Sulfite', 'ADDITIVE');
INSERT INTO `allergens` (`id`, `code`, `name`, `category`) VALUES (32, 'MSG', 'Bột ngọt (MSG)', 'ADDITIVE');
