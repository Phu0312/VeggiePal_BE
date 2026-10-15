-- Vegetarian restaurants for the map (task sheet US15).
-- Upserts by id on every start: editing a row here updates the database on the next start.
-- Removing a row here does NOT delete it from the database.
-- LocationServiceIntegrationTests pins distances from Ben Thanh market to these rows.
INSERT INTO `vegetarian_restaurants` (`id`, `name`, `address`, `latitude`, `longitude`, `phone_number`, `rating`, `is_vegan_only`) VALUES
-- TP.HCM: Quận 1
(1, 'Phương Mai Chay Quán', '82/86F Võ Thị Sáu, P. Tân Định, Quận 1, TP.HCM', 10.79050, 106.69120, '0888339257', 4.9, 1),
(2, 'Shamballa Vegetarian Restaurant', '31 Lý Tự Trọng, P. Bến Nghé, Quận 1, TP.HCM', 10.77680, 106.70010, '0917876788', 4.7, 1),
(3, 'Be An Vegetarian Café', '99 Nguyễn Huệ, Saigon Garden, P. Bến Nghé, Quận 1, TP.HCM', 10.77410, 106.70320, '02873093899', 4.8, 1),
(4, 'Nhà hàng Chay SEN', '171 Nguyễn Thái Học, P. Phạm Ngũ Lão, Quận 1, TP.HCM', 10.76630, 106.69320, '02862782186', 4.3, 1),
(5, 'Mộc Vegetarian Cuisine', '549 Trần Hưng Đạo, P. Cầu Kho, Quận 1, TP.HCM', 10.75890, 106.68740, '0915840089', 4.8, 1),
(6, 'La Moi Plant-Based Restaurant', '84 Nguyễn Du, P. Bến Nghé, Quận 1, TP.HCM', 10.77850, 106.69890, '0902552619', 4.7, 1),
(7, 'Lẩu Nấm Chay An Nhiên', '126 Nguyễn Văn Thủ, P. Đa Kao, Quận 1, TP.HCM', 10.78710, 106.69680, '0389831049', 4.2, 1),
(8, 'Ẩm Thực Chay Tuệ (Mai Thị Lựu)', '76 Mai Thị Lựu, P. Đa Kao, Quận 1, TP.HCM', 10.79120, 106.69810, '0907299998', 4.7, 1),
(9, 'Pi Vegetarian Saigon', '187/15 Điện Biên Phủ, P. Đa Kao, Quận 1, TP.HCM', 10.78520, 106.69480, '0939393939', 4.6, 1),
(10, 'Quán Chay Nhà', '19/14 Nguyễn Thị Minh Khai, P. Bến Nghé, Quận 1, TP.HCM', 10.78280, 106.70080, '0986731625', 4.6, 1),

-- TP.HCM: TP. Thủ Đức / Quận 2
(11, 'Hum Garden & Restaurant', '32 Đường D10, P. Thảo Điền, TP. Thủ Đức, TP.HCM', 10.80620, 106.73110, '0899189229', 4.7, 1),
(12, 'Chay Garden Thảo Điền', '52 Xuân Thủy, P. Thảo Điền, TP. Thủ Đức, TP.HCM', 10.80510, 106.73350, '0898899223', 4.7, 1),
(13, 'Du Yên Vegetarian', '26B Đường số 10, P. Thảo Điền, TP. Thủ Đức, TP.HCM', 10.80480, 106.73280, '0941021514', 4.6, 1),
(14, 'Phạm Nghiêm Trai', '49 Nguyễn Đăng Giai, P. Thảo Điền, TP. Thủ Đức, TP.HCM', 10.80210, 106.73020, '0787688582', 4.8, 1),
(15, 'Metta Vegetarian An Phú', '6 Đường 19, P. An Phú, TP. Thủ Đức, TP.HCM', 10.80050, 106.74120, '0386145968', 4.6, 1),
(16, 'Happy Veggie Thảo Điền', '33/12 Quốc Hương, P. Thảo Điền, TP. Thủ Đức, TP.HCM', 10.80420, 106.72890, '0919113686', 4.5, 1),
(17, 'Thiện Duyên Vegetarian', '505B Võ Nguyên Giáp, P. An Phú, TP. Thủ Đức, TP.HCM', 10.80150, 106.74550, '0903954928', 4.5, 1),
(18, 'L''Herbanyste', '215E4 Nguyễn Văn Hưởng, P. Thảo Điền, TP. Thủ Đức, TP.HCM', 10.81230, 106.73210, '0357392910', 4.6, 1),
(19, 'Hạnh Nguyện Vegetarian', '6 Hiền Vương, P. Hiệp Phú, TP. Thủ Đức, TP.HCM', 10.84620, 106.77210, '0903829123', 4.5, 1),
(20, 'Nhà hàng Chay Xanh', '50 Lê Văn Thịnh, P. Bình Trưng Đông, TP. Thủ Đức, TP.HCM', 10.78520, 106.76210, '0938123456', 4.4, 1),

-- TP.HCM: Quận 3
(21, 'Hum Vegetarian Võ Văn Tần', '32 Võ Văn Tần, P. Võ Thị Sáu, Quận 3, TP.HCM', 10.77720, 106.69120, '02839301900', 4.8, 1),
(22, 'Chay Garden Quận 3', '52 Võ Văn Tần, P. Võ Thị Sáu, Quận 3, TP.HCM', 10.77650, 106.69080, '0898899223', 4.7, 1),
(23, 'Nhà hàng Chay Vĩnh Nghiêm', '339 Nam Kỳ Khởi Nghĩa, P. Võ Thị Sáu, Quận 3, TP.HCM', 10.79150, 106.68210, '02838481825', 4.5, 1),
(24, 'Bông Súng Vegetarian Food', '155 Nguyễn Đình Chiểu, P. Võ Thị Sáu, Quận 3, TP.HCM', 10.77820, 106.68910, '02838220377', 4.4, 1),
(25, 'Quán Chay Ngon Hùng Phát', '407 Nguyễn Đình Chiểu, P. 5, Quận 3, TP.HCM', 10.77120, 106.68150, '0903123890', 4.3, 1),
(26, 'Nhà hàng Chay Đan Điền', '96 Trương Định, P. Võ Thị Sáu, Quận 3, TP.HCM', 10.77580, 106.69150, '0909876543', 4.5, 1),
(27, 'Chay Loving Hut', '210 Nguyễn Đình Chiểu, P. Võ Thị Sáu, Quận 3, TP.HCM', 10.77610, 106.68820, '02839303321', 4.3, 1),
(28, 'Veggie House', '211 Võ Văn Tần, P. 5, Quận 3, TP.HCM', 10.77350, 106.68450, '0938554988', 4.5, 1),
(29, 'Cơm Chay Kỳ Đồng', '16/3 Kỳ Đồng, P. 9, Quận 3, TP.HCM', 10.78310, 106.68020, '02839318821', 4.2, 1),
(30, 'Quán Chay Bồ Đề Tịnh Tâm', '39 Kỳ Đồng, P. 9, Quận 3, TP.HCM', 10.78350, 106.67980, '0908123789', 4.3, 1),

-- TP.HCM: Quận 5 & Quận 6
(31, 'Chay Bếp Xanh An Duyên', '10 Nguyễn Tri Phương, P. 6, Quận 5, TP.HCM', 10.75480, 106.66980, '02822188880', 4.6, 1),
(32, 'Buddha Chay Nguyễn Chí Thanh', '5 Nguyễn Chí Thanh, P. 9, Quận 5, TP.HCM', 10.75820, 106.67010, '02838351118', 4.5, 1),
(33, 'Nhà hàng Tiên Chay', '155 Lê Hồng Phong, P. 3, Quận 5, TP.HCM', 10.75620, 106.67810, '0908882345', 4.3, 1),
(34, 'Nhà hàng Chay Sala', '538 An Dương Vương, P. 9, Quận 5, TP.HCM', 10.75210, 106.66520, '0903332211', 4.4, 1),
(35, 'Nhà hàng Chay Vajra Lão Tử', '38 Lão Tử, P. 15, Quận 5, TP.HCM', 10.75280, 106.65780, '02839522888', 4.5, 1),
(36, 'Quán Cơm Chay Phật Hữu Duyên', '527 Nguyễn Trãi, P. 7, Quận 5, TP.HCM', 10.75150, 106.66120, '02838536632', 4.3, 1),
(37, 'Tuệ Mẫn Vegetarian', '59B Trần Hưng Đạo, P. 6, Quận 5, TP.HCM', 10.75380, 106.67420, '0902345678', 4.5, 1),
(38, 'Cơm Chay Giác Duyên', '360 Trần Phú, P. 7, Quận 5, TP.HCM', 10.75510, 106.66850, '02838350021', 4.2, 1),
(39, 'Sài Gòn Phố Palace Chay', '1118 Võ Văn Kiệt, P. 6, Quận 5, TP.HCM', 10.75080, 106.66210, '02838361111', 4.2, 1),
(40, 'Cơm Chay Quan Âm Hậu Giang', '137 Hậu Giang, P. 5, Quận 6, TP.HCM', 10.74850, 106.64320, '0903112233', 4.3, 1),

-- TP.HCM: Quận 10
(41, 'Nhà hàng Chay Vajra Lê Hồng Phong', '711 Lê Hồng Phong, P. 12, Quận 10, TP.HCM', 10.77150, 106.67120, '02838622111', 4.5, 1),
(42, 'Sân Mây Vegetarian', '771 Lê Hồng Phong, P. 12, Quận 10, TP.HCM', 10.77250, 106.67080, '0908112233', 4.4, 1),
(43, 'Liên Hoa Chay', '497/35 Sư Vạn Hạnh, P. 12, Quận 10, TP.HCM', 10.77420, 106.66810, '0903456789', 4.4, 1),
(44, 'Nhà hàng Chay Hương Đạo', '715 Lê Hồng Phong, P. 12, Quận 10, TP.HCM', 10.77180, 106.67110, '02822537866', 4.3, 1),
(45, 'Quán Chay Thiện Tâm', '389 Sư Vạn Hạnh, P. 12, Quận 10, TP.HCM', 10.77210, 106.66750, '02839797021', 4.3, 1),
(46, 'Quán Cơm Chay Thanh Lương', '545A Đường 3 Tháng 2, P. 8, Quận 10, TP.HCM', 10.76580, 106.66320, '02838552286', 4.1, 1),
(47, 'Thiên Quốc Chay', '421/8 Sư Vạn Hạnh, P. 12, Quận 10, TP.HCM', 10.77310, 106.66780, '0909123456', 4.3, 1),
(48, 'Hoa Thiên Đàng Chay', '25A Bắc Hải, P. 15, Quận 10, TP.HCM', 10.78120, 106.66210, '0902998877', 4.3, 1),
(49, 'Bánh Mì & Cơm Chay Thôi Kệ', '773 Lê Hồng Phong, P. 12, Quận 10, TP.HCM', 10.77280, 106.67050, '0565027649', 4.4, 1),
(50, 'Chay Phù Trúc', '42 Trần Minh Quyền, P. 11, Quận 10, TP.HCM', 10.77020, 106.67250, '0903654321', 4.2, 1),

-- TP.HCM: Quận Phú Nhuận
(51, 'Mầm Vegetarian', '197 Nguyễn Văn Trỗi, P. 10, Q. Phú Nhuận, TP.HCM', 10.79320, 106.67210, '0934076998', 4.8, 1),
(52, 'Here & Now Vegetarian', '220/12 Nguyễn Trọng Tuyển, P. 8, Q. Phú Nhuận, TP.HCM', 10.79580, 106.67150, '02862926669', 4.6, 1),
(53, 'Nhà hàng Chay Mani', '17 Hoa Mai, P. 7, Q. Phú Nhuận, TP.HCM', 10.79650, 106.68810, '0834155551', 4.2, 1),
(54, 'Heli Vegetarian Bistro', '92 Hoa Cúc, P. 7, Q. Phú Nhuận, TP.HCM', 10.79510, 106.68920, '0825252508', 4.6, 1),
(55, 'Shakti Chay & Cafe', '81 Phan Xích Long, P. 2, Q. Phú Nhuận, TP.HCM', 10.79710, 106.68720, '0934108558', 4.7, 1),
(56, 'Metta Vegetarian Phú Nhuận', '100 Trần Huy Liệu, P. 15, Q. Phú Nhuận, TP.HCM', 10.79210, 106.67850, '02862723066', 4.7, 1),
(57, 'Quán Chay Sen Vàng', '152 Nguyễn Trọng Tuyển, P. 8, Q. Phú Nhuận, TP.HCM', 10.79420, 106.67310, '0979238884', 4.5, 1),
(58, 'Cơm Chay Hương Sen', '143 Hồ Văn Huê, P. 9, Q. Phú Nhuận, TP.HCM', 10.80120, 106.67580, '02835039857', 4.4, 1),
(59, 'Chay Lá Bồ Đề', '358 Phan Xích Long, P. 2, Q. Phú Nhuận, TP.HCM', 10.79810, 106.68610, '0908674925', 4.2, 1),
(60, 'Quán Cơm Chay Bồ Đề', '140 Thích Quảng Đức, P. 4, Q. Phú Nhuận, TP.HCM', 10.80250, 106.68120, '0907956445', 4.3, 1),

-- TP.HCM: Quận Bình Thạnh & Tân Bình
(61, 'Nhà hàng Chay Thiền Ý (Cơ sở 1)', '36 Trần Quý Cáp, P. 11, Q. Bình Thạnh, TP.HCM', 10.80950, 106.69580, '0903112244', 4.6, 1),
(62, 'Chay Thiền Ý (Cơ sở 2)', '285A Nguyễn Văn Đậu, P. 11, Q. Bình Thạnh, TP.HCM', 10.81210, 106.69210, '0903112255', 4.5, 1),
(63, 'Zenhouse Café & Vegetarian', '60/2 Vân Côi, P. 7, Q. Tân Bình, TP.HCM', 10.78910, 106.65420, '02838686888', 4.6, 1),
(64, 'Buffet Chay D''GEMMA', '569 Lạc Long Quân, P. 10, Q. Tân Bình, TP.HCM', 10.77820, 106.64810, '0909998877', 4.5, 1),
(65, 'Hủ Tiếu Chay Cây Đề', '386 Lạc Long Quân, P. 5, Q. 11, TP.HCM', 10.76920, 106.64510, '02839755678', 4.4, 1),
(66, 'Bếp Chay Nhân Duyên', '12 Trương Công Định, P. 14, Q. Tân Bình, TP.HCM', 10.79850, 106.64120, '0903123456', 4.5, 1),
(67, 'Chay Thiền Ý Phổ Quang', '68 Phổ Quang, P. 2, Q. Tân Bình, TP.HCM', 10.80580, 106.66890, '0903112266', 4.5, 1),
(68, 'Quán Chay Pháp Hoa', '219 Lê Quang Định, P. 7, Q. Bình Thạnh, TP.HCM', 10.80820, 106.69120, '02838412345', 4.3, 1),
(69, 'Lẩu Nấm Chay An Nhiên Phạm Văn Đồng', '17 Phạm Văn Đồng, P. 3, Q. Gò Vấp, TP.HCM', 10.81980, 106.68920, '0908432903', 4.3, 1),
(70, 'Quán Chay Diệu Hạnh', '353 Bùi Đình Túy, P. 24, Q. Bình Thạnh, TP.HCM', 10.80890, 106.70210, '0909123890', 4.2, 1),

-- TP.HCM: Quận 7 & Quận 4
(71, 'Veganizta Vegetarian', '103 Tôn Dật Tiên, Crescent Residence 1, P. Tân Phú, Q. 7, TP.HCM', 10.72850, 106.71920, '0969835480', 4.6, 1),
(72, 'Tuệ Garden Phú Mỹ Hưng', '101 Nguyễn Bính, P. Tân Phong, Q. 7, TP.HCM', 10.73120, 106.70850, '0907299998', 4.7, 1),
(73, 'Chay Seva', '847 Huỳnh Tấn Phát, P. Phú Thuận, Q. 7, TP.HCM', 10.72580, 106.73810, '0901047718', 4.5, 1),
(74, 'Chay Kiều Đàm', '793/55/16 Trần Xuân Soạn, P. Tân Hưng, Q. 7, TP.HCM', 10.74120, 106.69850, '0903902270', 4.6, 1),
(75, 'Nhà hàng Chay An & Cafe', '40 Đường C, Cảnh Viên 2, P. Tân Phú, Q. 7, TP.HCM', 10.72510, 106.71230, '02822188368', 4.6, 1),
(76, 'Rau Củ Nấm Vegan Restaurant', '557 Lê Văn Lương, P. Tân Phong, Q. 7, TP.HCM', 10.73820, 106.70150, '0783473718', 4.4, 1),
(77, 'The Vegan Garden', '336 Hà Huy Tập, KP Mỹ Khánh 4, Q. 7, TP.HCM', 10.72950, 106.70610, '0916766024', 4.4, 1),
(78, 'Quán Chay Thanh Lạc Q4', '144 Nguyễn Tất Thành, P. 13, Q. 4, TP.HCM', 10.76120, 106.70980, '0903123789', 4.5, 1),
(79, 'Quán Chay Tâm Đức', '62/1A Lâm Văn Bền, P. Tân Thuận Tây, Q. 7, TP.HCM', 10.74820, 106.71850, '0374470723', 4.3, 1),
(80, 'Kamalas Vegan Lounge', '22 Đường P, Khu Panorama, P. Tân Phong, Q. 7, TP.HCM', 10.72620, 106.70520, '0931383199', 4.5, 1),

-- Các tỉnh Miền Nam: Bà Rịa - Vũng Tàu & Bình Dương
(81, 'Ẩm Thực Chay Quan Âm Liên Thọ', '36 Hoàng Hoa Thám, P. 2, TP. Vũng Tàu', 10.34210, 107.08510, '0918109927', 4.6, 1),
(82, 'Cơm Chay Hương Trần', '266 Lê Hồng Phong, P. 7, TP. Vũng Tàu', 10.35280, 107.08920, '0908123456', 4.5, 1),
(83, 'Bếp Chay Cỏ Mây', '77 Hoàng Hoa Thám, P. Thắng Tam, TP. Vũng Tàu', 10.34180, 107.08890, '0903987654', 4.6, 1),
(84, 'Nhà hàng Chay Thực Dưỡng Goodlife', '128 Hoàng Hoa Thám, P. 2, TP. Vũng Tàu', 10.34050, 107.09120, '0855555888', 4.7, 1),
(85, 'Nhà hàng Hương Vị Xưa Chay', '2 Hồ Đắc Di, P. 2, TP. Vũng Tàu', 10.34110, 107.08150, '0918234567', 4.4, 1),
(86, 'Chay Chánh Giác Vũng Tàu', '9K2 TTM Nguyễn Kiệm, P. 7, TP. Vũng Tàu', 10.35820, 107.08110, '0908765432', 4.3, 1),
(87, 'Vườn Chay Garden Restaurant', '38/5 Bình Giã, P. 8, TP. Vũng Tàu', 10.36120, 107.09520, '0909112233', 4.4, 1),
(88, 'Nhà hàng Chay Khai Tâm', '98 Thích Quảng Đức, P. Phú Cường, TP. Thủ Dầu Một, Bình Dương', 10.97820, 106.65410, '02743822111', 4.5, 1),
(89, 'Chay Tây Tạng Vajra Bình Dương', '45 Hoàng Hoa Thám, P. Hiệp Thành, TP. Thủ Dầu Một, Bình Dương', 10.98520, 106.66120, '0908234890', 4.4, 1),
(90, 'Cơm Chay An Lạc Thủ Dầu Một', '123 Yersin, P. Phú Cường, TP. Thủ Dầu Một, Bình Dương', 10.97950, 106.65280, '02743833444', 4.3, 1),

-- Các tỉnh Miền Nam: Cần Thơ, An Giang & Tây Ninh
(91, 'Cơm Chay Bách Thảo Cần Thơ', '86B Mậu Thân, P. An Hòa, Q. Ninh Kiều, TP. Cần Thơ', 10.03820, 105.77210, '02923831234', 4.5, 1),
(92, 'Quán Chay Cường Cần Thơ', '9 Đề Thám, P. An Cư, Q. Ninh Kiều, TP. Cần Thơ', 10.03210, 105.78120, '0908221133', 4.4, 1),
(93, 'Nhà hàng Chay Bát Nhã Cần Thơ', '237 Đường 30 Tháng 4, P. Hưng Lợi, Q. Ninh Kiều, TP. Cần Thơ', 10.02150, 105.76890, '02923845678', 4.5, 1),
(94, 'Cơm Chay O Đổng Cần Thơ', '23 Ngô Quyền, P. Tân An, Q. Ninh Kiều, TP. Cần Thơ', 10.03510, 105.78450, '0903112233', 4.3, 1),
(95, 'Nhà hàng Chay Thiện Tâm Cần Thơ', '45 Nguyễn Văn Cừ, P. An Hòa, Q. Ninh Kiều, TP. Cần Thơ', 10.04120, 105.76510, '02923899888', 4.4, 1),
(96, 'Cơm Chay Tri Tôn An Giang', '12 Nguyễn Trãi, TT. Tri Tôn, H. Tri Tôn, An Giang', 10.42150, 105.01230, '0918223344', 4.3, 1),
(97, 'Quán Chay Long Xuyên', '45 Phạm Cự Lượng, P. Mỹ Quý, TP. Long Xuyên, An Giang', 10.36820, 105.44120, '02963841234', 4.4, 1),
(98, 'Quán Chay Vương Miện Tây Ninh', '120 Phạm Hùng, TT. Hòa Thành, H. Hòa Thành, Tây Ninh', 11.28120, 106.12890, '02763842211', 4.5, 1),
(99, 'Cơm Chay Tri Ân Tây Ninh', '56 Nguyễn Văn Linh, P. Long Thành Bắc, TX. Hòa Thành, Tây Ninh', 11.27850, 106.13210, '0908123999', 4.4, 1),
(100, 'Nhà hàng Chay Tòa Thánh Tây Ninh', '18 QL22B, Hiệp Tân, TX. Hòa Thành, Tây Ninh', 11.28520, 106.11580, '02763833222', 4.6, 1)
AS new
ON DUPLICATE KEY UPDATE
    name = new.name,
    address = new.address,
    latitude = new.latitude,
    longitude = new.longitude,
    phone_number = new.phone_number,
    rating = new.rating,
    is_vegan_only = new.is_vegan_only;
