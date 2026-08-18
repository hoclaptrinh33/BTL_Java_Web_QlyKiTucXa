> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Triển khai](./10-trien-khai.md) · [Mục lục](./README.md) · [PR Plan →](./12-pr-plan.md)

# 20. Tham chiếu (References)

- Spring Boot 4.1 reference — security, Thymeleaf, Data JPA.
- Spring Security 7 `authorizeHttpRequests`, form login, CSRF.
- Hibernate 7 pessimistic lock / `@Version`.
- MySQL 8.4 InnoDB `SELECT … FOR UPDATE`, generated column unique nullable.
- Flyway migrations.
- Apache POI XSSF; OpenPDF.
- EVN biểu giá sinh hoạt (chỉ **tham khảo số mẫu**, không phải cam kết pháp lý).
- Workspace: `E:/lehai/Documents/Project/BTL_Java_Web_QlyKiTucXa` (greenfield).

---

# 21. Phụ lục A — Seed tài khoản dev

| Username       | Role    | Ghi chú                                 |
| -------------- | ------- | ---------------------------------------- |
| `admin`      | ADMIN   | mật khẩu`Admin@123` chỉ profile dev |
| `staffA`     | STAFF   | tòa A                                   |
| `D22CQCN001` | STUDENT | nam, CNTT, lớp D22CQCN01, POLICY        |
| `D22CQCN002` | STUDENT | nam, cùng lớp, NONE, nộp sau          |
| `D22CQDT001` | STUDENT | nữ, tòa B                              |

Seeder tạo đủ giường để demo hết chỗ / waitlist / gom lớp.

---

# 22. Phụ lục B — Checklist chấp nhận (rút gọn)

- [ ] Login MSSV và email; sai 5 lần bị tạm khóa.
- [ ] SV không vào `/admin/**`.
- [ ] Không gán nam vào tòa nữ (UI + service + engine).
- [ ] Hai request gán cùng giường: một thành, một lỗi rõ.
- [ ] Preview không đổi bed; commit tạo HĐ DRAFT.
- [ ] Waitlist khi hết giường khớp.
- [ ] Hóa đơn case A 280 kWh / 5 người và case B residual [mục 6.5.2](./04-05-dien-nuoc.md) khớp từng đồng.
- [ ] Hủy DRAFT nhả giường; TERMINATED sau check-in vẫn chiếm giường đến checkout.
- [ ] Checkout từ ACTIVE/EXPIRED/TERMINATED nhả giường VACANT.
- [ ] Hai thread assignManual cùng giường trên MySQL: một thành, một lỗi (IT opt-in).
- [ ] Vi phạm trừ điểm; 0 điểm không nộp đơn mới.
- [ ] Xuất được xlsx danh sách nội trú.
- [ ] UI đúng [§24](./13-thiet-ke-ui-ux.md) và mock `docs/image/exampleUI.png`: nền sáng, sidebar trắng, CTA tím, chip mã, modal chốt, badge EXPIRED ≠ đã trả.

---

# 23. Phụ lục C — Danh mục `system_configs` (V2 seed nguyên văn)

`plan()` đọc weight **lúc chạy** (không tin `computed_score` cũ). `AllocationRun.weights_json` chỉ audit (preview và commit mỗi lần ghi những gì vừa dùng). Commit **không** replay weight của preview.

| `config_key`                        | type    | Default                                                                                                                                               | Dùng bởi                                                         |
| ------------------------------------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `alloc.weight.policy`               | INT     | `1000`                                                                                                                                              | `AllocationEngine.plan`                                          |
| `alloc.weight.remote`               | INT     | `500`                                                                                                                                               | `plan`                                                           |
| `alloc.weight.prev_good`            | INT     | `200`                                                                                                                                               | `plan`                                                           |
| `alloc.preference.mode`             | STRING  | `SOFT`                                                                                                                                              | `plan` (`SOFT`/`STRICT`)                                     |
| `room.type.vip.capacity`            | INT     | `2`                                                                                                                                                 | `RoomService.create`                                             |
| `contract.deposit.ratio`            | DECIMAL | `0.5`                                                                                                                                               | `createDraft` — `HALF_UP(price_per_term * ratio)` VND nguyên |
| `contract.term.months`              | INT     | `5`                                                                                                                                                 | độ dài kỳ mặc định (không nhân vào cọc)                 |
| `contract.expiry.remind.days`       | INT     | `30`                                                                                                                                                | `ContractExpiryReminderJob`                                      |
| `billing.electricity.tiers`         | JSON    | `[{"to":50,"price":1984},{"to":100,"price":2050},{"to":200,"price":2380},{"to":300,"price":2998},{"to":400,"price":3350},{"to":null,"price":3460}]` | `BillingEngine.tieredElectricity`                                |
| `billing.water.price_per_m3`        | INT     | `15000`                                                                                                                                             | billing                                                            |
| `billing.fee.sanitation_per_person` | INT     | `20000`                                                                                                                                             | billing                                                            |
| `billing.fee.internet_per_room`     | INT     | `50000`                                                                                                                                             | billing                                                            |
| `billing.fee.parking_per_person`    | INT     | `30000`                                                                                                                                             | billing                                                            |
| `billing.room.split_monthly`        | BOOLEAN | `false`                                                                                                                                             | v1**bỏ qua** nhánh true                                    |
| `billing.late.rate`                 | DECIMAL | `0.05`                                                                                                                                              | `applyLateFees` trên **subtotal**                         |
| `billing.due.days`                  | INT     | `10`                                                                                                                                                | hạn thanh toán                                                   |
| `conduct.initial`                   | INT     | `100`                                                                                                                                               | hồ sơ SV / reset                                                 |
| `conduct.warn.threshold`            | INT     | `50`                                                                                                                                                | UI cảnh cáo                                                      |
| `ticket.autoclose.days`             | INT     | `7`                                                                                                                                                 | `TicketAutoCloseJob`                                             |

Giá phòng từng loại **không** nằm config toàn cục — cột `rooms.price_per_term` (seeder: 8 giường 1.200.000; 6 giường 1.800.000; 4 giường 2.400.000; VIP 4.000.000).

---
