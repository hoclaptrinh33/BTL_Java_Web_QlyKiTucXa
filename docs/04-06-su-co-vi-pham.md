> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module điện nước](./04-05-dien-nuoc.md) · [Mục lục](./README.md) · [Module dashboard →](./04-07-dashboard.md)

# 6.6. Module 6 — Sự cố & vi phạm

**Ticket:** SV tạo mô tả hỏng thiết bị; staff/admin chuyển `OPEN → IN_PROGRESS → RESOLVED`; SV xác nhận `CLOSED`. `TicketAutoCloseJob` (`@Scheduled` 08:00, cùng cụm nightly với nhắc HĐ): `RESOLVED` quá `ticket.autoclose.days` (mặc định 7) → `CLOSED`. Reject nếu spam/sai phòng.

**Vi phạm:** chỉ ADMIN/STAFF. Trừ `conduct_score` không âm. Điểm mặc định (form prefill, staff sửa được; thiếu combo → theo cột severity):

| violation_type      | severity   | `points_deducted` | action gợi ý                  |
| ------------------- | ---------- | ------------------- | ------------------------------- |
| `LATE_RETURN`     | `MINOR`  | 5                   | `WARNING` / `POINT_DEDUCT`  |
| `ILLEGAL_COOKING` | `MAJOR`  | 20                  | `POINT_DEDUCT`                |
| `DISTURBANCE`     | `MAJOR`  | 25                  | `POINT_DEDUCT`                |
| `DAMAGE`          | `SEVERE` | 50                  | `TERMINATE` nếu điểm về 0 |

Fallback theo severity nếu không có dòng trên: `MINOR=5`, `MAJOR=20`, `SEVERE=50`.

Ngưỡng:

- Đầu kỳ: 100 (`conduct.initial = 100`)
- `< 50`: cảnh cáo (notification + cờ UI)
- `0` hoặc `severity=SEVERE` + `action=TERMINATE`: `blocked_from_housing=true`, gợi ý terminate HĐ (`ACTIVE → TERMINATED`, giường giữ đến checkout)

Điểm **không tự cộng** trong kỳ. Reset 100 khi admin bấm “Reset điểm đợt mới”.

**URL:** `/student/tickets`, `/staff/tickets`, `/admin/tickets`, `/admin/violations`, `/student/violations` (read).

**Edge cases:** SV tạo ticket phòng không phải phòng mình → 403; trừ điểm > điểm hiện có → về 0 + cảnh báo terminate.

---
