# Thử thách 7 ngày và huy hiệu

Các API đi qua gateway, dùng bearer JWT. Ngày điểm danh tính theo `Asia/Ho_Chi_Minh`.
Response dùng `ApiResponse` (`code: 1000`, `result`).

## Người dùng

- `POST /api/users/me/challenges/7-day`: bắt đầu lượt tham gia. Khi một lượt còn `IN_PROGRESS`, đăng ký lại trả `409/1032`.
- `POST /api/users/me/challenges/7-day/check-ins`: điểm danh ngày hiện tại. Điểm danh lặp trả `409/1035`; nếu đã lỡ ngày trước, trả tiến độ `FAILED` và không cộng điểm.
- `GET /api/users/me/challenges/7-day`: tiến độ lượt gần nhất. Chưa từng tham gia trả `404/1033`.
- `GET /api/users/me/badges`: chỉ huy hiệu còn hiệu lực.
- `GET /api/users/me`: trường `badges` chứa danh sách huy hiệu còn hiệu lực để hiển thị trên profile.

Tiến độ gồm `startedOn`, `completedDays`, `totalDays: 7`, `points`, `status`
(`IN_PROGRESS`, `COMPLETED`, `FAILED`) và `checkedInToday`. Mỗi ngày hoàn thành được 10 điểm,
tối đa 70. Bỏ lỡ một ngày làm lượt tham gia `FAILED`; đủ 7/7 ngày thì `COMPLETED` và được cấp
huy hiệu `ACHIEVEMENT` nếu chưa có huy hiệu đó còn hiệu lực.

## Quản trị viên

- `POST /api/admin/users/{id}/badges/verification`: cấp huy hiệu `VERIFIED` thủ công; gọi lại khi huy hiệu còn hiệu lực trả cùng huy hiệu.
- `POST /api/admin/users/{id}/badges/{badgeId}/revoke` với `{"reason":"..."}`: thu hồi huy hiệu `VERIFIED` hoặc `ACHIEVEMENT`; lý do bắt buộc, tối đa 500 ký tự.
- `GET /api/admin/users/{id}/badges`: lịch sử cấp và thu hồi, bao gồm `revokedAt` và `revocationReason`.

Thu hồi không xóa bản ghi. Người dùng có thể tham gia một lượt mới sau khi lượt trước hoàn thành
hoặc thất bại; chỉ một lượt được tham gia tại một thời điểm.
