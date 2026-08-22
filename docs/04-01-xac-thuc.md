> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Mô hình dữ liệu](./03-mo-hinh-du-lieu.md) · [Mục lục](./README.md) · [Module hạ tầng →](./04-02-ha-tang.md)

# 6. Thiết kế từng module chức năng

Mỗi module: actors, user stories, quy tắc, trạng thái, màn hình/URL, service, edge cases.

Các module khác: [6.2 Hạ tầng](./04-02-ha-tang.md) · [6.3 Phân bổ](./04-03-phan-bo.md) · [6.4 Hợp đồng](./04-04-hop-dong.md) · [6.5 Điện nước](./04-05-dien-nuoc.md) · [6.6 Sự cố](./04-06-su-co-vi-pham.md) · [6.7 Dashboard](./04-07-dashboard.md)

---

## 6.1. Module 1 — Xác thực & Phân quyền

**Actors:** Guest, Student, Staff, Admin.

**User stories:**

- Là sinh viên, tôi đăng ký tài khoản bằng MSSV + email + mật khẩu để sau này nộp đơn ở KTX.
- Là sinh viên, tôi đăng nhập bằng **MSSV hoặc email**.
- Là BQL, tôi đăng nhập tài khoản quản trị (không tự đăng ký).
- Là mọi user đã login, tôi đổi mật khẩu và cập nhật SĐT / người thân khẩn cấp.
- Là admin, tôi khóa tài khoản hoặc reset mật khẩu tạm.

**Quy tắc nghiệp vụ:**

- Đăng ký chỉ tạo `ROLE_STUDENT`. MSSV 8–12 ký tự `[A-Za-z0-9]`. Email hợp lệ, unique.
- `gender` bắt buộc lúc đăng ký (không cho đổi tự do sau khi đã có hợp đồng **OCCUPYING** — phải admin).
- Mật khẩu ≥ 8 ký tự, BCrypt strength 10.
- Session: cookie `JSESSIONID`, timeout **30 phút**; remember-me **không bật** mặc định (tránh máy phòng máy).
- CSRF bật toàn cục (Thymeleaf `_csrf`).
- Brute-force đơn giản: sau 5 lần sai / 10 phút thì tạm khóa IP+username trong bộ nhớ app (`LoginAttemptService`) — đủ cho BTL, không cần Redis.
- Đổi mật khẩu: yêu cầu mật khẩu cũ.
- Profile sinh viên: được sửa `phone`, `emergency_*`, `hometown`. Không sửa `student_code`, `priority_category`, `previous_stay_good`, `conduct_score`.

**Trạng thái tài khoản:** `enabled=true/false`. Không state machine phức tạp.

**Màn hình / URL:**

| Method   | Path                                   | Role         | View                       |
| -------- | -------------------------------------- | ------------ | -------------------------- |
| GET/POST | `/login`                             | anon         | `auth/login.html`        |
| GET/POST | `/register`                          | anon         | `auth/register.html`     |
| POST     | `/logout`                            | auth         | —                         |
| GET      | `/`                                  | auth         | redirect theo role         |
| GET      | `/admin/dashboard`                   | ADMIN        | `admin/dashboard.html`   |
| GET      | `/staff/dashboard`                   | ADMIN, STAFF | `staff/dashboard.html`   |
| GET      | `/student/dashboard`                 | STUDENT      | `student/dashboard.html` |
| GET/POST | `/student/profile`                   | STUDENT      | `student/profile.html`   |
| GET/POST | `/student/password`                  | STUDENT      | `student/password.html`  |
| GET/POST | `/admin/profile`, `/staff/profile` | ADMIN/STAFF  | tương tự                |
| CRUD     | `/admin/users`                       | ADMIN        | `admin/users/*.html`     |
| GET      | `/admin/students?stay=all\|occupying\|vacant` | ADMIN | `admin/students/list.html` |

**Service:** `AuthService`, `KtxUserDetailsService.loadUserByUsername` tìm theo username **hoặc** email (`findByUsernameOrEmail`). `LoginSuccessHandler` điều hướng:

- ADMIN → `/admin/dashboard`
- STAFF → `/staff/dashboard`
- STUDENT → `/student/dashboard`

**Edge cases:**

- Trùng MSSV/email → 400 field error, không lộ “email đã thuộc role khác”.
- Sinh viên `blocked_from_housing=true` vẫn login nhưng không nộp đơn mới.
- User `enabled=false` → `DisabledException`, flash “Tài khoản bị khóa”.
- STAFF vào `/admin/**` → 403 trang `error/403.html`.
- ADMIN vào `/staff/**` được phép (vận hành thay staff) và không bị lọc tòa.

---
