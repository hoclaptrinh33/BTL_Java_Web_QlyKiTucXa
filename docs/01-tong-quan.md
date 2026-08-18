> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [Mục lục](./README.md) · [Thiết kế →](./02-thiet-ke.md)

# Đặc tả chức năng & kỹ thuật — Hệ thống Quản lý Ký túc xá (KTX)

| Trường                      | Giá trị                                                              |
| ----------------------------- | ---------------------------------------------------------------------- |
| **Tiêu đề**          | Xây dựng ứng dụng web quản lý ký túc xá và phân bổ chỗ ở |
| **Mã dự án**         | `BTL_Java_Web_QlyKiTucXa`                                            |
| **Tác giả**           | Nhóm BTL —*Lê Hải Đăng*                                        |
| **Ngày**               | 2026-08-18                                                             |
| **Trạng thái**        | Approved                                                               |
| **Phiên bản**         | 1.6                                                                    |
| **Đối tượng đọc** | Nhóm triển khai                                                      |

---

# 1. Tổng quan (Overview)

Đồ án xây dựng **một ứng dụng web monolith** giúp Ban quản lý ký túc xá (BQL) vận hành toàn bộ vòng đời lưu trú: quản lý tòa/phòng/giường, mở đợt đăng ký, phân bổ chỗ ở theo chính sách ưu tiên, lập hợp đồng, ghi điện nước, xuất hóa đơn, tiếp nhận sự cố và xử lý vi phạm. 

Giải pháp đề xuất là **Spring Boot 4.1.x (Java 25 LTS) + Spring MVC/Thymeleaf + Spring Security 7 (form login/session) + Spring Data JPA/Hibernate 7 + MySQL 8.4 LTS**, đóng gói một file JAR (hoặc WAR) chạy độc lập. Module cốt lõi là **Allocation Engine**: thuật toán greedy có trọng số, tie-break xác định, khóa bi quan trên giường khi gán chỗ, hỗ trợ dry-run trước khi commit. Sinh viên dùng giao diện server-rendered; không SPA, không JWT, không microservices.

---

# 2. Bối cảnh & động lực (Background & Motivation)

KTX đại học hiện thường quản lý bằng Excel/giấy: dễ trùng giường khi nhiều cán bộ cùng xếp phòng, khó truy vết ưu tiên (chính sách, vùng sâu, sinh viên cũ), hóa đơn điện nước tính tay dễ lệch, không có lịch sử vi phạm/điểm rèn luyện gắn với quyết định ở lại.

**Điểm đau cần giải:**

- Đăng ký theo đợt (tân sinh viên / năm học mới / hè) nhưng xếp phòng thủ công, thiếu công bằng và khó giải trình.
- Không khóa chỗ khi hai admin cùng gán một giường.
- Hóa đơn phòng vs điện nước tách rời, không chia đều theo số người thực tế trong tháng.
- Hết hạn hợp đồng không được nhắc; chuyển phòng/trả phòng không cập nhật tồn giường.
- Báo cáo lấp đầy / công nợ phải tổng hợp tay trước buổi họp BQL.

Hệ thống nhắm quy mô **BTL / phòng lab**: 3–5 tòa, 200–500 phòng, 1.000–3.000 sinh viên, đỉnh ~200 phiên đồng thời khi mở đợt. Độ trễ trang CRUD < 300 ms (local), một lần phân bổ 2.000 đơn < 5 giây.

---

# 3. Mục tiêu & ngoài phạm vi (Goals & Non-Goals)

## 3.1. Goals

- Cung cấp portal đăng nhập theo vai trò `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_STUDENT`.
- Quản lý hạ tầng: tòa (giới tính), phòng (loại/giá kỳ), giường (trống/có người/bảo trì), tài sản phòng.
- Mở đợt đăng ký; sinh viên nộp nguyện vọng; admin chạy phân bổ tự động (preview) rồi commit, hoặc gán tay.
- Quản lý hợp đồng, đặt cọc, check-in/check-out, chuyển phòng / trả phòng.
- Ghi chỉ số điện nước hàng tháng, tính tiền theo công thức cấu hình, chia đều cho người ở, theo dõi thanh toán.
- Ticket sửa chữa, biên bản vi phạm, điểm rèn luyện KTX.
- Dashboard occupancy / công nợ; xuất Excel/PDF danh sách nội trú và nợ phí.
- Có thể triển khai tăng dần theo milestone BTL, mỗi bước có thể demo.

## 3.2. Non-Goals

- Không microservices, Kafka, Redis, Elasticsearch, Docker Swarm, Kubernetes, cloud-native.
- Không SPA (React/Vue) + REST/JWT. API REST (nếu có) chỉ phục vụ Chart.js nội bộ hoặc sau này — **không** là giao diện chính.
- Không thanh toán cổng online (VNPay/MoMo). Ghi nhận tiền mặt / chuyển khoản thủ công.
- Không app mobile native, không chatbot, không nhận diện khuôn mặt check-in.
- Không tối ưu toàn cục ILP/solver (CPLEX, OR-Tools) — quá phức tạp cho BTL.
- Không đa trường / đa campus.
- Không SSO trường (LDAP/CAS) ở phiên bản 1.

---
