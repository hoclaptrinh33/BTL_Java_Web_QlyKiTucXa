> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module phân bổ](./04-03-phan-bo.md) · [Mục lục](./README.md) · [Module điện nước →](./04-05-dien-nuoc.md)

# 6.4. Module 4 — Hợp đồng & hồ sơ lưu trú

**Actors:** Admin (tạo/duyệt), Staff (check-in/out tòa mình), Student (xem, xin gia hạn).

**Stories:**

- Sau commit, hệ thống sinh `contract_no`, trạng thái DRAFT, tiền phòng snapshot, cọc = **50% giá kỳ**.
- Staff check-in: xác nhận nhận phòng + tình trạng tài sản → ACTIVE.
- Trước 30 ngày hết hạn, job tạo notification (+ email nếu bật).
- Sinh viên nộp đơn gia hạn; admin duyệt → kéo `end_date` thêm 1 kỳ, `PENDING_RENEWAL` → `ACTIVE`. Từ chối/hủy đơn → HĐ về `ACTIVE` (cùng `end_date`).
- Check-out: bàn giao tài sản, quyết định hoàn cọc / trừ hư hỏng.

**Quy tắc:**

- **Đặt cọc (đã chốt chủ đồ án, 2026-08-18):** 50% giá phòng/kỳ. `deposit_amount = HALF_UP(price_per_term * contract.deposit.ratio)` ra **VND nguyên**. `contract.deposit.ratio = 0.5`. `contract.term.months = 5` chỉ mô tả độ dài kỳ (không tham gia công thức cọc). Ví dụ: phòng 1.200.000 đ/kỳ → cọc 600.000 đ; VIP 4.000.000 → cọc 2.000.000.
- `contract_no` / `invoice_no` từ `document_sequences` (không `MAX()+1`).
- Không `ACTIVE` nếu bed lệch hoặc giới tính lệch (guard).
- Check-in **chỉ** từ `DRAFT` → `ACTIVE`.
- **Hủy nháp:** `DRAFT → COMPLETED` + `completion_reason=CANCELLED_BEFORE_CHECKIN` trong cùng TX: bed `VACANT`, xóa `current_contract_id`. Không dòng checkout. `TERMINATED` **không** dùng cho case này (để unique key giải phóng).
- Checkout từ `ACTIVE | EXPIRED | TERMINATED` → `COMPLETED`.
- `PENDING_RENEWAL → EXPIRED` khi job thấy `end_date < today` và đơn gia hạn vẫn `SUBMITTED`; giường vẫn chiếm. Path timeout **không** đổi khi reject/cancel.
- Gia hạn: bảng `renewal_requests` (V1). Duyệt → `end_date = requested_end`, `PENDING_RENEWAL → ACTIVE`. **REJECTED / CANCELLED** → HĐ `PENDING_RENEWAL → ACTIVE`, giữ `end_date` cũ (SV trả phòng / checkout được). Không tạo HĐ mới, không chạy engine.
- Thông báo: `contract.expiry.remind.days = 30`.
- SEVERE / điểm 0: `ContractService.terminate` (`ACTIVE → TERMINATED`), cọc `FORFEITED` tùy form admin; giường giữ đến checkout.

**URL:** `/admin/contracts`, `/admin/contracts/{id}`, POST check-in/out, POST `/admin/contracts/{id}/cancel-draft`, `/student/contract`, `/student/renewals`, `/staff/checkin`.

**Service:** `ContractService` (`createDraftFromAllocation` thuộc PR-08), `CheckInOutService`, `DocumentNumberService`, `ContractExpiryReminderJob` (`@Scheduled(cron = "0 0 8 * * *")`).

**Edge cases:** Check-in lần 2 → lỗi; checkout còn invoice OVERDUE → cảnh báo, admin force được; mất tài sản → `RoomAsset.condition=DAMAGED` + trừ cọc trên form checkout; hai đợt OPEN khác type không cho SV có HĐ OCCUPYING nộp đợt kia.

---
