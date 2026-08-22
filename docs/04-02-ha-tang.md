> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module xác thực](./04-01-xac-thuc.md) · [Mục lục](./README.md) · [Module phân bổ →](./04-03-phan-bo.md)

# 6.2. Module 2 — Hạ tầng KTX & phòng ở

**Actors:** Admin (ghi mọi tòa), Staff (chỉ tòa `assigned_building_id` — `StaffScope`), Student (chỉ xem phòng/giường của mình qua hợp đồng).

**User stories:**

- Admin thêm tòa A Nam, B Nữ; thêm phòng 101 loại 6 giường giá 1.800.000đ/kỳ; hệ thống sinh 6 giường VACANT.
- Admin sinh hàng loạt: Tòa A, tầng 1→5, 10 phòng/tầng, loại 6 giường → 50 phòng (`101–110` … `501–510`) + giường `G1…G6` VACANT; số đã có trong tòa bị bỏ qua.
- Admin đánh dấu giường G3 bảo trì khi gãy.
- Admin kê khai quạt, tủ, công tơ theo phòng.
- Staff xem sơ đồ tầng tòa được gán.

**Quy tắc:**

- `Building.gender_policy` bất biến nếu tòa còn hợp đồng **OCCUPYING** (đổi giới tính chỉ khi trống toàn bộ).
- Tạo `Room`: bắt buộc chọn `RoomType` → `capacity` map cố định: `STANDARD_4→4`, `STANDARD_6→6`, `STANDARD_8→8`, `VIP_AC→2` (đề xuất mặc định, cấu hình JSON `room.type.vip.capacity`).
- `RoomService.create` tạo đủ `Bed` `G1..Gn`. Không cho xóa phòng nếu còn bed OCCUPIED.
- `RoomService.createBatch` (form `/admin/rooms/batch`): một transaction, `saveAll` phòng + giường.
- Đổi `Room.status=MAINTENANCE` chỉ khi mọi bed không OCCUPIED, hoặc admin force (nhả hợp đồng trước).
- `Bed.status=MAINTENANCE` không được chọn bởi engine.
- Giá phòng là **theo kỳ** (`price_per_term`), snapshot vào contract lúc tạo. **Không** có field cọc trên phòng — cọc lúc lập hợp đồng.

### 6.2.1. Sinh phòng hàng loạt (batch)

**Việc:** Admin không nhập từng phòng khi tòa mới. Điền tòa, tầng từ → đến, số phòng mỗi tầng, loại, giá/kỳ, trạng thái → backend sinh danh sách.

**Quy luật số phòng:** `{tầng}{số thứ tự}` với `roomNumber = floor * 100 + seq` (seq từ 1). Tầng 1 × 10 phòng → `101`…`110`; tầng 2 → `201`…`210`. Mã cửa UI = `{building.code}-{roomNumber}` (`A-101`). Giới tính lấy từ tòa, không nhập trên form phòng.

**Trần / validate:**

| Ràng buộc | Giá trị |
| --- | --- |
| Tầng từ / đến | 1–30, đến ≥ từ |
| Phòng mỗi tầng | 1–20 (tránh đụng số tầng sau: seq ≤ 99) |
| Tổng phòng một lần | ≤ 100 (`RoomService.MAX_BATCH_ROOMS`) |
| Số đã có trong cùng tòa | **bỏ qua**, không fail cả dải; flash liệt kê |
| Mọi số đều đã có | lỗi `BATCH_EMPTY`, không ghi DB |

**Ngoài phạm vi v1:** import Excel / Apache POI. POI dành [PR-15 export báo cáo](./12-pr-plan.md).

**DTO:** `RoomBatchForm`, `RoomBatchResult`. Template: `admin/rooms/batch.html`. Nút **Thêm hàng loạt** trên `admin/rooms/list.html`.

**Trạng thái giường:** xem state machine [mục 9](./06-may-trang-thai.md).

**URL admin:**

| Method   | Path                                                                           |
| -------- | ------------------------------------------------------------------------------ |
| GET/POST | `/admin/buildings`, `/admin/buildings/new`, `/admin/buildings/{id}/edit` |
| GET/POST | `/admin/rooms`, `/admin/rooms/new`, `/admin/rooms/batch`, `/admin/rooms/{id}/edit` |
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
