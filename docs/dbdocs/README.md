# Hướng Dẫn Sử Dụng Tài Liệu CSDL (dbdocs & dbdiagram)

Tài liệu này hướng dẫn cách xem, kiểm tra cú pháp và phát hành tài liệu cơ sở dữ liệu dự án **Quản lý Ký túc xá (`BTL_Java_Web_QlyKiTucXa`)** lên nền tảng [dbdocs.io](https://dbdocs.io/) và [dbdiagram.io](https://dbdiagram.io/).

---

## 1. Cấu trúc Schema DBML

Tệp cấu hình chính:

- **[schema.dbml](../../schema.dbml)** (tại thư mục gốc dự án)
- **[docs/dbdocs/schema.dbml](./schema.dbml)** (bản lưu trữ trong thư mục tài liệu)

Dữ liệu được cập nhật đầy đủ nhất theo các bản migration Flyway **V1 -> V6**, bao gồm:

* **31 bảng CSDL**: Phủ kín toàn bộ các phân hệ từ tài khoản, cơ sở vật chất, hợp đồng, điện nước hóa đơn, xử lý vi phạm, đến kiểm toán audit log và cấu hình hệ thống.
* **37 Enums**: Chuẩn hóa toàn bộ các trạng thái và danh mục phân loại theo domain Java `com.ktx.domain.enums.*`.
* **Ràng buộc toàn vẹn OCCUPYING**: Quy định chặt chẽ về chiếm chỗ giường và sinh viên thông qua các trường Generated Stored Unique (`active_bed_key`, `active_student_key`).
* **Chuẩn hóa 3NF (V6)**: Khử trùng lặp `student_id` tại các bảng trung gian (`allocation_items`, `room_change_requests`, `renewal_requests`) và sinh tự động `invoices.total = subtotal + late_fee`.
* **7 Phân nhóm TableGroup**:
  1. `TaiKhoan_PhanQuyen_RBAC` (Tài khoản, sinh viên, cán bộ, vai trò, quyền hạn RBAC)
  2. `CoSoVatChat_HaTang` (Tòa nhà, phòng ở, giường, tài sản thiết bị)
  3. `DangKy_PhanBoPhong` (Đợt mở đăng ký, đơn đăng ký, phiên chạy và kết quả phân bổ)
  4. `HopDong_LuuTru` (Hợp đồng lưu trú, check-in/out, đổi phòng, gia hạn hợp đồng)
  5. `DienNuoc_HoaDon_ThanhToan` (Chỉ số điện nước, hóa đơn, dòng chi tiết, giao dịch thanh toán)
  6. `VanHanh_HoTro_ViPham` (Báo hỏng ticket, biên bản vi phạm, thông báo người dùng)
  7. `HeThong_CauHinh_NhatKy` (19 tham số cấu hình, cấp số văn bản, khóa bi quan, audit logs)

---

## 2. Cách xem và trực quan hóa sơ đồ

### Cách 1: Xem trực tiếp trên dbdiagram.io (Nhanh nhất - Không cần cài đặt)

1. Truy cập [https://dbdiagram.io/d](https://dbdiagram.io/d).
2. Mở file `schema.dbml` trong dự án, copy toàn bộ nội dung và dán vào khung soạn thảo bên trái.
3. dbdiagram sẽ tự động dựng sơ đồ ERD trực quan, liên kết quan hệ khóa ngoại và hiển thị phân nhóm màu sắc theo các `TableGroup`.

---

### Cách 2: Phát hành tài liệu web động qua CLI dbdocs.io

#### Bước 1: Kiểm tra hợp lệ file DBML (Validate)

Chạy lệnh sau tại thư mục gốc của dự án:

```powershell
npx dbdocs validate schema.dbml
```

Khi thành công, màn hình sẽ thông báo: `√ Done. Parse succeeded without errors.`

#### Bước 2: Đăng nhập tài khoản dbdocs

Nếu chưa đăng nhập:

```powershell
npx dbdocs login
```

Hệ thống sẽ yêu cầu nhập Email để nhận mã xác thực đăng nhập một lần (OTP).

#### Bước 3: Build và phát hành tài liệu

Chạy lệnh sau để phát hành tài liệu lên cloud dbdocs:

```powershell
# Chế độ công khai (Public)
npx dbdocs build schema.dbml --project "BTL_Java_Web_QlyKiTucXa" --public

# Hoặc đặt mật khẩu bảo vệ tài liệu
npx dbdocs build schema.dbml --project "BTL_Java_Web_QlyKiTucXa" -p "ktx2026"
```

Sau khi hoàn tất, dbdocs sẽ cung cấp đường link tài liệu web tương tác (ví dụ: `https://dbdocs.io/<username>/BTL_Java_Web_QlyKiTucXa`).

#### Bước 4: Đổi mật khẩu hoặc quản lý quyền truy cập

```powershell
# Đặt mật khẩu mới
npx dbdocs password --project "BTL_Java_Web_QlyKiTucXa" -p "mat_khau_moi"

# Gỡ bỏ mật khẩu (chuyển sang public)
npx dbdocs password --project "BTL_Java_Web_QlyKiTucXa" --remove
```

---

## 3. Cập nhật khi có thay đổi CSDL

Khi có bản migration Flyway mới (V7, V8...):

1. Cập nhật các bảng, cột và enum tương ứng vào file [schema.dbml](../../schema.dbml).
2. Chạy `npx dbdocs validate schema.dbml` để kiểm tra cú pháp.
3. Chạy `npx dbdocs build schema.dbml --project "BTL_Java_Web_QlyKiTucXa"` để đồng bộ phiên bản mới lên web.
