> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Sequence](./07-sequence.md) · [Mục lục](./README.md) · [Quyết định →](./09-quyet-dinh-va-rui-ro.md)

# 11. Bảo mật & quyền riêng tư (Security & Privacy)

**Mô hình đe dọa (BTL, không phải ngân hàng):**

| Threat                                        | Mức   | Mitigation                                                                                                   |
| --------------------------------------------- | ------ | ------------------------------------------------------------------------------------------------------------ |
| Đăng nhập giả mạo / nhồi mật khẩu     | High   | BCrypt, khóa tạm 5 lần sai, session timeout,`maximumSessions(1)`                                        |
| CSRF đổi phòng / commit                    | High   | CSRF token mặc định Spring Security                                                                       |
| IDOR xem hợp đồng người khác            | High   | Service lấy`studentId` từ principal, không tin `@RequestParam studentId`                              |
| Privilege escalation STAFF → commit allocate | High   | matcher`/admin/**` + `@PreAuthorize("hasRole('ADMIN')")` trên `commit`                                |
| Double-booking giường / hai HĐ một SV     | High   | `FOR UPDATE` period + bed `ORDER BY id` + UNIQUE `active_bed_key` **và** `active_student_key` |
| XSS trên mô tả ticket                      | Medium | Thymeleaf tự escape; không`th:utext` user input                                                          |
| Upload file độc                             | Medium | **V1 không upload** ảnh/tài liệu                                                                   |
| Lộ PII (SĐT người thân) trên báo cáo  | Medium | Chỉ ADMIN tải được Excel/PDF (không có bảng audit riêng ở v1)                                      |
| SQL injection                                 | Low    | JPA parameterized                                                                                            |
| Mass assignment`role=ADMIN` lúc register   | High   | `RegisterForm` không có field role; entity set cứng STUDENT                                             |

Mật khẩu không log. `application.yml` dùng `KTX_DB_USER` / `KTX_DB_PASSWORD`. HTTPS: khuyến nghị khi demo LAN, không bắt buộc localhost.

Dữ liệu cá nhân: họ tên, MSSV, SĐT, người thân — không chia sẻ ra ngoài trường. Xóa tài khoản: soft `enabled=false`, giữ hợp đồng/hóa đơn (ràng buộc kế toán đồ án).

---

# 12. Quan sát vận hành (Observability)

Quy mô BTL: **không** ELK/Prometheus cloud.

- Logging: Logback pattern `%d %p [%X{user}] %c - %m` ; MDC user từ filter.
- Mức INFO: login success/fail, allocation start/end (số assigned), phát hành hóa đơn, terminate HĐ.
- Mức WARN: lock timeout, commit lệch preview, `curr < prev`.
- Mức ERROR: exception chưa bắt, mail fail.
- Không log mật khẩu, không log full CCCD (không thu thập CCCD v1).
- Actuator: chỉ `health` trên localhost (`management.endpoints.web.exposure.include=health`).
- Metric nghiệp vụ: đếm trên dashboard (query SQL), không Micrometer push.
- “Alert”: admin dashboard badge (ticket OPEN, HĐ 30 ngày, hóa đơn OVERDUE). Không PagerDuty.

Kiểm thử:

- `AllocationEngineTest` (H2 hoặc thuần bộ nhớ): rank, gom lớp, hết giường, không lệch giới tính, SOFT không nới gender, map SKIPPED → không đổi status ở dry-run.
- `BillingEngineTest` (không DB): case A 280 kWh chia hết; case B residual 166.250 / 3; `applyLateFees` gọi 2 lần không đổi `late_fee`.
- `AllocationLockIT` (MySQL local, opt-in): **không** chạy trên H2 và **không** được ghi là đã cover InnoDB nếu chỉ pass H2.

---
