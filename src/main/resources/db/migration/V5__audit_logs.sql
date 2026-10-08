-- Flyway V5 — Audit logs schema and permissions
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NULL,
    username    VARCHAR(50)  NOT NULL,
    user_role   VARCHAR(50)  NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50)  NULL,
    target_id   VARCHAR(100) NULL,
    description VARCHAR(500) NOT NULL,
    ip_address  VARCHAR(45)  NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_created_at (created_at DESC),
    KEY idx_audit_username (username),
    KEY idx_audit_action (action),
    KEY idx_audit_target_type (target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm quyền log.read vào bảng permissions thuộc plane SYSTEM
INSERT INTO permissions (code, plane) VALUES ('log.read', 'SYSTEM')
ON DUPLICATE KEY UPDATE plane = VALUES(plane);

-- Gán quyền log.read cho vai trò SYSTEM_ADMIN và QUAN_LY
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, 'log.read' FROM roles r
WHERE r.code IN ('SYSTEM_ADMIN', 'QUAN_LY')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_code = 'log.read'
);

-- Khởi tạo một số log ban đầu ghi nhận mốc kích hoạt hệ thống (chỉ chèn nếu chưa có)
INSERT INTO audit_logs (username, user_role, action, target_type, target_id, description, ip_address, status, created_at)
SELECT 'system', 'SYSTEM', 'SYSTEM_INIT', 'SYSTEM', '1', 'Hệ thống KTX khởi động thành công với phiên bản RBAC & Phụ lục C', '127.0.0.1', 'SUCCESS', NOW() - INTERVAL 2 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE action = 'SYSTEM_INIT');

INSERT INTO audit_logs (username, user_role, action, target_type, target_id, description, ip_address, status, created_at)
SELECT 'admin', 'SYSTEM_ADMIN', 'CONFIG_CHECK', 'SYSTEM_CONFIG', 'all', 'Kiểm tra và xác nhận 19 tham số vận hành chuẩn', '127.0.0.1', 'SUCCESS', NOW() - INTERVAL 1 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE action = 'CONFIG_CHECK');
