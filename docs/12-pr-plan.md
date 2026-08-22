> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Phụ lục](./11-phu-luc.md) · [Mục lục](./README.md) · [UI/UX →](./13-thiet-ke-ui-ux.md)

# PR Plan

Chiến lược: mỗi PR review được, merge được, có test hoặc màn hình demo tối thiểu. Thứ tự nghiêm ngặt theo dependency.

## PR-01 — Skeleton Maven & bootstrap Spring Boot

- **Title:** `chore: khởi tạo Spring Boot 4.1, Java 25, MySQL 8.4, Flyway trống`
- **Files:** `pom.xml` (`java.version=25`), `KtxApplication.java`, `application.yml`, `application-dev.yml`, `.gitignore`, `README.md` (chạy local, yêu cầu JDK 25)
- **Deps:** không
- **Mô tả:** Parent Boot 4.1, dependencies web/thymeleaf/security/jpa/validation/flyway/mysql. `thymeleaf-extras-springsecurity7`. App chạy được, health trang lỗi mặc định.

## PR-02 — Schema V1 & domain entities

- **Title:** `feat(db): Flyway V1 — users đến system_configs`
- **Files:** `db/migration/V1__init.sql`, `V2__seed_config.sql`, toàn bộ `com.ktx.domain.*`, enums, repositories rỗng
- **Deps:** PR-01
- **Mô tả:** Đủ bảng kể cả `renewal_requests`, `document_sequences`, `system_locks`. Generated UNIQUE `active_bed_key` **và** `active_student_key` trên tập OCCUPYING. V2 = nguyên [Phụ lục C](./11-phu-luc.md). Chưa có UI.

## PR-03 — Security form login, đăng ký SV, seeder admin

- **Title:** `feat(auth): form login, register sinh viên, phân quyền URL`
- **Files:** `SecurityConfig` (csrf, 403, `HttpSessionEventPublisher`, `maximumSessions(1)`), `KtxUserDetails*`, `AuthController`, `AuthService`, `LoginAttemptService`, `DataSeeder`, templates `auth/*`, `layouts/main.html`, `GlobalExceptionHandler`, `/error/403`
- **Deps:** PR-02
- **Mô tả:** Login username/email, redirect `/admin|/staff|/student/dashboard`, CSRF, session 30m. Test `KtxUserDetailsService`. `/staff/**` = ADMIN+STAFF; `/admin/**` = ADMIN only.

## PR-04 — Profile & quản lý user admin

- **Title:** `feat(users): profile, đổi mật khẩu, CRUD user BQL`
- **Files:** `StudentProfileService`, `AdminUserController`, templates profile/users
- **Deps:** PR-03
- **Mô tả:** Field emergency contact; admin enable/disable/reset password. Tạo STAFF **bắt buộc** `assigned_building_id`.

## PR-05 — Hạ tầng tòa / phòng / giường / tài sản

- **Title:** `feat(infra): CRUD building, room, bed, asset`
- **Files:** `Building*`, `Room*`, `Bed*`, `Asset*`, **`StaffScope`**, staff room controllers đã lọc tòa, `reconcileOccupancy` stub, templates
- **Deps:** PR-03
- **Mô tả:** Tạo bed theo capacity; giới tính tòa; STAFF `assigned_building_id` NOT NULL enforce khi tạo user STAFF (cùng PR-04 nếu user form nằm đó — predicate ghi hạ tầng có từ PR này). ADMIN bypass `StaffScope`.

## PR-05a — Sinh phòng hàng loạt

- **Title:** `feat(infra): sinh phong hang loat theo dai tang`
- **Files:** `RoomBatchForm`, `RoomBatchResult`, `RoomService.createBatch`, `AdminRoomController` `GET/POST /admin/rooms/batch`, `admin/rooms/batch.html`, test
- **Deps:** PR-05 (CRUD phòng)
- **Mô tả:** Form tòa + tầng từ/đến + số phòng/tầng + loại + `price_per_term`. Số `{tầng}{stt}` (`101–110`). Một transaction, skip số đã có, tự sinh giường `G1…Gn`. Không Excel, không cọc trên phòng. Trần 100 phòng/lần.

## PR-06 — Đợt đăng ký & đơn nguyện vọng

- **Title:** `feat(apply): period lifecycle + sinh viên nộp/rút đơn`
- **Files:** `RegistrationPeriod*`, `RoomApplication*`, templates admin/student applications, validate cửa sổ & unique
- **Deps:** PR-04, PR-05
- **Mô tả:** Snapshot priority khi SUBMITTED; chặn nộp nếu đã có HĐ OCCUPYING. Chưa xếp phòng.

## PR-07 — AllocationEngine + unit test

- **Title:** `feat(alloc): engine greedy, điểm ưu tiên, gom lớp/khoa`
- **Files:** `AllocationEngine.java`, `OccupancySnapshot.java`, `AllocConfig.java`, `AllocationEngineTest.java`
- **Deps:** PR-06
- **Mô tả:** `plan()` tính lại score. Test H2/bộ nhớ: rank, SOFT (không nới gender), hết giường, comparator đầy đủ, null-safe lớp/khoa. Không nhận H2 là chứng minh lock.

## PR-08 — Preview / commit / gán tay + tạo HĐ nháp

- **Title:** `feat(alloc): dry-run, commit FOR UPDATE, createDraft, manual assign`
- **Files:** `AllocationService`, `ContractService.createDraftFromAllocation` + `cancelDraft`, `DocumentNumberService`, `AdminAllocationController`, templates preview/commit, `it/AllocationLockIT` (opt-in MySQL)
- **Deps:** PR-07
- **Mô tả:** INSERT HĐ DRAFT rồi UPDATE bed; map SKIPPED→REJECTED khi commit. `assignManual` khóa period hoặc `system_locks.ALLOCATION`. IT lock **không** chạy H2.

## PR-09 — Hợp đồng: check-in/out, cọc, nhắc hết hạn

- **Title:** `feat(contract): check-in/out, deposit invoice, job 30 ngày`
- **Files:** `CheckInOutService`, nốt `ContractService` (ACTIVE/TERMINATED/COMPLETED), `ContractExpiryReminderJob`, `Notification*`, `BillingEngine.issueRoomFee`/`issueDeposit` (có thể stub đến PR-12), controllers admin/staff/student
- **Deps:** PR-08
- **Mô tả:** Check-in DRAFT→ACTIVE; checkout ACTIVE|EXPIRED|TERMINATED; cọc 50% giá kỳ (`issueDeposit`). Hủy nháp đã có ở PR-08. Mail sau (PR-16).

## PR-10 — Chuyển phòng & trả phòng

- **Title:** `feat(contract): room-change và return-room workflow`
- **Files:** `RoomChangeService`, student/admin templates
- **Deps:** PR-09
- **Mô tả:** Khóa 2 giường `ORDER BY id ASC`. Return đi qua checkout.

## PR-11 — Gia hạn hợp đồng

- **Title:** `feat(contract): renewal request + gia hạn end_date`
- **Files:** `RenewalService`, templates (entity `renewal_requests` **đã có V1 / PR-02**)
- **Deps:** PR-09
- **Mô tả:** Không chạy engine; `PENDING_RENEWAL → EXPIRED` nếu quá hạn. Migration PR-11 chỉ khi cần cột phụ, không thiết kế bảng mới.

## PR-12 — Ghi chỉ số & BillingEngine

- **Title:** `feat(billing): reading tháng + công thức bậc thang + chia N người`
- **Files:** `UtilityReading*`, `BillingEngine`, `BillingEngineTest` (case 280 kWh), `Invoice*` domain
- **Deps:** PR-09
- **Mô tả:** `floor` + residual; cờ `elec_replaced`/`water_replaced`; `N` lúc bấm phát hành. Hủy HĐ: tombstone `{key}:cancelled:{id}` rồi mới issue lại. Test case A+B.

## PR-13 — Thanh toán & quá hạn

- **Title:** `feat(billing): ghi Payment, trạng thái OVERDUE, phí 5%`
- **Files:** `InvoiceService`, `PaymentService`, `InvoiceOverdueJob`, admin/student invoice views
- **Deps:** PR-12
- **Mô tả:** Trả góp; `late_fee = ceil(subtotal * rate)` một lần.

## PR-14 — Ticket sự cố & vi phạm / điểm rèn luyện

- **Title:** `feat(ops): maintenance tickets + violations + conduct score`
- **Files:** `Ticket*`, `Violation*`, `ConductService`, templates 3 role
- **Deps:** PR-05, PR-04
- **Mô tả:** Bảng điểm mặc định 4 dòng; `TicketAutoCloseJob` 7 ngày; SEVERE gợi ý terminate (gọi ContractService nếu PR-09 đã merge). `StaffScope` trên ticket/vi phạm.

## PR-15 — Dashboard, Chart.js, Excel/PDF

- **Title:** `feat(report): occupancy, công nợ, export POI/OpenPDF`
- **Files:** `DashboardService`, `ExportService`, `admin/dashboard.html`, report controllers, dependencies POI/OpenPDF
- **Deps:** PR-08, PR-13
- **Mô tả:** JSON nội bộ cho chart; download xlsx/pdf.

## PR-16 — Cấu hình hệ thống UI + mail tùy chọn

- **Title:** `feat(config): màn hình system_configs + Spring Mail opt-in`
- **Files:** `SystemConfigService`, `AdminConfigController`, `MailConfig`, `NotificationService` gửi mail nếu `ktx.mail.enabled`
- **Deps:** PR-09, PR-02
- **Mô tả:** Đổi giá điện/cọc/ngưỡng không sửa code.

## PR-17 — Hoàn thiện UX staff/admin

- **Title:** `feat(ux): polish navbar, flash, dashboard staff`
- **Files:** fragments sidebar, trang chủ role, copy/nhãn
- **Deps:** PR-05, PR-12, PR-14
- **Mô tả:** **Không** thêm `StaffScope` (đã có PR-05). URL matcher đã chặn STAFF khỏi `/admin/**` từ PR-03. Làm đúng [§24 UI/UX](./13-thiet-ke-ui-ux.md) + mock `docs/image/exampleUI.png`: nền sáng, sidebar trắng, accent tím, chip mã, modal commit.

## PR-18 — Seeder demo đầy đủ & README buổi chấm

- **Title:** `chore(demo): dữ liệu kịch bản phân bổ + hóa đơn + checklist`
- **Files:** `DataSeeder` mở rộng, `README.md`, `docs/` copy đặc tả
- **Deps:** PR-15, PR-16
- **Mô tả:** Kịch bản 15 phút: nộp đơn → preview → commit → check-in → reading → hóa đơn.

---

*Hết tài liệu thiết kế v1.6 — Approved. Stack: Java 25 LTS + Spring Boot 4.1 + MySQL 8.4 LTS. Cọc 50% giá kỳ. Mọi thay đổi số tiền/điểm sau này phải cập nhật [Phụ lục C](./11-phu-luc.md) / `V2__seed_config.sql`, không hard-code rải rác trong service.*
