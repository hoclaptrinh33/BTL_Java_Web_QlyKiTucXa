# Quản lý Ký túc xá (BTL_Java_Web_QlyKiTucXa)

Ứng dụng web quản lý ký túc xá, tự động phân bổ chỗ ở theo điểm ưu tiên (Gom lớp, giải quyết hết chỗ / waitlist), quản lý hợp đồng lưu trú, ghi chỉ số điện nước bậc thang và xuất hóa đơn đối soát.

| Thuộc tính | Giá trị |
| --- | --- |
| **Mã đồ án** | `BTL_Java_Web_QlyKiTucXa` |
| **Kiến trúc** | Spring Boot 4.1 · Spring Security 7 · Spring Data JPA · Hibernate 7 |
| **Môi trường** | **Java 25 LTS** (Eclipse Temurin 25+) · **MySQL 8.4 LTS** · Thymeleaf |
| **Đặc tả thiết kế** | [docs/README.md](./docs/README.md) (v1.6 Approved) |
| **Checklist nghiệm thu** | [docs/11-phu-luc.md](./docs/11-phu-luc.md) (Phụ lục A, B, C) |

---

## 1. Yêu cầu môi trường & Cài đặt

### Yêu cầu tiên quyết
- **JDK 25** (Khuyến nghị Eclipse Temurin 25). Kiểm tra: `java -version`
- **MySQL 8.4 LTS Server** (hoặc tương thích MySQL 8.0+). Port mặc định `3306`.
- Maven 3.9+ (hoặc dùng wrapper `mvnw.cmd` / `./mvnw` đính kèm repo).

### Khởi tạo cơ sở dữ liệu
Đăng nhập MySQL Client (với quyền root) và chạy script tạo database và user:

```sql
CREATE DATABASE ktx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ktx'@'localhost' IDENTIFIED BY 'ktx';
GRANT ALL PRIVILEGES ON ktx.* TO 'ktx'@'localhost';
FLUSH PRIVILEGES;
```

> **Lưu ý MySQL 8.4**: Mặc định ứng dụng kết nối qua `jdbc:mysql://localhost:3306/ktx?sslMode=DISABLED&characterEncoding=utf8`. Nếu MySQL Server yêu cầu public key retrieval trong lab: cấu hình biến môi trường hoặc thêm `allowPublicKeyRetrieval=true`.

### Cấu hình biến môi trường (Tùy chọn)
Mặc định ứng dụng sử dụng user `ktx` / mật khẩu `ktx`. Bạn có thể thay đổi bằng biến môi trường (xem [`.env.example`](./.env.example)):
- `KTX_DB_USER`: Tên tài khoản MySQL (mặc định `ktx`).
- `KTX_DB_PASSWORD`: Mật khẩu MySQL (mặc định `ktx`).

### Khởi chạy ứng dụng

- **Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```

Ứng dụng khởi động tại địa chỉ: **`http://localhost:8080`**

### Chạy toàn bộ kiểm thử tự động
```powershell
.\mvnw.cmd -q test
```

---

## 2. Dữ liệu mẫu & Tài khoản thử nghiệm (Phụ lục A)

Hệ thống tích hợp `DataSeeder` tự động nạp dữ liệu chuẩn bị cho buổi demo khi khởi chạy với profile `dev`. Mật khẩu chung cho tất cả các tài khoản thử nghiệm là: **`Admin@123`**.

### 2.1. Tài khoản Quản trị & Cán bộ
| Username | Role | Họ tên | Phạm vi phụ trách | Ghi chú |
| --- | --- | --- | --- | --- |
| `admin` | `ROLE_ADMIN` | Quản trị viên | Toàn bộ hệ thống | Toàn quyền phân bổ, duyệt đơn, cấu hình giá, xuất báo cáo |
| `staffA` | `ROLE_STAFF` | Cán bộ A | Tòa A (Nam) | Quản lý phòng/giường tòa A, check-in/out, ghi điện nước |
| `staffB` | `ROLE_STAFF` | Cán bộ B | Tòa B (Nữ) | Quản lý phòng/giường tòa B, check-in/out |

### 2.2. Sinh viên phục vụ Demo Kịch bản 15 phút
| MSSV / Username | Họ và tên | Giới tính | Lớp | Điểm UT | Tình trạng phục vụ demo |
| --- | --- | --- | --- | --- | --- |
| **`D22CQCN004`** | Vũ Hải Đăng | Nam | D22CQCN03 | 0 | **Dùng demo Nộp đơn mới**: Điểm rèn luyện 100, chưa có đơn, nộp trực tiếp vào Đợt OPEN |
| **`D22CQCN001`** | Nguyễn Văn Nam | Nam | D22CQCN01 | 1200 | Chính sách (1000đ) + Ở ngoan (200đ) -> Trúng tuyển Rank 1 (Tòa A) |
| **`D22CQCN003`** | Lê Hoàng Long | Nam | D22CQCN02 | 500 | Vùng sâu vùng xa (500đ) -> Trúng tuyển Rank 2 (Tòa A) |
| **`D22CQCN002`** | Trần Văn Hùng | Nam | D22CQCN01 | 200 | Cùng lớp với `D22CQCN001` -> **Demo Gom lớp** vào phòng A101 |
| **`D22CQDT001`** | Phạm Thị Lan | Nữ | D22CQDT01 | 200 | Nữ nộp đơn Tòa B -> Trúng tuyển giường B101-1 |
| **`D22CQDT002`** | Hoàng Thùy Linh | Nữ | D22CQDT01 | 0 | Nữ nộp sớm -> Trúng tuyển giường B101-2 |
| **`D22CQDT003`** | Nguyễn Mai Phương | Nữ | D22CQDT02 | 0 | Nữ nộp sau, Tòa B hết chỗ -> **Demo Hết chỗ / WAITLIST** |

### 2.3. Sinh viên phục vụ Đối soát Hóa đơn & Điện nước (Phụ lục B)
| MSSV / Username | Phòng | Tình trạng hóa đơn | Công thức đối soát |
| --- | --- | --- | --- |
| **`D21CQCN010`** | A102 (5 SV) | **PAID** (Đã thanh toán) | **Case A 280 kWh**: Chia 5 người = 199.908đ + 50.000đ phụ phí = **249.908đ** |
| `D21CQCN011` – `14` | A102 (5 SV) | **UNPAID** (Chưa trả) | Cùng phòng A102, mỗi sinh viên 249.908đ (phần dư = 0đ) |
| **`D21CQCN020`** | A103 (3 SV) | **OVERDUE** (Quá hạn) | **Case B Residual**: Chia 3 người dư 2đ dồn cho min MSSV + Phạt chậm 5% = **110.689đ** |
| `D21CQCN021` – `22` | A103 (3 SV) | **UNPAID** | Cùng phòng A103: 55.416đ + 50.000đ phụ phí = **105.416đ** |

### 2.4. Sinh viên phục vụ Demo Kỷ luật & Trạng thái Hợp đồng
| MSSV / Username | Trạng thái | Mục đích demo |
| --- | --- | --- |
| **`D22CQCN098`** | Bị cấm ở KTX (`blocked_from_housing = true`) | Hệ thống chặn nộp đơn; Engine tự động `SKIPPED_BLOCKED` |
| **`D22CQCN099`** | 0 điểm rèn luyện (`conduct_score = 0`) | Hệ thống chặn không cho nộp đơn ở mới |
| **`D20CQCN001`** | HĐ DRAFT tại A104-1 | Demo chức năng Hủy DRAFT nhả giường ngay lập tức |
| **`D20CQCN002`** | HĐ TERMINATED tại A104-2 | Hủy HĐ sau check-in: Giường vẫn OCCUPIED đến khi checkout |
| **`D20CQCN003`** | HĐ EXPIRED tại A105-1 | Hết hạn hợp đồng: Giường vẫn OCCUPIED đến khi checkout |

---

## 3. Kịch bản Demo 15 phút (Buổi chấm BTL)

Kịch bản được thiết kế tinh gọn, bám sát luồng nghiệp vụ cốt lõi: **Nộp đơn → Preview → Commit → Check-in → Reading → Hóa đơn**. Giảng viên có thể theo dõi trực tiếp trên UI mà không cần giải thích code:

```
[Sinh viên: Nộp đơn] ──> [Admin: Preview Dry-run] ──> [Admin: Commit tạo HĐ]
                                                              │
[Sinh viên: Xem Hóa đơn] <── [Staff: Ghi số điện nước] <── [Staff: Check-in]
```

### Bước 1: Sinh viên nộp đơn đăng ký chỗ ở (2 phút)
1. Truy cập `http://localhost:8080/login`, đăng nhập bằng tài khoản sinh viên:
   - Username: `D22CQCN004` (hoặc email: `d22cqcn004@example.com`) / Mật khẩu: `Admin@123`
2. Vào menu **Đơn đăng ký** (`/student/applications`) → Bấm **Nộp đơn mới**.
3. Chọn đợt đang mở: *Đợt Đăng ký Tân SV K22 (Đang nhận đơn)*.
4. Chọn nguyện vọng: Tòa A (Nam) · Loại phòng: STANDARD_4. Bấm **Gửi đơn**.
5. **Kiểm chứng**: Đơn được ghi nhận ở trạng thái `SUBMITTED`.
6. *(Tùy chọn kiểm tra ràng buộc)*: Đăng xuất, thử đăng nhập bằng `D22CQCN098` (bị cấm) hoặc `D22CQCN099` (0 điểm rèn luyện) → Hệ thống hiển thị cảnh báo đỏ và khóa form nộp đơn.

### Bước 2: Quản trị viên Chạy xem trước phân bổ (Dry-run Preview) (4 phút)
1. Đăng xuất, đăng nhập tài khoản Quản trị viên:
   - Username: `admin` / Mật khẩu: `Admin@123`
2. Vào menu **Phân bổ chỗ ở** (`/admin/allocations`).
3. Chọn đợt: *Đợt 1 — Đăng ký KTX Học kỳ 1 (2026-2027)* (đợt đã đóng nhận đơn, sẵn sàng chạy thuật toán).
4. Bấm nút **Chạy xem trước (Dry-run)**:
   - Thuật toán `AllocationEngine` tính điểm deterministic theo trọng số Phụ lục C (`alloc.weight.*`).
5. **Kiểm chứng bảng kết quả xem trước**:
   - **Xếp hạng điểm**: `D22CQCN001` (1200 điểm - Rank 1), `D22CQCN003` (500 điểm - Rank 2), `D22CQCN002` (200 điểm - Rank 3).
   - **Cơ chế Gom lớp**: Sinh viên `D22CQCN002` cùng lớp `D22CQCN01` với `D22CQCN001` được thuật toán ưu tiên gom vào cùng phòng A101 (giường 101-2).
   - **Cơ chế Hết chỗ / Waitlist**: Tòa B chỉ còn 2 giường trống nhưng có 3 sinh viên nữ nộp đơn. `D22CQDT001` và `D22CQDT002` được xếp giường; `D22CQDT003` nộp sau rơi vào trạng thái **WAITLISTED (NO_VACANT_BED)**.
   - **Cơ chế Kỷ luật**: Sinh viên bị cấm `D22CQCN098` tự động bị bỏ qua với trạng thái **SKIPPED (SKIPPED_BLOCKED)**.
   - **Tính toàn vẹn Dry-run**: Kiểm tra danh sách giường trong hệ thống vẫn ở trạng thái `VACANT`, chưa bị ghi đè.

### Bước 3: Chốt phân bổ & Khóa phòng (Commit Allocation) (3 phút)
1. Tại trang chi tiết lượt phân bổ, bấm **Chốt phân bổ (Commit)**.
2. Xác nhận tại Modal cảnh báo chốt kết quả.
3. **Kiểm chứng**:
   - Các đơn trúng tuyển chuyển sang trạng thái `APPROVED`, tạo hợp đồng ở trạng thái **DRAFT**.
   - Đơn không còn chỗ chuyển sang **WAITLISTED**.
   - Toàn bộ giường trúng tuyển chuyển sang trạng thái **OCCUPIED** và gắn `current_contract_id`.
4. *(Demo hủy nháp)*: Bấm nút **Hủy DRAFT** trên một hợp đồng nháp → Giường ngay lập tức được nhả về trạng thái `VACANT`.

### Bước 4: Thủ tục nhận phòng (Check-in) & Quản lý Hợp đồng (2 phút)
1. Đăng xuất, đăng nhập tài khoản Cán bộ tòa A:
   - Username: `staffA` / Mật khẩu: `Admin@123`
2. Cán bộ thực hiện Check-in cho sinh viên:
   - Hợp đồng chuyển từ `DRAFT` sang `ACTIVE`.
   - Lập biên bản bàn giao chìa khóa, hiện trạng tài sản (bàn, ghế, điều hòa, quạt).
3. **Kiểm chứng trạng thái chiếm giường**:
   - Xem phòng A104: Sinh viên `D20CQCN002` có HĐ `TERMINATED` (bị chấm dứt) nhưng do chưa làm thủ tục Check-out nên giường vẫn bị chiếm giữ (`OCCUPIED`). Chỉ sau khi Check-out giường mới về `VACANT`.
   - Xem phòng A105: Sinh viên `D20CQCN003` có HĐ `EXPIRED` (hết hạn kỳ trước) vẫn chiếm giường cho tới khi Check-out.

### Bước 5: Ghi chỉ số Điện nước (Utility Reading) (2 phút)
1. Cán bộ `staffA` xem sổ ghi điện nước tháng 09/2026:
   - **Phòng A102 (5 người)**: Chỉ số điện cũ 1000, mới 1280 (tiêu thụ 280 kWh). Chỉ số nước cũ 50, mới 68 (tiêu thụ 18 m³).
   - **Phòng A103 (3 người)**: Chỉ số điện cũ 100, mới 151 (tiêu thụ 51 kWh). Chỉ số nước cũ 20, mới 21 (tiêu thụ 1 m³).

### Bước 6: Phát hành & Đối soát Hóa đơn (Billing Engine) (2 phút)
1. Đăng xuất, đăng nhập sinh viên `D21CQCN010` (Phòng A102 - Case A):
   - Vào menu **Hóa đơn & Tiền phòng** (`/student/invoices`).
   - Mở chi tiết hóa đơn tháng 09/2026.
   - **Đối soát Case A**:
     - Điện: 280 kWh theo 6 bậc EVN = 679.540đ.
     - Nước: 18 m³ × 15.000đ = 270.000đ.
     - Internet: 50.000đ/phòng.
     - Tổng chia đều 5 người: `(679.540 + 270.000 + 50.000) / 5 = 199.908đ/người` (dư 0đ).
     - Phụ phí cá nhân: Vệ sinh 20.000đ + Gửi xe 30.000đ = 50.000đ.
     - **Tổng cộng: 249.908đ** (Khớp chính xác từng đồng theo mục 6.5.2 đặc tả). Trạng thái: `PAID`.
2. Đăng xuất, đăng nhập sinh viên `D21CQCN020` (Phòng A103 - Case B Residual):
   - Mở chi tiết hóa đơn tháng 09/2026.
   - **Đối soát Case B**:
     - Điện 51 kWh (101.250đ) + Nước 1 m³ (15.000đ) + Net (50.000đ) = 166.250đ.
     - Chia 3 người: `floor(166.250 / 3) = 55.416đ`. Phần dư 2đ tự động dồn cho sinh viên có MSSV nhỏ nhất (`D21CQCN020`) thành 55.418đ.
     - Subtotal = 55.418đ + 50.000đ (phụ phí) = 105.418đ.
     - Phạt quá hạn 5%: `ceil(105.418 × 0.05) = 5.271đ`.
     - **Tổng cộng: 110.689đ**. Trạng thái: `OVERDUE` (Cảnh báo đỏ).

---

## 4. Bảng Checklist Nghiệm thu (Phụ lục B)

| STT | Tiêu chí chấp nhận | Trạng thái | Cơ chế kỹ thuật chứng minh |
| :---: | --- | :---: | --- |
| 1 | Đăng nhập bằng MSSV hoặc Email; sai 5 lần tạm khóa | **ĐẠT** | `KtxUserDetailsService.loadUserByUsername` hỗ trợ username/email; `LoginAttemptService` chặn theo IP/Username sau 5 lần thất bại |
| 2 | Sinh viên không được phép truy cập `/admin/**` | **ĐẠT** | `SecurityConfig` phân quyền nghiêm ngặt: `.requestMatchers("/admin/**").hasRole("ADMIN")` |
| 3 | Không gán nam vào tòa nữ (UI + Service + Engine) | **ĐẠT** | Ràng buộc `BuildingGenderPolicy` tại `AllocationEngineImpl`, form nộp đơn lọc tòa theo giới tính SV |
| 4 | Hai request gán cùng giường: một thành, một lỗi rõ | **ĐẠT** | Hibernate `@Version` trên `Bed` và pessimistic locking `SELECT ... FOR UPDATE` chặn xung đột đồng thời |
| 5 | Preview không đổi bed; Commit tạo HĐ DRAFT | **ĐẠT** | `previewAndStore` dùng snapshot in-memory; `commit` lưu `AllocationRun` và phát sinh hợp đồng DRAFT |
| 6 | Đưa vào danh sách chờ (Waitlist) khi hết giường khớp | **ĐẠT** | Thuật toán chuyển `AllocationResult.WAITLISTED` kèm nguyên nhân `NO_VACANT_BED` khi hết chỗ |
| 7 | Hóa đơn Case A (280 kWh) và Case B (residual) khớp từng đồng | **ĐẠT** | `BillingEngine` tính toán 6 bậc EVN, chia nguyên `floor` và dồn phần dư `residual` cho min student ID |
| 8 | Hủy DRAFT nhả giường; TERMINATED sau check-in vẫn chiếm giường | **ĐẠT** | `cancelDraft` giải phóng giường ngay; hợp đồng `TERMINATED` giữ cờ chiếm giữ đến khi phát sinh bản ghi `CHECK_OUT` |
| 9 | Check-out từ ACTIVE / EXPIRED / TERMINATED nhả giường VACANT | **ĐẠT** | `CheckInOutService.checkOut` chuyển trạng thái giường sang `VACANT` và xóa `currentContractId` |
| 10 | Hai luồng `assignManual` cùng giường: một thành, một lỗi | **ĐẠT** | Unit/IT test `AllocationLockIT` với pessimistic write lock trên DB MySQL |
| 11 | Vi phạm trừ điểm; 0 điểm không được nộp đơn mới | **ĐẠT** | `ConductService` trừ điểm tự động; `StudentApplicationController` chặn nộp đơn nếu `conductScore <= 0` |
| 12 | Xuất Excel (.xlsx) danh sách nội trú | **ĐẠT** | Tích hợp Apache POI XSSF xuất báo cáo danh sách sinh viên nội trú, công nợ |
| 13 | Giao diện chuẩn UI/UX (§24): Nền sáng, sidebar trắng, CTA tím | **ĐẠT** | Theme CSS chuẩn Bootstrap tùy biến, sidebar trắng tối giản, chip mã định danh, modal xác nhận chốt |

---

## 5. Danh mục Cấu hình Hệ thống (Phụ lục C)

Các tham số nghiệp vụ được nạp qua Flyway `V2__seed_config.sql` và quản trị trực tiếp tại màn hình `/admin/configs`:

| Khóa cấu hình (`config_key`) | Kiểu dữ liệu | Giá trị mặc định | Diễn giải |
| --- | :---: | :---: | --- |
| `alloc.weight.policy` | INT | `1000` | Trọng số ưu tiên diện chính sách |
| `alloc.weight.remote` | INT | `500` | Trọng số ưu tiên vùng sâu vùng xa |
| `alloc.weight.prev_good` | INT | `200` | Trọng số ưu tiên sinh viên lưu trú tốt kỳ trước |
| `alloc.preference.mode` | STRING | `SOFT` | Chế độ nguyện vọng (`SOFT` / `STRICT`) |
| `contract.deposit.ratio` | DECIMAL | `0.5` | Tỷ lệ tiền đặt cọc (50% giá phòng/kỳ) |
| `contract.term.months` | INT | `5` | Độ dài tiêu chuẩn của một học kỳ (5 tháng) |
| `billing.electricity.tiers` | JSON | 6 bậc EVN | Biểu giá điện bậc thang sinh hoạt EVN |
| `billing.water.price_per_m3` | INT | `15000` | Đơn giá nước sinh hoạt (15.000 đ/m³) |
| `billing.fee.sanitation_per_person` | INT | `20000` | Phí vệ sinh môi trường (20.000 đ/người/tháng) |
| `billing.fee.internet_per_room` | INT | `50000` | Phí internet tốc độ cao (50.000 đ/phòng/tháng) |
| `billing.fee.parking_per_person` | INT | `30000` | Phí gửi xe sinh viên (30.000 đ/người/tháng) |
| `billing.late.rate` | DECIMAL | `0.05` | Tỷ lệ phạt nộp chậm tiền phòng/dịch vụ (5% subtotal) |
| `conduct.initial` | INT | `100` | Điểm rèn luyện khởi tạo ban đầu cho sinh viên |
| `conduct.warn.threshold` | INT | `50` | Ngưỡng điểm rèn luyện cảnh báo vi phạm |
