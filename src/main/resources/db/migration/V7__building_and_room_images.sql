-- Flyway V7 — Lưu trữ ảnh tòa nhà và ảnh phòng / loại phòng
-- InnoDB, utf8mb4

CREATE TABLE building_images (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    building_id     BIGINT          NOT NULL,
    image_url       VARCHAR(255)    NOT NULL,
    caption         VARCHAR(150)    NULL,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    display_order   INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_bldg_img_building (building_id),
    CONSTRAINT fk_bldg_img_building FOREIGN KEY (building_id) REFERENCES buildings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE room_images (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    room_type       VARCHAR(20)     NOT NULL,
    building_id     BIGINT          NULL,
    room_id         BIGINT          NULL,
    image_url       VARCHAR(255)    NOT NULL,
    caption         VARCHAR(150)    NULL,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    display_order   INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_room_img_type (room_type),
    KEY idx_room_img_bldg (building_id),
    KEY idx_room_img_room (room_id),
    CONSTRAINT fk_room_img_bldg FOREIGN KEY (building_id) REFERENCES buildings (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_img_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
