> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module sự cố](./04-06-su-co-vi-pham.md) · [Mục lục](./README.md) · [API & Java →](./05-api-va-giao-dien-java.md)

# 6.7. Module 7 — Dashboard & báo cáo

**Admin dashboard** `/admin/dashboard`:

- Occupancy = `COUNT(beds OCCUPIED) / COUNT(beds thuộc Room.status=ACTIVE và bed không MAINTENANCE)` theo tòa / toàn hệ. Phòng `INACTIVE` không vào mẫu số. Tử số phải khớp HĐ OCCUPYING; drift hiện trên `reconcileOccupancy`.
- Công nợ: tổng `invoices` UNPAID+OVERDUE theo tháng (Chart.js bar).
- Số đơn SUBMITTED / ALLOCATED / WAITLISTED đợt đang mở.
- Ticket OPEN, hợp đồng hết hạn 30 ngày.

**Xuất:**

- `ExportService.exportResidentsXlsx()` — MSSV, họ tên, tòa, phòng, giường, HĐ, hạn.
- `exportDebtsXlsx()` / PDF — hóa đơn quá hạn.
- `exportInvoicePdf(invoiceId)` — biên lai in.

**URL:** `/admin/reports/residents`, `/admin/reports/debts`, GET download `*.xlsx|pdf`. Staff: occupancy tòa mình.

---
