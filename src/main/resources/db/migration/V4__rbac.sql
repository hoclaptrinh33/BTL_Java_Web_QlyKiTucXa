-- Flyway V4 — RBAC core schema and seed data

-- 1. Thêm cột account_kind vào bảng users
ALTER TABLE users ADD COLUMN account_kind VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

-- Cập nhật giá trị account_kind cho dữ liệu hiện có
UPDATE users SET account_kind = 'STUDENT' WHERE role = 'STUDENT';
UPDATE users SET account_kind = 'INTERNAL' WHERE role != 'STUDENT';

-- 2. Bảng permissions
CREATE TABLE permissions (
    code        VARCHAR(50)  NOT NULL,
    plane       VARCHAR(20)  NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng roles
CREATE TABLE roles (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(255) NULL,
    system_locked  BOOLEAN      NOT NULL DEFAULT FALSE,
    scope_mode     VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng role_permissions
CREATE TABLE role_permissions (
    role_id         BIGINT      NOT NULL,
    permission_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (role_id, permission_code),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_code) REFERENCES permissions (code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng user_roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng user_buildings
CREATE TABLE user_buildings (
    user_id     BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, building_id),
    CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ub_building FOREIGN KEY (building_id) REFERENCES buildings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Seed Permissions
-- Mã quyền SYSTEM (3)
INSERT INTO permissions (code, plane) VALUES ('config.read', 'SYSTEM');
INSERT INTO permissions (code, plane) VALUES ('config.write', 'SYSTEM');
INSERT INTO permissions (code, plane) VALUES ('admin_account.manage', 'SYSTEM');

-- Mã quyền OPERATION (23)
INSERT INTO permissions (code, plane) VALUES ('student.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('student.write', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('building.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('building.write', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('room.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('room.write', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('period.manage', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('application.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('allocation.manage', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('contract.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('contract.write', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('invoice.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('invoice.issue', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('payment.record', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('meter.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('ticket.handle', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('violation.write', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('checkin.operate', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('checkout.force', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('report.read', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('user.manage', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('role.manage', 'OPERATION');
INSERT INTO permissions (code, plane) VALUES ('building.assign', 'OPERATION');

-- 8. Seed Roles khóa
INSERT INTO roles (code, name, description, system_locked, scope_mode) VALUES
('SYSTEM_ADMIN', 'Quản trị hệ thống', 'Quản trị hệ thống, cấu hình và tài khoản quản trị', TRUE, 'ALL'),
('QUAN_LY', 'Quản lý', 'Quản lý vận hành toàn diện ký túc xá', TRUE, 'ALL'),
('CAN_BO', 'Cán bộ', 'Cán bộ quản lý theo tòa nhà được phân công', TRUE, 'BUILDINGS');

-- 9. Gán quyền cho vai khóa (role_permissions)
-- SYSTEM_ADMIN: chỉ 3 quyền SYSTEM
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code FROM roles r, permissions p
WHERE r.code = 'SYSTEM_ADMIN' AND p.plane = 'SYSTEM';

-- QUAN_LY: mọi quyền OPERATION, không có quyền SYSTEM
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code FROM roles r, permissions p
WHERE r.code = 'QUAN_LY' AND p.plane = 'OPERATION';

-- CAN_BO: 5 quyền: room.read, ticket.handle, violation.write, checkin.operate, meter.read
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code FROM roles r, permissions p
WHERE r.code = 'CAN_BO' AND p.code IN ('room.read', 'ticket.handle', 'violation.write', 'checkin.operate', 'meter.read');

-- 10. Migrate user cũ theo users.role
-- admin cũ -> SYSTEM_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.role = 'ADMIN' AND r.code = 'SYSTEM_ADMIN'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- staff cũ -> CAN_BO
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.role = 'STAFF' AND r.code = 'CAN_BO'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- Đồng bộ tòa đã gán của staff vào user_buildings
INSERT INTO user_buildings (user_id, building_id)
SELECT s.user_id, s.assigned_building_id FROM staff s
WHERE NOT EXISTS (SELECT 1 FROM user_buildings ub WHERE ub.user_id = s.user_id AND ub.building_id = s.assigned_building_id);
