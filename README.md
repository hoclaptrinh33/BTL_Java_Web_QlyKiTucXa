# Quản lý Ký túc xá

Ứng dụng web quản lý ký túc xá và phân bổ chỗ ở.

| Trường | Giá trị |
| --- | --- |
| **Mã dự án** | `BTL_Java_Web_QlyKiTucXa` |
| **Stack** | Java 25 LTS · Spring Boot 4.1 · Hibernate 7 · MySQL 8.4 LTS · Thymeleaf + Security 7 |
| **Trạng thái** | Đặc tả v1.6 đã duyệt — skeleton Maven đã khởi tạo |

## Tài liệu

- Đặc tả chức năng và kỹ thuật: [docs/README.md](./docs/README.md)
- **Gitflow — clone, nhánh `feature/…`, PR, không đẩy `main`:** [CONTRIBUTING.md](./CONTRIBUTING.md)
- Nhận việc: [Issues](https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa/issues) · bảng việc [#40](https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa/issues/40)

## Yêu cầu chạy local

- JDK **25** (Temurin)
- Maven 3.9+ (hoặc dùng `mvnw` trong repo)
- MySQL **8.4 LTS**, database `ktx`, user/password mặc định `ktx` / `ktx`

Biến môi trường (xem [`.env.example`](./.env.example)):

| Biến | Mặc định |
| --- | --- |
| `KTX_DB_USER` | `ktx` |
| `KTX_DB_PASSWORD` | `ktx` |

Nếu MySQL yêu cầu public key: thêm `allowPublicKeyRetrieval=true` vào JDBC URL (chỉ lab).

```sql
CREATE DATABASE ktx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ktx'@'localhost' IDENTIFIED BY 'ktx';
GRANT ALL PRIVILEGES ON ktx.* TO 'ktx'@'localhost';
FLUSH PRIVILEGES;
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Hiện chưa có schema Flyway (sẽ thêm ở PR-02). App chỉ bootstrap được sau khi có migration và MySQL sẵn sàng.

## Cấu trúc thư mục

```
BTL_Java_Web_QlyKiTucXa/
├── pom.xml
├── README.md
├── docs/                          # đặc tả v1.6
└── src/
    ├── main/
    │   ├── java/com/ktx/
    │   │   ├── KtxApplication.java
    │   │   ├── config/            # SecurityConfig, WebMvc, Mail, DataSeeder
    │   │   ├── security/          # UserDetails, login handler
    │   │   ├── common/            # exception, advice, pagination, StaffScope, util
    │   │   ├── domain/            # @Entity + enum
    │   │   ├── repository/        # Spring Data JPA
    │   │   ├── dto/               # form + view model
    │   │   ├── service/           # nghiệp vụ + AllocationEngine + BillingEngine
    │   │   ├── scheduler/         # job hết hạn HĐ, overdue, đóng ticket
    │   │   └── web/               # MVC: auth / admin / staff / student
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── db/migration/      # Flyway (trống — PR-02)
    │       ├── templates/         # Thymeleaf theo vai trò
    │       └── static/{css,js,images}/
    └── test/java/com/ktx/
        ├── service/               # AllocationEngineTest, BillingEngineTest
        └── it/                    # AllocationLockIT (MySQL local)
```

Package gốc: **`com.ktx`**. Chi tiết đặt tên: [docs/02-thiet-ke.md](./docs/02-thiet-ke.md) §4.3. Kế hoạch implement: [docs/12-pr-plan.md](./docs/12-pr-plan.md).
