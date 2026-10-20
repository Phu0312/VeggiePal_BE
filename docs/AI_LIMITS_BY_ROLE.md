# Bảng Quy định Giới hạn AI theo Từng Role (AI Guardrails & Limits by Role)

> **Dự án:** VeggiePal Microservices Backend  
> **Service phụ trách:** `ai-service` (Port `8086`)  
> **Phiên bản:** `1.0.0` (Căn cứ trên cấu hình mã nguồn thực tế)  
> **Ngày cập nhật:** 16/10/2026  

---

## Mục lục

1. [Tổng quan về Hệ thống Giới hạn AI (AI Guardrails System)](#1-tổng-quan-về-hệ-thống-giới-hạn-ai-ai-guardrails-system)
2. [Bảng Ma trận So sánh Giới hạn AI giữa các Role](#2-bảng-ma-trận-so-sánh-giới-hạn-ai-giữa-các-role)
3. [Chi tiết Giới hạn cho Role: GUEST (Khách vãng lai)](#3-chi-tiết-giới-hạn-cho-role-guest-khách-vãng-lai)
4. [Chi tiết Giới hạn cho Role: MEMBER (Thành viên đã đăng nhập)](#4-chi-tiết-giới-hạn-cho-role-member-thành-viên-đã-đăng-nhập)
5. [Chi tiết Giới hạn & Đặc quyền cho Role: ADMIN (Quản trị viên)](#5-chi-tiết-giới-hạn--đặc-quyền-cho-role-admin-quản-trị-viên)
6. [Chi tiết Giới hạn cho INTERNAL / SYSTEM (Kiểm duyệt & Tự động hóa)](#6-chi-tiết-giới-hạn-cho-internal--system-kiểm-duyệt--tự-động-hóa)
7. [Bảng Biến Môi trường & Giá trị Mặc định (Configuration Properties)](#7-bảng-biến-môi-trường--giá-trị-mặc-định-configuration-properties)
8. [Quy tắc Xử lý Ngoại lệ & Mã Lỗi khi Vượt Ngưỡng](#8-quy-tắc-xử-lý-ngoại-lệ--mã-lỗi-khi-vượt-ngưỡng)

---

## 1. Tổng quan về Hệ thống Giới hạn AI (AI Guardrails System)

Trong VeggiePal Backend, **AI Service** (`:8086`) tích hợp trực tiếp với nền tảng Groq Cloud (các mô hình LLM mã nguồn mở tốc độ cao như `openai/gpt-oss-20b` và Speech-to-Text `whisper-large-v3-turbo`). Để đảm bảo:
- **Ngăn chặn lạm dụng và cạn kiệt tài nguyên (Abuse Prevention):** Tránh hiện tượng spam bot, gọi API hàng loạt làm cạn hạn ngạch API Key Groq.
- **Tối ưu hóa chi phí và hiệu năng (Cost & Latency Control):** Kiểm soát độ dài token prompt/completion và thời gian xử lý.
- **Bảo mật và an toàn dữ liệu (Safety & Privacy):** Phân định ranh giới rõ ràng giữa người dùng chưa đăng nhập, thành viên và quản trị viên; không để AI bịa đặt dữ liệu dinh dưỡng (chống hallucination).

Hệ thống quản lý định mức sử dụng thông qua bảng cơ sở dữ liệu [`ai_daily_quotas`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/db/migration/V1__create_ai_schema.sql#L26-L37) và [`ai_guest_sessions`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/db/migration/V2__secure_guest_sessions.sql#L1-L10) với khóa định danh duy nhất (`principal_key`), kiểm soát độc lập theo từng ngày (`usage_date`).

---

## 2. Bảng Ma trận So sánh Giới hạn AI giữa các Role

| Tiêu chí / Giới hạn | GUEST (Khách vãng lai) | MEMBER (Thành viên - `USER`) | ADMIN (Quản trị viên) | INTERNAL / SYSTEM |
|---|:---:|:---:|:---:|:---:|
| **Cơ chế Định danh (Principal Key)** | `guest-ip:{ipHash}` | `user:{userId}` (từ JWT claim) | `user:{adminId}` (từ JWT claim) | Token tác giả hoặc Admin |
| **Yêu cầu Xác thực (Auth Scheme)** | Không (Public + Session UUID) | Bearer JWT (`ROLE_USER`) | Bearer JWT (`ROLE_ADMIN`) | Bearer Token nội bộ |
| **Số lượt Chatbot / Ngày (Daily Requests)** | **3 lượt** / IP hash / ngày | **50 lượt** / ngày | **50 lượt** / ngày (khi dùng chat) | *Không áp dụng* |
| **Ngân sách Token / Ngày (Token Budget)** | *Theo lượt chat* | **50,000 tokens** / ngày | **50,000 tokens** / ngày | *Không áp dụng* |
| **Số Session tối đa / IP / Ngày** | **3 sessions** / ngày / IP | *Không giới hạn* (Dựa vào User) | *Không giới hạn* | *Không giới hạn* |
| **Thời hạn sống của Session (TTL)** | **24 giờ** | Theo hạn JWT (24 giờ) | Theo hạn JWT (24 giờ) | Theo phiên gọi API |
| **Độ dài câu hỏi tối đa (Max Input)** | **4,000 ký tự** | **4,000 ký tự** | **4,000 ký tự** | Toàn bộ nội dung Blog/Comment |
| **Bộ nhớ Ngữ cảnh Chat (Sliding Window)** | **0 tin nhắn** (Stateless, 1 lượt) | **12 tin nhắn gần nhất** | **12 tin nhắn gần nhất** | *Không áp dụng* |
| **Lưu lịch sử Hội thoại (Persistence)** | ❌ Không lưu | ✅ Lưu vĩnh viễn trong MySQL | ✅ Lưu vĩnh viễn trong MySQL | *Không áp dụng* |
| **Sinh thực đơn 7 ngày (Meal Planning)** | ❌ Bị chặn (401 Unauthorized) | ✅ Không giới hạn số kế hoạch | ✅ Toàn quyền | *Không áp dụng* |
| **Trích xuất công thức Video (Video Job)** | ❌ Bị chặn (401 Unauthorized) | ✅ File $\le$ 100MB, $\le$ 30 phút | ✅ File $\le$ 100MB, $\le$ 30 phút | *Không áp dụng* |
| **Gợi ý nội dung (AI Recommendation)** | ❌ Bị chặn (401 Unauthorized) | ✅ Tối đa 10 gợi ý (def: 6) | ✅ Toàn quyền | *Không áp dụng* |
| **Duyệt bài kiểm duyệt (Admin Review)** | ❌ Bị chặn (403 Forbidden) | ❌ Bị chặn (403 Forbidden) | ✅ Xem & duyệt `REVIEW_REQUIRED` | Tự động phân loại qua AI |
| **Xem thống kê & Nhật ký (AI Monitoring)** | ❌ Bị chặn (403 Forbidden) | ❌ Bị chặn (403 Forbidden) | ✅ Thống kê 1-90 ngày, Log 100 | *Không áp dụng* |

---

## 3. Chi tiết Giới hạn cho Role: GUEST (Khách vãng lai)

Actor **Guest** là đối tượng người dùng chưa tạo tài khoản hoặc chưa đăng nhập vào VeggiePal, muốn dùng thử tính năng tư vấn dinh dưỡng chay của AI.

### 3.1. Giới hạn Khởi tạo Phiên (Guest Session)
- **Điểm bắt đầu:** `POST /ai/chat/guest-sessions` (tiếp nhận IP từ client request).
- **Mã hóa bảo vệ địa chỉ IP:** IP của khách không lưu trực tiếp mà được băm bằng SHA-256 kết hợp chuỗi Pepper bí mật (`ai.guest.session-pepper`):
  $$\text{IP Hash} = \text{SHA256}(\text{remoteAddress} + \text{":"} + \text{pepper})$$
- **Giới hạn số phiên trên mỗi IP:** Tối đa **3 phiên / IP / ngày** ([`ai.guest.max-sessions-per-ip-per-day = 3`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L42)). Vượt quá sẽ bị chặn với mã lỗi `6003 QUOTA_EXCEEDED`.
- **Thời gian hết hạn của phiên (TTL):** Phiên tồn tại đúng **24 giờ** kể từ thời điểm tạo ([`ai.guest.session-ttl = 24h`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L41)).

### 3.2. Giới hạn Tương tác Chatbot Dùng thử
- **Điểm bắt đầu:** `POST /ai/chat/guest` (kèm `guestSession` và `message`).
- **Xác thực phiên:** Hệ thống đối chiếu `sessionId` trong bảng `ai_guest_sessions`, kiểm tra thời gian hết hạn (`expiresAt > now()`) và khớp băm IP hiện tại. Nếu sai lệch hoặc giả mạo IP sẽ trả về lỗi `6001 UNAUTHENTICATED`.
- **Định mức câu hỏi:** Tối đa **3 câu hỏi / ngày** cho mỗi IP hash, dù tạo thêm session ([`ai.chat.guest-daily-limit = 3`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L38)).
- **Độ dài câu hỏi:** Tối đa **4,000 ký tự** (`@Size(max=4000)`).
- **Định dạng Session ID:** Bắt buộc tuân theo biểu thức chính quy Regex: `^[A-Za-z0-9_-]{16,100}$`.

### 3.3. Các Tính năng AI BỊ KHÓA đối với Guest
Guest hoàn toàn không có quyền gọi các endpoint sau (sẽ bị Spring Security chặn với lỗi `401 Unauthorized`):
1. ❌ **Không có lịch sử hội thoại:** Không thể tạo cuộc hội thoại (`conversationId`), không thể xem lại các tin nhắn đã chat trước đó. Mỗi lượt chat của Guest là độc lập.
2. ❌ **Không thể Lập thực đơn (Meal Planning):** Endpoint `POST /ai/meal-plans` yêu cầu thông tin dị ứng và hồ sơ sức khỏe trong JWT.
3. ❌ **Không thể Xử lý Video (Video Processing):** Endpoint `POST /ai/videos/process` bị khóa.
4. ❌ **Không thể Nhận Gợi ý (AI Recommendation):** Endpoint `GET /ai/recommendations` bị khóa.
5. ❌ **Không có quyền Quản trị (Admin APIs):** Bị chặn với lỗi `403 Forbidden`.

---

## 4. Chi tiết Giới hạn cho Role: MEMBER (Thành viên đã đăng nhập)

Actor **Member** là người dùng đã xác thực danh tính qua Bearer JWT (HS256) chứa claim `role: "USER"` hoặc `"ADMIN"`. Định danh kiểm soát được gắn trực tiếp theo ID tài khoản: `principal = "user:{userId}"`.

### 4.1. Giới hạn Chatbot Dinh dưỡng Đa lượt (`/ai/chat`)
- **Số lượt tương tác:** Tối đa **50 requests / ngày** ([`ai.chat.member-daily-limit = 50`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L39)).
- **Ngân sách Token tích lũy trong ngày:** Tối đa **50,000 tokens / ngày** ([`ai.token.daily-limit = 50000`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L45)). Token được tính bằng tổng token prompt gửi đi cộng với token completion nhận về từ Groq:
  $$\text{Total Tokens} = \sum \text{Prompt Tokens} + \sum \text{Completion Tokens} \le 50,000$$
- **Cơ chế Cắt trượt Ngữ cảnh (Sliding Window):** Để tránh làm phình kích thước token và chi phí API, hệ thống chỉ giữ lại **tối đa 12 tin nhắn gần nhất** (`ai.chat.max-history-messages = 12`) trong cuộc hội thoại để đưa vào context prompt gửi cho LLM.
- **Độ dài câu hỏi tối đa:** **4,000 ký tự / request** ([`ai.chat.max-input-characters = 4000`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L44)).
- **Tiêu đề cuộc hội thoại:** Tự động cắt lấy **80 ký tự đầu tiên** của tin nhắn mở đầu để đặt làm tên cuộc hội thoại.
- **Tham số sinh văn bản của LLM:**
  - Mô hình: `openai/gpt-oss-20b` (hoặc cấu hình qua `GROQ_CHAT_MODEL`).
  - `temperature`: `0.4` (cân bằng giữa tính sáng tạo và độ chính xác chuyên môn).
  - `max_tokens`: `700 tokens` cho mỗi câu trả lời.

### 4.2. Giới hạn Lập Thực đơn 7 Ngày (`/ai/meal-plans`)
- **Phạm vi dữ liệu đầu vào:** AI Service chỉ đọc tối đa **100 công thức nấu ăn** (`size=100`) từ Recipe Service để làm tập ứng viên; tuyệt đối không để AI tự tạo ra món ăn hoặc ID không có trong cơ sở dữ liệu.
- **Ràng buộc cấu trúc thực đơn:**
  - Bắt buộc đúng **7 ngày** liên tục: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`.
  - Mỗi ngày bắt buộc đủ đúng **3 bữa**: `BREAKFAST`, `LUNCH`, `DINNER`.
- **Ràng buộc an toàn thực phẩm (Fail-Closed Safety):**
  - Món ăn bắt buộc phải có thuộc tính `isVegan = true`.
  - Bất kỳ công thức nào có chứa nguyên liệu trùng với danh sách dị ứng của người dùng ([`UserAllergy`](file:///d:/project/VeggiePal/VeganApp_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/entity/UserAllergy.java)) hoặc có cờ `isAllergen = true` đều bị hệ thống loại bỏ khỏi tập ứng viên trước khi gửi cho AI.
- **Ràng buộc đổi món (`POST /ai/meal-plans/{id}/replace`):** Món mới được chọn bắt buộc phải khác với món hiện tại (`replacementId != currentRecipeId`), và phải vượt qua bộ lọc dị ứng như lúc tạo ban đầu.
- **Tham số sinh văn bản của LLM:**
  - `temperature`: `0.2` (giảm độ ngẫu nhiên để tuân thủ định dạng JSON).
  - `max_tokens`: `1,500 tokens`.

### 4.3. Giới hạn Xử lý & Trích xuất Video Công thức (`/ai/videos`)
- **Dung lượng file tải lên:** Tối đa **100MB** (`104,857,600 bytes`). AI Service sở hữu cả `ai.video.max-file-bytes` và giới hạn multipart `100MB/105MB`; Gateway stream body và không parse multipart. Vượt quá sẽ trả về lỗi `6009 FILE_TOO_LARGE`.
- **Thời lượng video tối đa:** Tối đa **1,800 giây (30 phút)** ([`ai.video.max-duration-seconds = 1800`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L49)), được kiểm tra nghiêm ngặt qua công cụ dòng lệnh `ffprobe`.
- **Định dạng file:** Bắt buộc có MIME-type bắt đầu bằng `video/`.
- **Thời gian chờ tiến trình (Timeouts):**
  - `ffprobe` đo thời lượng: Timeout sau **20 giây**.
  - `ffmpeg` tách audio mono 16kHz MP3: Timeout sau **120 giây**.
- **Mô hình phiên âm:** Groq Whisper `whisper-large-v3-turbo`.
- **Mô hình trích xuất:** `openai/gpt-oss-20b`, `temperature = 0.1`, `max_tokens = 1,200`.

### 4.4. Giới hạn Gợi ý Nội dung (`/ai/recommendations`)
- **Số lượng gợi ý:** Tham số `limit` nhận giá trị từ **1 đến tối đa 10** (giá trị mặc định: **6 mục**).
- **Tập dữ liệu quét:** Chỉ quét tối đa **20 bài blog** và **20 công thức mới nhất** từ Blog Service và Recipe Service để xếp hạng.

---

## 5. Chi tiết Giới hạn & Đặc quyền cho Role: ADMIN (Quản trị viên)

Actor **Admin** sở hữu toàn bộ quyền hạn của Member, đồng thời có các đặc quyền can thiệp kiểm duyệt và theo dõi hệ thống.

### 5.1. Giới hạn & Quy tắc trong Duyệt Kiểm duyệt Nội dung (Admin Moderation)
- **Truy cập hàng đợi duyệt:** `GET /ai/admin/moderation` chỉ hiển thị các nội dung mà mô hình AI phân loại là `REVIEW_REQUIRED`. Các bài viết đã được AI tự động `APPROVED` hoặc `REJECTED` sẽ không xuất hiện trong hàng đợi này.
- **Ràng buộc khi ra quyết định (`PUT /ai/admin/moderation/{id}`):**
  - Admin **chỉ được phép chọn** một trong hai quyết định: `APPROVED` hoặc `REJECTED`.
  - Nếu Admin gửi lại quyết định `REVIEW_REQUIRED`, hệ thống sẽ từ chối và ném mã lỗi `6000 INVALID_REQUEST`.
  - Quyết định này sẽ được gửi đồng bộ kèm Bearer Token của Admin sang endpoint nội bộ của Blog Service: `PUT /internal/moderation/{type}/{id}` để cập nhật trạng thái bài viết/bình luận.

### 5.2. Giới hạn trong Giám sát Hoạt động AI (AI Monitoring)
- **Thống kê tổng hợp (`GET /ai/admin/monitoring/summary`):**
  - Tham số khoảng thời gian `days` được giới hạn chặt chẽ: **tối thiểu 1 ngày và tối đa 90 ngày** (`Math.max(1, Math.min(days, 90))`, mặc định: 7 ngày).
- **Xem nhật ký sử dụng (`GET /ai/admin/monitoring/logs`):**
  - Cố định trả về tối đa **100 bản ghi mới nhất** (`findTop100ByOrderByCreatedAtDesc`).
- **Tìm kiếm nhật ký chi tiết (`GET /ai/admin/monitoring/logs/search`):**
  - Kích thước trang `size` được giới hạn từ **1 đến tối đa 100 bản ghi / trang** (`Math.max(1, Math.min(size, 100))`).
- **Xem danh sách Models & Limits (`/models`, `/limits`):**
  - Chỉ cho phép xem ở chế độ **Read-only**. Không hỗ trợ thay đổi cấu hình runtime qua REST API (muốn đổi phải cấu hình lại biến môi trường hoặc file properties).

---

## 6. Chi tiết Giới hạn cho INTERNAL / SYSTEM (Kiểm duyệt & Tự động hóa)

Các tiến trình tự động giữa các microservice (tiêu biểu là Blog Service gọi sang AI Service để kiểm duyệt nội dung) có các quy chuẩn giới hạn khắt khe:

### 6.1. Ngưỡng Phân loại Tự động (Confidence Threshold)
Khi Blog Service gọi `POST /ai/moderation/check`:
- Hệ thống gửi prompt kiểm duyệt tới Groq LLM với các nhóm vi phạm: `hate`, `harassment`, `sexual`, `violence`, `self_harm`, `illegal`, `spam`, `misinformation_dangerous`.
- **Quy tắc an toàn mức cao (High Confidence Rule):**
  - Chỉ được phép tự động gán nhãn `REJECTED` khi AI phát hiện vi phạm rõ ràng và **độ tin cậy đạt từ 85% trở lên** (`confidence >= 0.85`).
  - Nếu AI đề xuất `REJECTED` nhưng `confidence < 0.85`, hệ thống tự động hạ bậc (downgrade) quyết định xuống `REVIEW_REQUIRED` để chuyển cho Admin duyệt thủ công, tránh hiện tượng chặn nhầm bài viết của người dùng.
- **Ràng buộc tham số LLM:**
  - `temperature`: `0.0` (tuyệt đối không ngẫu nhiên, kết quả mang tính xác thực cao).
  - `max_tokens`: `350 tokens`.

### 6.2. Giới hạn Kết nối & Khả năng Chịu lỗi (Resilience & Retries)
- **Timeout gọi sang Groq API:**
  - Thời gian kết nối (`connectTimeout`): **5 giây**.
  - Thời gian chờ đọc dữ liệu (`readTimeout`): **45 giây**.
- **Cơ chế Thử lại (Retry Policy):**
  - Số lần thử lại tối đa: **2 lần** ([`groq.api.max-retries = 2`](file:///d:/project/VeggiePal/VeganApp_BE/ai-service/src/main/resources/application.properties#L9)).
  - Điều kiện kích hoạt Retry: Khi Groq trả về mã lỗi `429 Too Many Requests` (chạm rate limit của Groq) hoặc các lỗi máy chủ `5xx Server Error`.
  - Thuật toán chờ lùi theo hàm mũ (Exponential Backoff):  
    $$\text{Sleep Time} = 100 \times 2^{\text{attempt}} \text{ (ms)}$$
    *(Lần 1: 100ms, Lần 2: 200ms)*.

---

## 7. Bảng Biến Môi trường & Giá trị Mặc định (Configuration Properties)

Toàn bộ các ngưỡng giới hạn trên có thể tùy biến khi triển khai hệ thống thông qua biến môi trường (Environment Variables) trong file `.env`:

| Tên Biến Môi trường (.env) | Tên Property trong Spring | Giá trị Mặc định | Ý nghĩa & Phạm vi áp dụng |
|---|---|:---:|---|
| `AI_GUEST_DAILY_LIMIT` | `ai.chat.guest-daily-limit` | `3` | Số lượt chat tối đa trong ngày của một IP hash, dùng chung giữa các session. |
| `AI_MEMBER_DAILY_LIMIT` | `ai.chat.member-daily-limit` | `50` | Số lượt chat tối đa trong ngày của mỗi Member (`user:{id}`). |
| `AI_DAILY_TOKEN_LIMIT` | `ai.token.daily-limit` | `50000` | Giới hạn tổng số tokens (prompt + completion) / ngày / user. |
| `AI_GUEST_MAX_SESSIONS_PER_IP_PER_DAY` | `ai.guest.max-sessions-per-ip-per-day` | `3` | Số lượng phiên chat dùng thử tối đa cho 1 địa chỉ IP / ngày. |
| `AI_GUEST_SESSION_TTL` | `ai.guest.session-ttl` | `24h` | Thời hạn sống của một phiên Guest (hết hạn phải tạo mới). |
| `AI_CHAT_MAX_HISTORY` | `ai.chat.max-history-messages` | `12` | Số tin nhắn lịch sử tối đa đưa vào ngữ cảnh hội thoại. |
| `AI_CHAT_MAX_INPUT_CHARS` | `ai.chat.max-input-characters` | `4000` | Số ký tự tối đa cho mỗi câu hỏi của người dùng. |
| `AI_VIDEO_MAX_BYTES` | `ai.video.max-file-bytes` | `104857600` | Dung lượng tối đa của file video upload (tương đương 100MB). |
| `AI_VIDEO_MAX_DURATION_SECONDS` | `ai.video.max-duration-seconds` | `1800` | Thời lượng tối đa của video công thức (tương đương 30 phút). |
| `GROQ_CONNECT_TIMEOUT` | `groq.api.connect-timeout` | `5s` | Timeout kết nối tới máy chủ Groq API. |
| `GROQ_READ_TIMEOUT` | `groq.api.read-timeout` | `45s` | Timeout đọc kết quả từ Groq API. |
| `GROQ_MAX_RETRIES` | `groq.api.max-retries` | `2` | Số lần thử lại tối đa khi Groq bị lỗi 429 hoặc 5xx. |

---

## 8. Quy tắc Xử lý Ngoại lệ & Mã Lỗi khi Vượt Ngưỡng

Khi người dùng vi phạm hoặc vượt qua các ngưỡng giới hạn quy định, hệ thống sẽ trả về mã lỗi chuẩn trong envelope `ApiResponse`:

```json
{
  "code": 6003,
  "message": "AI daily quota exceeded"
}
```

### Bảng Đối chiếu Mã Lỗi & HTTP Status Code:

| Mã Lỗi (Code) | Tên Enum Lỗi | HTTP Status Code | Nguyên nhân kích hoạt |
|:---:|---|:---:|---|
| **`6000`** | `INVALID_REQUEST` | `400 Bad Request` | Câu hỏi vượt 4,000 ký tự; UUID session sai định dạng; Admin gửi quyết định `REVIEW_REQUIRED`; Mục tiêu calo âm. |
| **`6001`** | `UNAUTHENTICATED` | `401 Unauthorized` | Không có Bearer token; Guest session không tồn tại, hết hạn 24h hoặc IP không khớp với IP tạo phiên. |
| **`6002`** | `RESOURCE_NOT_FOUND` | `404 Not Found` | Không tìm thấy cuộc hội thoại (`conversationId`), thực đơn (`mealPlanId`) hoặc không thuộc sở hữu của user. |
| **`6003`** | `QUOTA_EXCEEDED` | `429 Too Many Requests` | Vượt quá 3 sessions/ngày của IP; Vượt quá 3 câu chat của Guest; Vượt quá 50 câu chat của Member; Vượt quá 50,000 tokens/ngày. |
| **`6004`** | `GROQ_UNAVAILABLE` | `503 Service Unavailable` | Groq API gặp sự cố hoặc timeout sau khi đã retry 2 lần. |
| **`6005`** | `GROQ_NOT_CONFIGURED` | `503 Service Unavailable` | Máy chủ chưa cấu hình `GROQ_API_KEY` trong file môi trường `.env`. |
| **`6006`** | `DOWNSTREAM_UNAVAILABLE` | `503 Service Unavailable` | Blog Service, Recipe Service hoặc Nutrition Service không phản hồi khi AI cần lấy dữ liệu thật. |
| **`6007`** | `INVALID_AI_RESPONSE` | `502 Bad Gateway` | Mô hình LLM trả về cấu trúc JSON sai quy chuẩn (ví dụ thiếu bữa trong ngày hoặc sai định dạng trích xuất). |
| **`6008`** | `VIDEO_PROCESSING_FAILED` | `422 Unprocessable Entity` | Video dài hơn 30 phút, file video hỏng hoặc tiến trình FFmpeg/FFprobe bị lỗi. |
| **`6009`** | `FILE_TOO_LARGE` | `413 Payload Too Large` | File video tải lên vượt quá giới hạn 100MB. |
