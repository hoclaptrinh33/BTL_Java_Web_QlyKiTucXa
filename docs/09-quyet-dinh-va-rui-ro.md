> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Bảo mật](./08-bao-mat-va-van-hanh.md) · [Mục lục](./README.md) · [Triển khai →](./10-trien-khai.md)

# 13. Quyết định then chốt (Key Decisions)

| #   | Quyết định                                                                                                                                                          | Lý do                                                                                                                                            |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| K1  | Monolith Spring MVC + Thymeleaf, không SPA/JWT                                                                                                                        | Đúng stack BTL, form login/session, ít bề mặt bảo mật, dễ chấm demo                                                                      |
| K2  | Package gốc`com.ktx`, layered `domain/repository/service/web`                                                                                                     | Sinh viên dễ chia việc theo layer rồi theo controller                                                                                         |
| K3  | Allocation**greedy có trọng số + tie-break xác định**, không ILP                                                                                          | O(n log n + n·m) đủ 2.000 đơn; giải trình được từng hạng; cài 1–2 tuần                                                             |
| K4  | Điểm ưu tiên rời rạc 1000/500/200; nộp sớm là tie-break, không cộng điểm thời gian                                                                       | Tránh phụ thuộc millisecond; công bằng “ai nộp trước trong cùng diện”                                                                 |
| K5  | Cùng lớp/khoa chỉ khi**chọn giường**, không khi xếp hạng                                                                                                | Không để một lớp chiếm hết chỗ tốt                                                                                                       |
| K6  | Dry-run lưu DB; commit**tính lại** + `FOR UPDATE` bed `ORDER BY id` + UNIQUE `active_bed_key` **và** `active_student_key` trên tập OCCUPYING | Preview không stale-safe; DB chặn đua giường**và** hai HĐ/SV                                                                         |
| K7  | Occupancy denormalize`beds.status` + `current_contract_id` (không FK vòng); sự thật = HĐ OCCUPYING; insert HĐ rồi update bed                                | InnoDB không deferred; có`reconcileOccupancy`                                                                                                 |
| K8  | Hóa đơn utility**theo người**, `floor` + residual → `min student_id`; tiền phòng **theo kỳ**                                                  | Residual luôn ≥ 0;`sum(items)=total`                                                                                                          |
| K9  | Cọc = 50% giá kỳ = HALF_UP(`price_per_term * contract.deposit.ratio`) VND nguyên; `ratio` mặc định `0.5`                                                  | Chủ đồ án chốt 2026-08-18 (ghi đè mặc định 1 tháng). Đủ ràng buộc trả phòng, cấu hình được nếu nhà trường đổi tỷ lệ |
| K10 | STAFF`assigned_building_id` NOT NULL; `StaffScope` từ PR-05; ADMIN bypass trên `/staff/**`                                                                     | Một policy, áp sớm, không chờ PR-17                                                                                                          |
| K11 | Flyway +`ddl-auto=validate`                                                                                                                                          | Schema tái lập được khi chấm                                                                                                                |
| K12 | Giá điện bậc thang**mẫu** trong `system_configs`                                                                                                          | Giảng viên đổi số không sửa code                                                                                                           |
| K13 | Không upload file, không cổng thanh toán                                                                                                                           | Cắt scope; tránh lỗ hổng multipart                                                                                                            |
| K14 | Điểm rèn luyện 100 → cảnh cáo 50 → 0 buộc rời                                                                                                                | Đủ demo quy trình kỷ luật                                                                                                                    |
| K15 | Tòa không MIXED ở v1                                                                                                                                                | Đơn giản hóa filter giới tính, đúng nhiều KTX thật                                                                                      |
| K16 | OCCUPYING = {DRAFT, ACTIVE, PENDING_RENEWAL, EXPIRED, TERMINATED}; FREE = {COMPLETED}; hủy nháp = DRAFT→COMPLETED                                                   | Unique generated column khớp SM; TERMINATED còn chiếm giường                                                                                 |
| K17 | Java **25 LTS** + Spring Boot **4.1** + Hibernate 7 + MySQL **8.4 LTS**; vẫn Thymeleaf/session                                                                 | Greenfield 2026-08: Boot 3.3/3.4 hết OSS, MySQL 8.0 EOL. Giữ MVC để khớp đề; không SPA/JWT                                                                               |
| K18 | UI sáng: canvas `#F4F6FB`, sidebar/card trắng, accent `violet` `#6D5EF5`; mock [exampleUI.png](./image/exampleUI.png) — [§24](./13-thiet-ke-ui-ux.md) | Chủ đồ án chốt theme trắng/clean (v1.6). Không sidebar tối, không linen/brass. PR-17 bám mock + §24 |

---

# 14. Phương án đã cân nhắc (Alternatives Considered)

## 14.1. REST + SPA (React/Vue) + JWT vs Thymeleaf session

| Tiêu chí              | SPA + JWT                        | Thymeleaf (chọn)         |
| ----------------------- | -------------------------------- | ------------------------- |
| Khối lượng BTL       | 2 codebase, CORS, refresh token  | 1 repo, form + CSRF sẵn  |
| Phân quyền view       | FE dễ lộ nút, phải chặn API | Server không render nút |
| UX                      | Mượt hơn                      | Đủ cho CRUD quản trị  |
| Chấm đồ án Java Web | Dễ lệch “môn Spring MVC”    | Khớp đề                |

Chọn Thymeleaf. REST mỏng chỉ cho Chart.js.

## 14.2. Greedy allocation vs ILP / optimizer

| Tiêu chí                                  | ILP (OR-Tools)         | Greedy trọng số (chọn)                  |
| ------------------------------------------- | ---------------------- | ------------------------------------------ |
| Tối ưu toàn cục (lấp đầy + gom lớp) | Tốt hơn              | Cận đúng, có thể không tối ưu 100% |
| Giải trình “tại sao SV A trước B”    | Khó                   | In ra rank + score                         |
| Phụ thuộc native lib                      | Có                    | Thuần Java                                |
| Thời gian code/test                        | Cao, rủi ro demo fail | Kiểm thử đơn vị dễ                   |
| Double-booking                              | Vẫn cần lock         | Lock từng giường tự nhiên             |

Chọn greedy. Nếu còn thời gian: post-process local search đổi chỗ cùng rank-band — **không** thuộc v1.

## 14.3. Hóa đơn theo phòng (1 phiếu, thu trưởng phòng) vs chia từng SV

| Tiêu chí                          | Phòng          | Từng SV (chọn)                 |
| ----------------------------------- | --------------- | -------------------------------- |
| Thực tế một số KTX              | Thu 1 lần      | Nhiều KTX thu từng người     |
| Công nợ cá nhân / cấm gia hạn | Khó quy trách | Rõ ai nợ                       |
| Số bản ghi                        | Ít             | Gấp N lần (vẫn < 20k/năm)    |
| Người vào giữa tháng           | Dễ             | V1 không prorate — chấp nhận |

Chọn per-student. Có thể thêm báo cáo tổng phòng.

## 14.4. Khác (ngắn)

- **Redis lock vs InnoDB FOR UPDATE:** Redis thêm moving part — loại.
- **Hibernate `ddl-auto=update` vs Flyway:** update không review được — loại cho nhánh chính.
- **BedAssignment table riêng vs contract.bed_id:** thêm bảng rõ lịch sử giường; v1 dùng contract + `check_in_outs` + room_change để giảm entity. Có thể tách ở PR sau nếu cần lịch sử nhiều lần đổi.

---

# 15. Rủi ro & giảm thiểu

| ID  | Rủi ro                                                  | Severity | Mitigation                                                                                                                |
| --- | -------------------------------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------- |
| R1  | Double-booking giường hoặc hai HĐ một SV            | Critical | Period/`ALLOCATION` lock + bed `FOR UPDATE ORDER BY id` + unique bed **và** student + IT MySQL (không tin H2) |
| R2  | Engine “nuốt” nguyện vọng hoặc lệch giới tính   | High     | Test matrix giới tính × tòa; validate mọi path ghi bed                                                               |
| R3  | Preview khác commit → mất niềm tin demo              | Medium   | UI diff; banner “kết quả tính lại”                                                                                  |
| R4  | Trễ tiến độ vì làm dashboard/export trước engine | High     | Milestone: engine trước, chart sau (PR plan)                                                                            |
| R5  | Số tiền lệch khi chia                                 | Low      | `floor` + residual ≥ 0 → min `student_id`; test case A và B [mục 6.5.2](./04-05-dien-nuoc.md)                                             |
| R6  | Seeder/dev data làm hỏng DB chấm                      | Medium   | profile`dev` tách; Flyway sạch                                                                                        |
| R7  | Session fixation / CSRF quên trên POST JS              | Medium   | Không viết fetch bỏ CSRF; dùng form                                                                                   |
| R8  | Job mail làm fail startup khi không có SMTP           | Low      | `ktx.mail.enabled=false` mặc định                                                                                    |
| R9  | Scope creep “app điểm danh QR”                       | Medium   | Non-goals; giảng viên ký đặc tả                                                                                     |
| R10 | STAFF gán nhầm tòa khác                              | Medium   | `assigned_building_id` NOT NULL + `StaffScope` từ PR-05 (mọi lệnh ghi staff)                                       |

---
