# Tài liệu dự án — Quản lý Ký túc xá

Đặc tả chức năng và kỹ thuật **v1.6** (Approved, 2026-08-18). Đã tách theo chương để đọc và implement theo từng file.

| Trường | Giá trị |
| --- | --- |
| **Tiêu đề** | Xây dựng ứng dụng web quản lý ký túc xá và phân bổ chỗ ở |
| **Mã dự án** | `BTL_Java_Web_QlyKiTucXa` |
| **Trạng thái** | Approved · phiên bản 1.6 |
| **Stack** | Java 25 LTS · Spring Boot 4.1 · Hibernate 7 · MySQL 8.4 LTS · Thymeleaf + Security 7 |

v1.4: stack Java 25 / Boot 4.1 / MySQL 8.4. v1.5: thêm §24 UI/UX. v1.6: theme trắng/clean theo [exampleUI.png](./image/exampleUI.png) — bỏ sidebar mực / linen / brass.

Quy trình Git (Gitflow) cho thành viên: [../CONTRIBUTING.md](../CONTRIBUTING.md).

## Mục lục đặc tả

| File | Nội dung (số mục gốc) |
| --- | --- |
| [01-tong-quan.md](./01-tong-quan.md) | §1–3 Tổng quan, bối cảnh, goals / non-goals |
| [02-thiet-ke.md](./02-thiet-ke.md) | §4 Kiến trúc, stack, package, vai trò HTTP |
| [03-mo-hinh-du-lieu.md](./03-mo-hinh-du-lieu.md) | §5 OCCUPYING, ERD, entity, enum, Flyway |
| [04-01-xac-thuc.md](./04-01-xac-thuc.md) | §6.1 Xác thực & phân quyền |
| [04-02-ha-tang.md](./04-02-ha-tang.md) | §6.2 Hạ tầng tòa / phòng / giường / tài sản |
| [04-03-phan-bo.md](./04-03-phan-bo.md) | §6.3 Phân bổ chỗ ở & đăng ký phòng (cốt lõi) |
| [04-04-hop-dong.md](./04-04-hop-dong.md) | §6.4 Hợp đồng & hồ sơ lưu trú |
| [04-05-dien-nuoc.md](./04-05-dien-nuoc.md) | §6.5 Điện, nước & hóa đơn |
| [04-06-su-co-vi-pham.md](./04-06-su-co-vi-pham.md) | §6.6 Sự cố & vi phạm |
| [04-07-dashboard.md](./04-07-dashboard.md) | §6.7 Dashboard & báo cáo |
| [05-api-va-giao-dien-java.md](./05-api-va-giao-dien-java.md) | §7–8 URL map, SecurityConfig, interface Java |
| [06-may-trang-thai.md](./06-may-trang-thai.md) | §9 Máy trạng thái |
| [07-sequence.md](./07-sequence.md) | §10 Sequence diagrams |
| [08-bao-mat-va-van-hanh.md](./08-bao-mat-va-van-hanh.md) | §11–12 Bảo mật, logging, kiểm thử |
| [09-quyet-dinh-va-rui-ro.md](./09-quyet-dinh-va-rui-ro.md) | §13–15 Quyết định then chốt, phương án loại, rủi ro |
| [10-trien-khai.md](./10-trien-khai.md) | §16–19 Milestone, open questions, SLA, `application.yml` |
| [11-phu-luc.md](./11-phu-luc.md) | §20–23 Tham chiếu, seed, checklist, `system_configs` |
| [12-pr-plan.md](./12-pr-plan.md) | PR-01 … PR-18 |
| [13-thiet-ke-ui-ux.md](./13-thiet-ke-ui-ux.md) | §24 Layout, token, sitemap, catalog màn, wireframe |

## Đọc theo module chức năng

| # | Chức năng | File | Mục liên quan |
|---|---|---|---|
| 1 | Xác thực & phân quyền | [04-01](./04-01-xac-thuc.md) | [§7.1 SecurityConfig](./05-api-va-giao-dien-java.md) |
| 2 | Hạ tầng tòa / phòng / giường / tài sản | [04-02](./04-02-ha-tang.md) | [§5.2.4–5.2.7](./03-mo-hinh-du-lieu.md) |
| 3 | Phân bổ chỗ ở & đăng ký phòng (cốt lõi) | [04-03](./04-03-phan-bo.md) | [§10.3 sequence](./07-sequence.md) |
| 4 | Hợp đồng & hồ sơ lưu trú | [04-04](./04-04-hop-dong.md) | [§5.2.11–5.2.14](./03-mo-hinh-du-lieu.md) |
| 5 | Điện, nước & hóa đơn | [04-05](./04-05-dien-nuoc.md) | [Phụ lục C](./11-phu-luc.md) |
| 6 | Sự cố & vi phạm | [04-06](./04-06-su-co-vi-pham.md) | |
| 7 | Dashboard & xuất Excel/PDF | [04-07](./04-07-dashboard.md) | |

## Đọc khi implement

- Mô hình dữ liệu / ERD: [03-mo-hinh-du-lieu.md](./03-mo-hinh-du-lieu.md) (§5)
- Máy trạng thái: [06-may-trang-thai.md](./06-may-trang-thai.md) (§9)
- Quyết định then chốt: [09-quyet-dinh-va-rui-ro.md](./09-quyet-dinh-va-rui-ro.md) (§13)
- Kế hoạch PR (18 slice): [12-pr-plan.md](./12-pr-plan.md)
- Thiết kế UI/UX: [13-thiet-ke-ui-ux.md](./13-thiet-ke-ui-ux.md) (§24)
- Checklist chấm demo: [11-phu-luc.md](./11-phu-luc.md) (Phụ lục B)
- Seed `system_configs`: [11-phu-luc.md](./11-phu-luc.md) (Phụ lục C)

Số mục `§` trong văn bản vẫn giữ nguyên để tham chiếu chéo giữa các file.
