> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module xác thực](./04-01-xac-thuc.md) · [Mục lục](./README.md) · [Module phân bổ →](./04-03-phan-bo.md)

# 6.2. Module 2 — Hạ tầng KTX & phòng ở

**Actors:** Admin (ghi mọi tòa), Staff (chỉ tòa `assigned_building_id` — `StaffScope`), Student (chỉ xem phòng/giường của mình qua hợp đồng).

**User stories:**

- Admin thêm tòa A Nam, B Nữ; thêm phòng 101 loại 6 giường giá 1.800.000đ/kỳ; hệ thống sinh 6 giường VACANT.
- Admin đánh dấu giường G3 bảo trì khi gãy.
- Admin kê khai quạt, tủ, công tơ theo phòng.
- Staff xem sơ đồ tầng tòa được gán.

**Quy tắc:**

- `Building.gender_policy` bất biến nếu tòa còn hợp đồng **OCCUPYING** (đổi giới tính chỉ khi trống toàn bộ).
- Tạo `Room`: bắt buộc chọn `RoomType` → `capacity` map cố định: `STANDARD_4→4`, `STANDARD_6→6`, `STANDARD_8→8`, `VIP_AC→2` (đề xuất mặc định, cấu hình JSON `room.type.vip.capacity`).
- `RoomService.create` tạo đủ `Bed` `G1..Gn`. Không cho xóa phòng nếu còn bed OCCUPIED.
- Đổi `Room.status=MAINTENANCE` chỉ khi mọi bed không OCCUPIED, hoặc admin force (nhả hợp đồng trước).
- `Bed.status=MAINTENANCE` không được chọn bởi engine.
- Giá phòng là **theo kỳ** (`price_per_term`), snapshot vào contract lúc tạo.

**Trạng thái giường:** xem state machine [mục 9](./06-may-trang-thai.md).

**URL admin:**

| Method   | Path                                                                           |
| -------- | ------------------------------------------------------------------------------ |
| GET/POST | `/admin/buildings`, `/admin/buildings/new`, `/admin/buildings/{id}/edit` |
| GET/POST | `/admin/rooms`, `/admin/rooms/new`, `/admin/rooms/{id}/edit`             |
| POST     | `/admin/rooms/{id}/beds/{bedId}/status`                                      |
| GET/POST | `/admin/rooms/{id}/assets`                                                   |
| GET      | `/admin/rooms/{id}` (chi tiết + occupancy)                                  |

Staff: `/staff/buildings/{id}/rooms` (read), POST status bed/asset **chỉ** khi `id == assigned_building_id`. Mọi query staff đi qua `StaffScope.assertBuilding` / `assertRoom` (class có từ PR-05).

**Service:** `BuildingService`, `RoomService`, `BedService`, `AssetService`, `StaffScope`.

**Edge cases:**

- Số bed hiện có ≠ capacity (do xóa tay) → `RoomService.reconcileBeds` cảnh báo trên UI, không tự xóa OCCUPIED.
- Drift cache giường vs HĐ → `RoomService.reconcileOccupancy` (admin).
- Hai admin sửa cùng bed: `@Version` + flash “Dữ liệu đã thay đổi”.
- Phòng VIP cùng tòa Nam/Nữ — engine vẫn lọc giới tính theo tòa.

---
