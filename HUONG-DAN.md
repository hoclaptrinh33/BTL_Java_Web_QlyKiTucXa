# Hướng dẫn chạy và dùng DormManage

File này dành cho người **mới clone repo, người dùng thử nghiệm hoặc giảng viên chấm đồ án**. Đọc xong có thể:

1. Cài đặt môi trường, tạo database, khởi chạy ứng dụng.
2. Nắm rõ **toàn bộ danh sách tài khoản thử nghiệm** (Admin hệ thống, Quản lý KTX, Cán bộ tòa, Sinh viên các diện).
3. Thực hiện kịch bản demo chuẩn 15 phút hoặc thử nghiệm từng phân hệ chức năng.
4. Tra cứu bản đồ màn hình và xử lý các lỗi thường gặp.

Đặc tả chi tiết nằm ở [`docs/README.md`](./docs/README.md). Quy trình Gitflow nằm ở [`CONTRIBUTING.md`](./CONTRIBUTING.md).

Repo: [hoclaptrinh33/BTL_Java_Web_QlyKiTucXa](https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa)

---

## 1. Tổng quan hệ thống

**DormManage** là hệ thống quản lý ký túc xá và phân bổ chỗ ở sinh viên toàn diện, xây dựng trên nền tảng **Java 25 LTS** và **Spring Boot 4.1**:

- **Quản lý hạ tầng**: Tòa nhà (Nam/Nữ), tầng, phòng ở, giường (`G1`...`Gn`), tài sản và tình trạng trang thiết bị.
- **Quy trình đăng ký & Phân bổ chỗ ở**: Đợt đăng ký (DRAFT, OPEN, CLOSED), nộp đơn nguyện vọng, thuật toán `AllocationEngine` tính điểm ưu tiên đa tiêu chí, cơ chế **gom lớp** cùng phòng, danh sách chờ (**Waitlist**), xử lý loại trừ sinh viên kỷ luật.
- **Hợp đồng & Lưu trú**: Vòng đời hợp đồng (DRAFT → ACTIVE → COMPLETED / TERMINATED / EXPIRED), thủ tục Check-in / Check-out bàn giao chìa khóa, yêu cầu chuyển phòng, gia hạn hợp đồng.
- **Điện nước & Hóa đơn**: Ghi nhận chỉ số công tơ điện nước tháng, biểu giá lũy tiến 6 bậc EVN, chia theo số người thực tế trong phòng, chia phần dư dồn cho MSSV nhỏ nhất, tính phí phạt chậm trả 5% (OVERDUE).
- **Sự cố & Kỷ luật**: Sinh viên báo hỏng hóc (Ticket), cán bộ tiếp nhận xử lý; lập biên bản vi phạm, trừ điểm rèn luyện, cấm ở KTX.
- **Báo cáo & Thống kê**: Biểu đồ Chart.js tỷ lệ lấp đầy, thu chi, xuất dữ liệu Excel & PDF.
- **Phân quyền RBAC 2 mặt bằng**: Phân tách rõ ràng giữa Quản trị kỹ thuật (`SYSTEM plane`) và Quản lý vận hành (`OPERATION plane`), ghi nhận vết kiểm toán an toàn (**Audit Log**).

---

## 2. Chuẩn bị môi trường

Cần chuẩn bị trên máy:

- **JDK 25** (Eclipse Temurin hoặc Oracle JDK 25)
- **MySQL 8.4 LTS** (hoặc MySQL 8.0+) đang chạy local port 3306
- **Git**
- Maven: Dùng trực tiếp Maven Wrapper (`mvnw.cmd` trên Windows hoặc `./mvnw` trên Linux/macOS), không cần cài Maven rời.

Kiểm tra phiên bản Java:

```powershell
java -version
```

Kết quả hiển thị phải là phiên bản `25`. Nếu đang trỏ JDK khác, vui lòng cấu hình biến môi trường `JAVA_HOME` tương ứng.

---

## 3. Lấy mã nguồn mới nhất

Luôn làm việc trên nhánh **`develop`**:

```powershell
cd E:\lehai\Documents\Project
git clone git@github.com:hoclaptrinh33/BTL_Java_Web_QlyKiTucXa.git
cd BTL_Java_Web_QlyKiTucXa
git checkout develop
git pull origin develop
```

*(Hoặc dùng HTTPS nếu chưa cấu hình SSH: `git clone https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa.git`)*

---

## 4. Tạo cơ sở dữ liệu MySQL

Mở MySQL Workbench, MySQL CLI hoặc DBeaver và thực thi:

```sql
CREATE DATABASE ktx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ktx'@'localhost' IDENTIFIED BY 'ktx';
GRANT ALL PRIVILEGES ON ktx.* TO 'ktx'@'localhost';
FLUSH PRIVILEGES;
```

> [!NOTE]
> - Ứng dụng đọc thông tin database mặc định là user `ktx`, mật khẩu `ktx` (tham khảo file [`.env.example`](./.env.example)).
> - Cơ chế **Flyway Migration** tự động tạo schema bảng và seed cấu hình hệ thống khi app khởi động (`V1__init.sql` → `V2__seed_config.sql` → `V4__rbac.sql` → `V5__audit_logs.sql` → `V6__normalize_3nf.sql`). **Tuyệt đối không cần import SQL bằng tay.**
> - Nếu gặp lỗi kết nối Public Key trên máy lab/máy mới, thêm tham số `allowPublicKeyRetrieval=true` vào JDBC URL trong `src/main/resources/application.yml`.

---

## 5. Khởi chạy ứng dụng

Khởi chạy với profile **`dev`** để kích hoạt `DataSeeder` nạp sẵn toàn bộ dữ liệu mẫu phục vụ kịch bản demo:

```powershell
cd E:\lehai\Documents\Project\BTL_Java_Web_QlyKiTucXa
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=history"
```

> [!WARNING]
> Trên **PowerShell (Windows)**, bắt buộc phải bọc `"-Dspring-boot.run.profiles=dev"` trong **dấu ngoặc kép `""`**. Nếu không có ngoặc kép, PowerShell sẽ ngắt tham số thành `.run.profiles=dev` và Maven sẽ báo lỗi `Unknown lifecycle phase`.
>
> *(Trên Linux / macOS: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`)*

Khi màn hình terminal hiển thị dòng thông báo:

```text
Started KtxApplication in ... seconds
```

Mở trình duyệt truy cập:

```text
http://localhost:8080
```

Hệ thống sẽ tự động chuyển hướng tới trang Đăng nhập (`/login`).

- Chạy kiểm thử tự động toàn dự án:

```powershell
.\mvnw.cmd test
```

- Dừng ứng dụng: Nhấn tổ hợp phím `Ctrl + C` trong cửa sổ dòng lệnh.
- Trường hợp muốn xóa trắng DB để seed lại từ đầu:

```sql
DROP DATABASE ktx;
CREATE DATABASE ktx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 6. Danh mục tài khoản thử nghiệm (Profile `dev`)

> [!IMPORTANT]
> Mật khẩu mặc định cho **TẤT CẢ** tài khoản mẫu là: **`Admin@123`**
> Mọi tài khoản đều có thể đăng nhập bằng **Username** hoặc **Email**.

Hệ thống áp dụng mô hình phân quyền đa cấp RBAC (Role-Based Access Control) với sự phân định rõ ràng giữa Quản trị kỹ thuật, Quản lý vận hành, Cán bộ quản lý tòa và Sinh viên:

### 6.1. Nhóm tài khoản Hệ thống & Quản lý vận hành

| Username             | Email                  | Vai trò (Role)                                         | Phạm vi phụ trách & Quyền hạn chính                                                                                                                                                                                                                                                                                                                             |
| :------------------- | :--------------------- | :------------------------------------------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`admin`**  | `admin@example.com`  | **SYSTEM_ADMIN** *(Quản trị hệ thống)*      | Quản trị kỹ thuật (`SYSTEM plane`): Quản lý cấu hình giá/trọng số (`/admin/configs`), Quản lý tài khoản quản trị nội bộ (`/admin/accounts`), Xem nhật ký kiểm toán hệ thống (`/admin/logs`), Dashboard kỹ thuật (`/admin/dashboard`). *(Không thao tác nghiệp vụ KTX).*                                                    |
| **`quanly`** | `quanly@example.com` | **QUAN_LY** (`ROLE_ADMIN`) *(Quản lý KTX)*  | Toàn quyền vận hành (`OPERATION plane`): Quản lý tòa/phòng/giường, Mở đợt & duyệt đơn, Chạy thuật toán Phân bổ chỗ ở (Dry-run & Commit), Hợp đồng lưu trú, Check-in/out, Điện nước, Phát hành hóa đơn, Xử lý sự cố, Kỷ luật vi phạm, Báo cáo Excel/PDF, Phân quyền cán bộ (`/manage/users`, `/manage/roles`). |
| **`staffA`** | `staffa@example.com` | **CAN_BO** (`ROLE_STAFF`) *(Cán bộ Tòa A)* | Phụ trách**Tòa A (Nam)**: Xem sơ đồ phòng Tòa A, làm thủ tục Check-in / Check-out, Ghi chỉ số điện nước tháng Tòa A, Tiếp nhận & xử lý ticket sự cố Tòa A, Lập biên bản vi phạm Tòa A. *(Bị chặn 403 nếu truy cập Tòa B).*                                                                                                |
| **`staffB`** | `staffb@example.com` | **CAN_BO** (`ROLE_STAFF`) *(Cán bộ Tòa B)* | Phụ trách**Tòa B (Nữ)**: Tương tự như `staffA` nhưng áp dụng riêng cho phạm vi Tòa B. *(Bị chặn 403 nếu truy cập Tòa A).*                                                                                                                                                                                                                |

### 6.2. Nhóm sinh viên phục vụ Demo Kịch bản chuẩn (15 phút)

| MSSV / Username          | Họ và tên         | Giới tính | Lớp      | Điểm UT | Mục đích & Tình huống kiểm thử                                                                                          |
| :----------------------- | :------------------- | :---------- | :-------- | :-------- | :----------------------------------------------------------------------------------------------------------------------------- |
| **`D22CQCN004`** | Vũ Hải Đăng      | Nam         | D22CQCN03 | 0         | **Dùng demo Nộp đơn mới**: ĐRL 100, chưa có đơn, nộp trực tiếp trên web vào Đợt Tân SV K22 (`OPEN`). |
| **`D22CQCN001`** | Nguyễn Văn Nam     | Nam         | D22CQCN01 | 1200      | Chính sách (1000đ) + Ở ngoan (200đ) →**Trúng tuyển Rank 1** Tòa A (Phòng A101, giường 101-1).                |
| **`D22CQCN003`** | Lê Hoàng Long      | Nam         | D22CQCN02 | 500       | Vùng sâu vùng xa (500đ) →**Trúng tuyển Rank 2** Tòa A.                                                           |
| **`D22CQCN002`** | Trần Văn Hùng     | Nam         | D22CQCN01 | 200       | Cùng lớp`D22CQCN01` với `D22CQCN001` → **Demo thuật toán Gom lớp** vào cùng phòng A101 (giường 101-2).   |
| **`D22CQDT001`** | Phạm Thị Lan       | Nữ         | D22CQDT01 | 200       | Nữ nộp đơn Tòa B → Trúng tuyển giường B101-1.                                                                        |
| **`D22CQDT002`** | Hoàng Thùy Linh    | Nữ         | D22CQDT01 | 0         | Nữ nộp sớm → Trúng tuyển giường B101-2.                                                                                |
| **`D22CQDT003`** | Nguyễn Mai Phương | Nữ         | D22CQDT02 | 0         | Nữ nộp sau, Tòa B hết chỗ trống →**Demo Hết chỗ / WAITLISTED**.                                                 |

### 6.3. Nhóm sinh viên phục vụ Đối soát Điện nước & Hóa đơn (Phụ lục B)

| MSSV / Username          | Phòng      | Tình trạng hóa đơn               | Công thức đối soát & Chi tiết                                                                                                                               |
| :----------------------- | :---------- | :------------------------------------ | :---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`D21CQCN010`** | A102 (5 SV) | **PAID** *(Đã thanh toán)* | **Case A (280 kWh, 18m³)**: Chia 5 người = 199.908đ + 50.000đ phụ phí = **249.908đ** (Phần dư dồn = 0đ). Đã lập phiếu thu tiền mặt. |
| `D21CQCN011` – `14` | A102 (5 SV) | **UNPAID** *(Chưa trả)*     | Các sinh viên cùng phòng A102, mỗi người 249.908đ.                                                                                                        |
| **`D21CQCN020`** | A103 (3 SV) | **OVERDUE** *(Quá hạn)*     | **Case B Residual**: Chia 3 người dư 2đ dồn cho min MSSV + Phạt chậm thanh toán 5% = **110.689đ**.                                           |
| `D21CQCN021` – `22` | A103 (3 SV) | **UNPAID** *(Chưa trả)*     | Các sinh viên cùng phòng A103: 55.416đ + 50.000đ phụ phí =**105.416đ**.                                                                            |

### 6.4. Nhóm sinh viên phục vụ Demo Kỷ luật & Trạng thái Hợp đồng

| MSSV / Username          | Trạng thái / Hợp đồng                        | Tình huống kiểm chứng trên hệ thống                                                               |
| :----------------------- | :------------------------------------------------ | :------------------------------------------------------------------------------------------------------- |
| **`D22CQCN098`** | Bị cấm ở KTX (`blocked_from_housing = true`) | Hệ thống khóa form nộp đơn; Thuật toán phân bổ tự động đánh dấu`SKIPPED_BLOCKED`.      |
| **`D22CQCN099`** | 0 điểm rèn luyện (`conduct_score = 0`)      | Bị lập biên bản trừ 100đ do phá hoại tài sản; Hệ thống chặn nộp đơn mới.                |
| **`D20CQCN001`** | HĐ**DRAFT** tại A104-1                    | Demo chức năng**Hủy DRAFT** nhả giường ngay lập tức về `VACANT`.                        |
| **`D20CQCN002`** | HĐ**TERMINATED** tại A104-2               | Hủy HĐ sau Check-in: Giường vẫn ở trạng thái`OCCUPIED` cho tới khi làm thủ tục Check-out.  |
| **`D20CQCN003`** | HĐ**EXPIRED** tại A105-1                  | Hết hạn hợp đồng kỳ trước: Giường vẫn giữ`OCCUPIED` cho tới khi làm thủ tục Check-out. |

---

## 7. Kịch bản Demo 15 phút (Buổi chấm BTL)

Luồng nghiệp vụ khép kín từ lúc sinh viên nộp đơn đến khi phân bổ, check-in, tính điện nước và thanh toán hóa đơn:

```
[1. Sinh viên: Nộp đơn] ──> [2. Quản lý: Preview Dry-run] ──> [3. Quản lý: Commit tạo HĐ]
                                                                      │
[6. Sinh viên: Hóa đơn] <── [5. Staff: Ghi số điện nước] <── [4. Staff: Check-in]
```

### Bước 1: Sinh viên nộp đơn đăng ký chỗ ở (2 phút)

1. Đăng nhập tài khoản sinh viên: `D22CQCN004` / `Admin@123`.
2. Vào menu **Đơn đăng ký** (`/student/applications`) → Bấm **Nộp đơn mới**.
3. Chọn đợt đang mở: *Đợt Đăng ký Tân SV K22 (Đang nhận đơn)*.
4. Chọn nguyện vọng: Tòa A (Nam) · Loại phòng: `STANDARD_4`. Bấm **Gửi đơn**.
5. **Kiểm chứng**: Đơn được ghi nhận ở trạng thái `SUBMITTED`.
6. *(Tùy chọn kiểm tra ràng buộc)*: Đăng xuất, thử đăng nhập bằng `D22CQCN098` (bị cấm) hoặc `D22CQCN099` (0 điểm rèn luyện) → Hệ thống hiển thị cảnh báo đỏ và chặn nộp đơn.

### Bước 2: Quản lý chạy xem trước phân bổ (Dry-run Preview) (3 phút)

1. Đăng xuất, đăng nhập tài khoản Quản lý: `quanly` / `Admin@123`.
2. Vào menu **Phân bổ chỗ ở** (`/admin/allocations`).
3. Chọn: *Đợt 1 — Đăng ký KTX Học kỳ 1 (2026-2027)* (đợt đã đóng nhận đơn).
4. Bấm nút **Chạy xem trước (Dry-run)**:
   - Thuật toán `AllocationEngine` tính điểm deterministic theo trọng số cấu hình.
5. **Kiểm chứng bảng kết quả**:
   - **Xếp hạng điểm**: `D22CQCN001` (1200đ - Rank 1), `D22CQCN003` (500đ - Rank 2), `D22CQCN002` (200đ - Rank 3).
   - **Cơ chế Gom lớp**: `D22CQCN002` cùng lớp `D22CQCN01` với `D22CQCN001` được ưu tiên xếp chung phòng A101 (giường 101-2).
   - **Cơ chế Waitlist**: Tòa B chỉ còn 2 giường trống nhưng có 3 nữ nộp đơn. `D22CQDT001` và `D22CQDT002` trúng tuyển; `D22CQDT003` nộp sau rơi vào danh sách chờ **WAITLISTED (NO_VACANT_BED)**.
   - **Cơ chế Kỷ luật**: `D22CQCN098` tự động bị loại bỏ với lý do **SKIPPED (SKIPPED_BLOCKED)**.
   - **Tính an toàn của Dry-run**: Trạng thái các giường trong hệ thống vẫn là `VACANT`, chưa bị ghi đè.

### Bước 3: Chốt phân bổ & Sinh Hợp đồng (Commit Allocation) (2 phút)

1. Tại trang chi tiết lượt phân bổ, bấm **Chốt phân bổ (Commit)** và xác nhận modal.
2. **Kiểm chứng**:
   - Các đơn trúng tuyển chuyển sang `APPROVED`, tạo hợp đồng ở trạng thái **DRAFT**.
   - Các đơn không còn chỗ chuyển sang **WAITLISTED**.
   - Giường trúng tuyển chuyển sang **OCCUPIED** và lưu `current_contract_id`.
3. *(Demo hủy nháp)*: Bấm **Hủy DRAFT** trên một hợp đồng nháp → Giường lập tức nhả về trạng thái **VACANT**.

### Bước 4: Thủ tục nhận phòng (Check-in) & Hợp đồng (2 phút)

1. Đăng xuất, đăng nhập tài khoản Cán bộ Tòa A: `staffA` / `Admin@123`.
2. Cán bộ thực hiện Check-in cho sinh viên:
   - Hợp đồng chuyển từ `DRAFT` sang `ACTIVE`.
   - Bàn giao chìa khóa, lập biên bản ghi nhận tình trạng tài sản phòng.
3. **Kiểm chứng quy tắc chiếm giường**:
   - Xem phòng A104: Sinh viên `D20CQCN002` có HĐ `TERMINATED` nhưng chưa làm thủ tục Check-out nên giường vẫn là `OCCUPIED`.
   - Xem phòng A105: Sinh viên `D20CQCN003` có HĐ `EXPIRED` vẫn chiếm giường cho tới khi Check-out.

### Bước 5: Ghi chỉ số Điện nước (2 phút)

1. Cán bộ `staffA` vào menu **Điện nước** (`/staff/utilities` hoặc `/admin/utilities`):
   - **Phòng A102 (5 người)**: Chỉ số điện cũ 1000, mới 1280 (tiêu thụ 280 kWh). Chỉ số nước cũ 50, mới 68 (tiêu thụ 18 m³).
   - **Phòng A103 (3 người)**: Chỉ số điện cũ 100, mới 151 (tiêu thụ 51 kWh). Chỉ số nước cũ 20, mới 21 (tiêu thụ 1 m³).

### Bước 6: Đối soát & Thanh toán Hóa đơn (2 phút)

1. Đăng xuất, đăng nhập sinh viên `D21CQCN010` (Phòng A102 - Case A):
   - Vào menu **Hóa đơn & Tiền phòng** (`/student/invoices`).
   - Kiểm tra hóa đơn điện nước tháng 09/2026: Trạng thái **PAID** (249.908đ), xem chi tiết các khoản điện 6 bậc, nước, internet, rác, xe.
2. Đăng xuất, đăng nhập sinh viên `D21CQCN020` (Phòng A103 - Case B):
   - Kiểm tra hóa đơn quá hạn: Trạng thái **OVERDUE** (110.689đ đã bao gồm 5% tiền phạt chậm).

### Bước 7: Quản trị kỹ thuật & Nhật ký Kiểm toán Audit Log (2 phút)

1. Đăng nhập tài khoản Quản trị hệ thống: `admin` / `Admin@123`.
2. Vào **Cấu hình hệ thống** (`/admin/configs`): Xem và chỉnh sửa biểu giá điện nước, trọng số thuật toán phân bổ.
3. Vào **Nhật ký hệ thống** (`/admin/logs`): Xem toàn bộ lịch sử đăng nhập, thay đổi cấu hình, phân quyền với đầy đủ IP, thời gian, tác nhân và hành động.

---

## 8. Bản đồ màn hình & Phân quyền truy cập

| Đường dẫn (URL)                         | Vai trò được phép             | Chức năng thực hiện                                                            |
| :------------------------------------------ | :--------------------------------- | :--------------------------------------------------------------------------------- |
| `/login`, `/register`                   | Khách (Public)                    | Đăng nhập hệ thống, đăng ký tài khoản sinh viên mới                    |
| `/admin/configs`                          | `admin` (SYSTEM_ADMIN)           | Quản lý tham số hệ thống, biểu giá điện nước, trọng số phân bổ      |
| `/admin/accounts`                         | `admin` (SYSTEM_ADMIN)           | Quản lý tài khoản quản trị nội bộ                                          |
| `/admin/logs`                             | `admin` (SYSTEM_ADMIN)           | Tra cứu nhật ký an ninh, vết kiểm toán hệ thống (Audit Logs)               |
| `/manage/dashboard`                       | `quanly`, `staffA`, `staffB` | Cổng điều hành chung: KPI phòng ở, việc cần xử lý trong ngày            |
| `/admin/buildings`, `/admin/rooms`      | `quanly`                         | Quản lý tòa nhà, sinh phòng hàng loạt, quản lý giường & tài sản       |
| `/admin/periods`, `/admin/applications` | `quanly`                         | Quản lý đợt đăng ký, xem & duyệt đơn đăng ký của sinh viên          |
| `/admin/allocations`                      | `quanly`                         | Chạy thuật toán phân bổ chỗ ở: Dry-run preview, commit, phân bổ bù       |
| `/admin/contracts`                        | `quanly`                         | Danh sách hợp đồng lưu trú toàn trường, xử lý gia hạn, chuyển phòng  |
| `/admin/invoices`, `/admin/payments`    | `quanly`                         | Quản lý hóa đơn tiền phòng/điện nước, ghi nhận phiếu thu              |
| `/admin/reports`                          | `quanly`                         | Báo cáo doanh thu, công nợ, tỷ lệ lấp đầy; xuất file Excel và PDF       |
| `/manage/users`, `/manage/roles`        | `quanly`                         | Quản lý danh sách cán bộ, phân quyền vai trò động theo RBAC              |
| `/staff/buildings/rooms`                  | `staffA`, `staffB`             | Sơ đồ phòng trực quan theo tòa được phân công                           |
| `/staff/check-in-out`                     | `staffA`, `staffB`             | Làm thủ tục bàn giao nhận phòng (Check-in) và trả phòng (Check-out)       |
| `/staff/utilities`                        | `staffA`, `staffB`             | Sổ ghi chỉ số công tơ điện nước tháng theo tòa phụ trách              |
| `/staff/tickets`, `/staff/violations`   | `staffA`, `staffB`             | Tiếp nhận sửa chữa hỏng hóc, lập biên bản vi phạm kỷ luật sinh viên   |
| `/student/dashboard`                      | Sinh viên (`STUDENT`)           | Trang chủ sinh viên: Thông tin phòng đang ở, thông báo, trạng thái đơn |
| `/student/applications`                   | Sinh viên (`STUDENT`)           | Nộp đơn đăng ký phòng ở mới, theo dõi trạng thái duyệt đơn          |
| `/student/invoices`                       | Sinh viên (`STUDENT`)           | Tra cứu hóa đơn tiền phòng & điện nước, lịch sử nộp tiền             |
| `/student/tickets`                        | Sinh viên (`STUDENT`)           | Gửi phản ánh, yêu cầu sửa chữa trang thiết bị hư hỏng                   |
| `/student/profile`, `/student/password` | Sinh viên (`STUDENT`)           | Cập nhật thông tin liên lạc cá nhân, đổi mật khẩu tài khoản           |

> [!NOTE]
> - Thời gian hết hạn phiên làm việc (Session timeout) là **30 phút**.
> - Hệ thống áp dụng chính sách `maximumSessions(1)`: Đăng nhập trên thiết bị mới sẽ làm hết hiệu lực phiên làm việc cũ.
> - Nếu đăng nhập sai mật khẩu quá **5 lần liên tiếp**, tài khoản sẽ bị tạm khóa **10 phút** để chống tấn công brute-force.

---

## 9. Xử lý các sự cố thường gặp (Troubleshooting)

| Hiện tượng                                                          | Nguyên nhân                                             | Cách khắc phục                                                                                                                                             |
| :--------------------------------------------------------------------- | :-------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Communications link failure`                                        | MySQL chưa bật hoặc sai port 3306                      | Khởi động dịch vụ MySQL trên máy hoặc qua Docker container.                                                                                           |
| `Access denied for user 'ktx'@'localhost'`                           | Chưa tạo user DB hoặc sai mật khẩu                   | Chạy script tạo user và cấp quyền ở Mục 4.                                                                                                             |
| Lỗi Flyway:`Migration checksum mismatch`                            | Schema DB đã bị thay đổi thủ công                  | Chạy`DROP DATABASE ktx; CREATE DATABASE ktx...` rồi khởi động lại app profile `dev`.                                                                |
| Đăng nhập tài khoản`admin` nhưng không thấy menu Tòa/Phòng | Phân quyền RBAC đã tách bạch hai vai trò           | Đăng nhập bằng tài khoản**`quanly`** (`Admin@123`) để quản lý vận hành KTX. Tài khoản `admin` dành cho quản trị hệ thống kỹ thuật. |
| `staffA` không xem được Tòa B (Lỗi 403 Forbidden)              | Tính năng bảo mật phân vùng dữ liệu theo cán bộ | Đúng theo thiết kế:`staffA` chỉ quản lý Tòa A. Đăng nhập bằng **`staffB`** để thao tác trên Tòa B.                                 |
| Tài khoản bị thông báo "Tạm khóa 10 phút"                      | Gõ sai mật khẩu quá 5 lần                            | Đợi hết 10 phút hoặc khởi động lại ứng dụng (bộ đếm lưu trên bộ nhớ RAM).                                                                   |
| Lỗi`java.lang.UnsupportedClassVersionError`                         | Đang dùng JDK cũ hơn 25                               | Cài đặt JDK 25 và trỏ lại biến môi trường`JAVA_HOME`.                                                                                             |

---

## 10. Tài liệu kỹ thuật chi tiết

Khi cần tìm hiểu sâu hơn về kiến trúc và mã nguồn, tham khảo các tài liệu trong thư mục [`docs/`](./docs/):

- [`docs/01-tong-quan.md`](./docs/01-tong-quan.md): Bối cảnh nghiệp vụ và mục tiêu hệ thống.
- [`docs/03-mo-hinh-du-lieu.md`](./docs/03-mo-hinh-du-lieu.md): Mô hình thực thể ERD, quan hệ chiếm giường `OCCUPYING`.
- [`docs/04-01-xac-thuc.md`](./docs/04-01-xac-thuc.md): Cơ chế xác thực, phân quyền RBAC và Audit Log.
- [`docs/04-03-phan-bo.md`](./docs/04-03-phan-bo.md): Thuật toán phân bổ chỗ ở `AllocationEngine`, công thức tính điểm và gom lớp.
- [`docs/04-05-dien-nuoc.md`](./docs/04-05-dien-nuoc.md): Thuật toán chia điện nước 6 bậc EVN và xử lý phần dư.
- [`docs/11-phu-luc.md`](./docs/11-phu-luc.md): Chi tiết seed data và bảng đối soát chuẩn.
