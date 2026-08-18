> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module hợp đồng](./04-04-hop-dong.md) · [Mục lục](./README.md) · [Module sự cố →](./04-06-su-co-vi-pham.md)

# 6.5. Module 5 — Điện, nước & hóa đơn

**Actors:** Staff/Admin ghi chỉ số. Staff phát hành hóa đơn **một phòng** trong tòa `StaffScope`. Admin phát hành **cả tòa** (hoặc một phòng bất kỳ). Student xem; thanh toán tại quầy — staff/admin ghi `Payment`.

## 6.5.1. Công thức — số liệu mẫu (không phải giá EVN chính thức)

Cấu hình JSON `billing.electricity.tiers`:

| Bậc | Sản lượng | Đơn giá mẫu (đ/kWh) |
| ---- | ------------ | ------------------------ |
| 1    | 0–50        | 1.984                    |
| 2    | 51–100      | 2.050                    |
| 3    | 101–200     | 2.380                    |
| 4    | 201–300     | 2.998                    |
| 5    | 301–400     | 3.350                    |
| 6    | ≥ 401       | 3.460                    |

Nước: `billing.water.price_per_m3 = 15000` (cố định).

Phụ phí **theo tháng** (đề xuất mặc định, cấu hình):

| Key                                   | Mặc định | Cách chia                                                                             |
| ------------------------------------- | ----------- | -------------------------------------------------------------------------------------- |
| `billing.fee.sanitation_per_person` | 20.000      | nhân số người                                                                      |
| `billing.fee.internet_per_room`     | 50.000      | chia đều                                                                             |
| `billing.fee.parking_per_person`    | 30.000      | nhân số người (ai đăng ký xe — v1:**tính mọi người**; open question) |

Tiền phòng: thu **một lần theo kỳ** khi check-in (invoice `ROOM_TERM`). Key `billing.room.split_monthly` mặc định `false` và **v1 không triển khai** nhánh `true` (không tách tiền phòng thành hóa đơn tháng).

**Tiêu thụ điện/nước** (hai cờ độc lập):

```
kWh:
  nếu elec_replaced:
      bắt buộc elec_old_final, elec_new_start, elec_curr
      kWh = (elec_old_final - elec_prev) + (elec_curr - elec_new_start)
  else:
      kWh = elec_curr - elec_prev
      yêu cầu elec_curr >= elec_prev (trừ new_building_meter: prev=0)

m3: tương tự với water_replaced / water_old_final / water_new_start / water_curr / water_prev

Từ chối lập hóa đơn nếu kWh hoặc m3 null / âm.
Thay một công tơ không bắt buộc bốn chỉ số của công tơ kia.
```

**Chia đều:** `N` = số hợp đồng **OCCUPYING** có giường thuộc phòng **tại lúc bấm phát hành** (click = “chốt”). V1 **không** as-of ngày cuối tháng, **không** bảng `occupancy_snapshots`. Staff nên lập hóa đơn ngay sau khi khóa sổ chỉ số tháng T. `N=0` → không lập hóa đơn utility cá nhân.

Toàn bộ tiền tính **số nguyên VND**. Không `HALF_UP` trên thương số rồi cộng residual (residual có thể âm).

```
elec_total    = tiered(kWh)          // bậc thang, từng bậc nhân nguyên
water_total   = m3 * water_price
divisible     = elec_total + water_total + internet_per_room
share         = floor(divisible / N)           // nguyên
residual      = divisible - share * N          // luôn 0..N-1
sanitation, parking giữ nguyên per-person
```

Với mỗi SV trong phòng, sort `student_id ASC`; `minId` = nhỏ nhất:

```
room_part = share + (student.id == minId ? residual : 0)
subtotal  = room_part + sanitation_per_person + parking_per_person
late_fee  = 0 khi phát hành
total     = subtotal + late_fee
```

Suy dòng `InvoiceItem` sao cho `sum(items) = total` (làm tròn **một lần** ở nồi `divisible`):

```
nếu divisible == 0: ELEC=WATER=INTERNET=0
else:
  elec_line = floor(elec_total * room_part / divisible)
  water_line = floor(water_total * room_part / divisible)
  inet_line  = room_part - elec_line - water_line   // dòng cuối hút phần dư
+ SANITATION + PARKING
(+ LATE_FEE khi đã áp, xem dưới)
```

**Phí trễ (idempotent):** `late_fee = ceil(subtotal * billing.late.rate)` (rate mặc định 0.05). Ghi cột `late_fee`, `total = subtotal + late_fee`, thêm item `LATE_FEE`. Áp **tối đa một lần**: nếu `late_fee > 0` rồi → no-op. Job `InvoiceOverdueJob` **hoặc** lần staff mở hóa đơn quá hạn đều gọi `BillingEngine.applyLateFees` cùng predicate. Không nhân `total` đã gồm phí (tránh vòng). `billing.due.days = 10` sau ngày phát hành; quá hạn → `OVERDUE` rồi mới xét phí.

## 6.5.2. Ví dụ số

**Case A — chia hết** (oracle `BillingEngineTest.evenSplit280kWh`): phòng 6 giường, tháng 9, điện 280 kWh, nước 18 m³, N = 5.

```
Điện:
  50×1.984 = 99.200
  50×2.050 = 102.500
 100×2.380 = 238.000
  80×2.998 = 239.840
  Tổng điện = 679.540
Nước: 18×15.000 = 270.000
Internet phòng = 50.000
divisible = 999.540
share = floor(999.540 / 5) = 199.908    residual = 0
Vệ sinh 20.000 + xe 30.000 = 50.000
Mỗi SV subtotal = 249.908 đ
5 × 249.908 = 1.249.540 = (999.540 + 5×50.000) ✓
```

**Case B — dư đồng** (oracle `BillingEngineTest.residualSplit`): điện 51 kWh, nước 1 m³, internet 50.000, N = 3, `student_id` = 10, 20, 30.

```
Điện: 50×1.984 + 1×2.050 = 101.250
Nước: 15.000
divisible = 101.250 + 15.000 + 50.000 = 166.250
share = floor(166.250 / 3) = 55.416
residual = 166.250 − 166.248 = 2          (luôn ≥ 0)
SV 10 (min): room_part = 55.418
SV 20, 30:   room_part = 55.416
Mỗi người + 50.000 phụ phí
subtotal: 105.418 + 105.416 + 105.416 = 316.250
         = 166.250 + 3×50.000 ✓
```

Dòng item SV 10 (min), `room_part=55418`, `divisible=166250`:

```
ELEC      = floor(101250 * 55418 / 166250) = 33.750
WATER     = floor( 15000 * 55418 / 166250) =  5.000
INTERNET  = 55.418 − 33.750 − 5.000        = 16.668
SANITATION= 20.000
PARKING   = 30.000
sum items = 105.418 = subtotal
```

`late_fee` ví dụ: `subtotal=105.418`, rate 5% → `ceil(5270,9)=5.271`; `total=110.689`; không cộng lần hai.

Dấu `.` là phân tách hàng nghìn kiểu VN.

## 6.5.3. Luồng phát hành

1. Staff nhập reading tháng T (prev mặc định = curr tháng T-1).
2. Staff: `POST /staff/readings/{roomId}/issue` — đúng một phòng trong `StaffScope`. Admin: `POST /admin/invoices/generate?buildingId=` (cả tòa) hoặc `?roomId=` (một phòng).
3. `BillingEngine.issueUtilityInvoices(roomId, yearMonth)`:
   - Đã có hóa đơn cùng logical key và `status != CANCELLED` → bỏ qua.
   - Chỉ còn hàng `CANCELLED` → `InvoiceService.cancel` đã đổi key thành `{old}:cancelled:{id}` lúc hủy; insert hóa đơn mới với logical key gốc.
   - `N` lấy OCCUPYING **lúc gọi hàm**.
4. `InvoiceOverdueJob` đêm: UNPAID quá `due_date` → `OVERDUE` rồi `applyLateFees` (no-op nếu `late_fee > 0`).

Tiền phòng: `BillingEngine.issueRoomFee(contractId)` khi check-in. Cọc: `BillingEngine.issueDeposit(contractId)` cùng lúc. Cùng quy tắc tombstone nếu admin hủy rồi phát hành lại.

**URL:** `/staff/readings`, `POST /staff/readings/{roomId}/issue`, `/admin/readings`, `/admin/invoices/generate`, `/admin/invoices`, `/admin/payments`, `/student/invoices`.

**Edge cases:** `curr < prev` khi cờ replaced tương ứng = false → reject; `elec_replaced` thiếu `elec_old_final` → reject (nước không bị kéo theo); không prorate v1; reading trùng unique → update nếu chưa phát hành, khóa nếu đã có invoice chưa hủy.

---
