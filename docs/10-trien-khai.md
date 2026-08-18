> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Quyết định](./09-quyet-dinh-va-rui-ro.md) · [Mục lục](./README.md) · [Phụ lục →](./11-phu-luc.md)

# 16. Kế hoạch triển khai / rollout (BTL milestones)

Không dùng feature flag sản phẩm. Dùng **nhánh + milestone demo**:

| Mốc | Tuần (gợi ý) | Có thể demo                                                             |
| ---- | --------------- | ------------------------------------------------------------------------- |
| M0   | 0.5             | Repo Maven (JDK 25, Boot 4.1), MySQL 8.4, login admin seed                |
| M1   | 1–2            | CRUD tòa/phòng/giường/tài sản + phân quyền URL                    |
| M2   | 2–3            | Đăng ký SV, profile, đợt, nộp đơn                                 |
| M3   | 3–5            | **Allocation engine + preview/commit + gán tay** (mốc ăn điểm) |
| M4   | 5–6            | Hợp đồng, check-in/out, chuyển/trả phòng                            |
| M5   | 6–7            | Reading + billing + thanh toán                                           |
| M6   | 7–8            | Ticket, vi phạm, điểm rèn luyện                                      |
| M7   | 8               | Dashboard Chart.js, Excel/PDF, job nhắc HĐ                              |
| M8   | 8–9            | Seeder demo, README chạy máy giảng viên, chốt bug                    |

Rollback BTL: revert PR Git; Flyway `V{n}` lỗi thì không migrate tiếp, không `repair` bừa. Backup `mysqldump` trước buổi demo lớn.

Cấu hình mặc định in trên README; giảng viên đổi giá trong `/admin/configs`.

---

# 17. Câu hỏi mở (Open Questions)

Chủ đồ án đã trả lời ngày **2026-08-18**. Các mục **RESOLVED** là quyết định cuối, không mở lại. Các mục còn lại: **RESOLVED-WITH-DEFAULT** — giữ giá trị đã viết trong đặc tả.

1. **Giá từng loại phòng (seeder)?** 8 giường 1.200.000; 6 giường 1.800.000; 4 giường 2.400.000; VIP 4.000.000 đ/kỳ. **Trạng thái: giữ mặc định đặc tả.**
2. **Công thức cọc?** **RESOLVED:** 50% giá kỳ. `deposit_amount = HALF_UP(price_per_term * 0.5)`. Ví dụ 1.200.000 → 600.000 đ. Key `contract.deposit.ratio=0.5` (ghi đè mặc định cũ “1 tháng = price/5”).
3. **Giá điện/nước?** Số liệu mẫu [mục 6.5.1](./04-05-dien-nuoc.md). **Trạng thái: giữ mặc định đặc tả.**
4. **Phụ phí / gửi xe?** Mọi người 30.000 đ/tháng. **Trạng thái: giữ mặc định đặc tả.**
5. **SOFT hay STRICT?** **RESOLVED:** `SOFT` (`alloc.preference.mode`).
6. **`previous_stay_good` tân SV?** `false`. **Trạng thái: giữ mặc định đặc tả.**
7. **Minh chứng ưu tiên?** Admin tick, không upload file. **Trạng thái: giữ mặc định đặc tả.**
8. **Số kỳ tối đa?** Không giới hạn v1. **Trạng thái: giữ mặc định đặc tả.**
9. **Hai HĐ hè / năm học?** Không — một HĐ OCCUPYING; gia hạn tại chỗ. Không bắt checkout giữa hè và năm học. **Trạng thái: giữ mặc định đặc tả** (phần “không bắt checkout”).
10. **Phòng MIXED?** Ngoài v1. **Trạng thái: giữ mặc định đặc tả.**
11. **Reset điểm rèn luyện?** Bấm tay. **Trạng thái: giữ mặc định đặc tả.**
12. **Ngưỡng buộc rời?** 0 điểm **hoặc** một vi phạm SEVERE. **Trạng thái: giữ mặc định đặc tả.**
13. **SMTP?** Tắt mail (`ktx.mail.enabled=false`). **Trạng thái: giữ mặc định đặc tả.**
14. **Quy mô seeder?** 3–5 tòa / lab như mục 2. **Trạng thái: giữ mặc định đặc tả.**
15. **Định dạng MSSV?** **RESOLVED:** 8–12 ký tự alphanumeric, vd `D22CQCN001` (`[A-Za-z0-9]{8,12}`).
16. **Tiền phòng theo tháng hay theo kỳ?** **RESOLVED:** theo kỳ lúc check-in. `billing.room.split_monthly=false`; v1 không tách tháng.
17. **Staff commit allocation?** **RESOLVED:** không. Chỉ ADMIN preview / commit / `assignManual`.
18. **Hoàn cọc?** Trừ nợ + hư hỏng, phần còn `REFUNDED` ghi tay. **Trạng thái: giữ mặc định đặc tả.**

---

# 18. Ước lượng tải, SLA, lưu trữ

| Đại lượng                       | Ước lượng                                                                 |
| ----------------------------------- | ----------------------------------------------------------------------------- |
| Sinh viên nội trú                | 1.000–3.000                                                                  |
| Phòng / giường                   | 300 / ~1.800                                                                  |
| Đơn / đợt                       | ≤ 2.000                                                                      |
| Thời gian`plan()`                | < 5 s (local, 2k đơn × 1.8k giường; lọc SQL theo gender+vacant trước) |
| Hóa đơn / tháng                 | ≤ 3.000                                                                      |
| Dung lượng DB 2 năm              | < 500 MB (không file đính kèm)                                            |
| Độ trễ trang list phân trang 20 | < 300 ms local                                                                |

Tối ưu engine: query beds vacant join room/building theo `gender_policy` một lần; index `beds(status)`, `beds(room_id, status)`, `rooms(building_id)`.

---

# 19. Cấu hình `application.yml` (phác thảo)

`application.yml` (mọi profile):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktx?sslMode=DISABLED&characterEncoding=utf8
    username: ${KTX_DB_USER:ktx}
    password: ${KTX_DB_PASSWORD:ktx}
  jpa:
    hibernate.ddl-auto: validate
    open-in-view: false
    properties.hibernate.dialect: org.hibernate.dialect.MySQLDialect
  servlet.session.timeout: 30m
server.servlet.session.cookie.http-only: true
ktx:
  mail.enabled: false
  timezone: Asia/Ho_Chi_Minh
```

`application-dev.yml`:

```yaml
spring.thymeleaf.cache: false
```

Connector MySQL 8.4: `useSSL=false` đã deprecated; dùng `sslMode=DISABLED` cho lab. Nếu server yêu cầu public key: thêm `allowPublicKeyRetrieval=true` (ghi trong README, không bật mặc định production). `open-in-view: false` — service fetch-join khi cần.
