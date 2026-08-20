-- Flyway V1 — schema đầy đủ theo docs/03-mo-hinh-du-lieu.md §5.0–5.4
-- InnoDB, utf8mb4. Không ON DELETE CASCADE từ contracts → beds.
-- beds.current_contract_id cố ý không FK (tránh vòng InnoDB không defer được).

-- ---------------------------------------------------------------------------
-- 5.2.1 users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.4 buildings  (trước staff — assigned_building_id NOT NULL)
-- ---------------------------------------------------------------------------
CREATE TABLE buildings (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(10)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    gender_policy   VARCHAR(10)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_buildings_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.2 students
-- ---------------------------------------------------------------------------
CREATE TABLE students (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL,
    student_code            VARCHAR(20)  NOT NULL,
    full_name               VARCHAR(120) NOT NULL,
    gender                  VARCHAR(10)  NOT NULL,
    date_of_birth           DATE         NULL,
    faculty_code            VARCHAR(30)  NULL,
    class_code              VARCHAR(30)  NULL,
    phone                   VARCHAR(20)  NULL,
    emergency_name          VARCHAR(120) NULL,
    emergency_phone         VARCHAR(20)  NULL,
    hometown                VARCHAR(120) NULL,
    priority_category       VARCHAR(30)  NOT NULL DEFAULT 'NONE',
    previous_stay_good      BOOLEAN      NOT NULL DEFAULT FALSE,
    conduct_score           INT          NOT NULL DEFAULT 100,
    blocked_from_housing    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_students_user_id (user_id),
    UNIQUE KEY uk_students_student_code (student_code),
    KEY idx_students_gender_faculty_class (gender, faculty_code, class_code),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_students_conduct_score CHECK (conduct_score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.3 staff — ROLE_STAFF bắt buộc một tòa
-- ---------------------------------------------------------------------------
CREATE TABLE staff (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL,
    full_name               VARCHAR(120) NOT NULL,
    phone                   VARCHAR(20)  NULL,
    assigned_building_id    BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_user_id (user_id),
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_staff_building FOREIGN KEY (assigned_building_id) REFERENCES buildings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.5 rooms
-- ---------------------------------------------------------------------------
CREATE TABLE rooms (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    building_id     BIGINT          NOT NULL,
    room_number     VARCHAR(10)     NOT NULL,
    floor           INT             NOT NULL,
    room_type       VARCHAR(20)     NOT NULL,
    capacity        INT             NOT NULL,
    price_per_term  DECIMAL(12, 0)  NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rooms_building_number (building_id, room_number),
    KEY idx_rooms_building_id (building_id),
    KEY idx_rooms_status (status),
    CONSTRAINT fk_rooms_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.6 beds — current_contract_id là cache, không FK cứng tới contracts
-- ---------------------------------------------------------------------------
CREATE TABLE beds (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    room_id                 BIGINT      NOT NULL,
    bed_code                VARCHAR(10) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'VACANT',
    current_contract_id     BIGINT      NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beds_room_code (room_id, bed_code),
    KEY idx_beds_status (status),
    KEY idx_beds_room_status (room_id, status),
    CONSTRAINT fk_beds_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.7 room_assets
-- ---------------------------------------------------------------------------
CREATE TABLE room_assets (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    room_id         BIGINT       NOT NULL,
    name            VARCHAR(120) NOT NULL,
    category        VARCHAR(30)  NOT NULL,
    quantity        INT          NOT NULL DEFAULT 1,
    `condition`     VARCHAR(20)  NOT NULL DEFAULT 'GOOD',
    note            VARCHAR(500) NULL,
    serial_number   VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    KEY idx_room_assets_room_id (room_id),
    CONSTRAINT fk_room_assets_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.8 registration_periods
-- ---------------------------------------------------------------------------
CREATE TABLE registration_periods (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(120) NOT NULL,
    period_type     VARCHAR(30)  NOT NULL,
    academic_year   VARCHAR(20)  NOT NULL,
    open_at         DATETIME     NOT NULL,
    close_at        DATETIME     NOT NULL,
    term_start      DATE         NOT NULL,
    term_end        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by      BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_registration_periods_status_type (status, period_type),
    CONSTRAINT fk_registration_periods_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.9 room_applications
-- ---------------------------------------------------------------------------
CREATE TABLE room_applications (
    id                              BIGINT       NOT NULL AUTO_INCREMENT,
    period_id                       BIGINT       NOT NULL,
    student_id                      BIGINT       NOT NULL,
    preferred_building_id           BIGINT       NULL,
    preferred_room_type             VARCHAR(20)  NULL,
    priority_snapshot               VARCHAR(30)  NOT NULL,
    previous_stay_good_snapshot     BOOLEAN      NOT NULL DEFAULT FALSE,
    status                          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_at                    DATETIME     NULL,
    computed_score                  INT          NULL,
    note                            VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_applications_period_student (period_id, student_id),
    CONSTRAINT fk_room_applications_period FOREIGN KEY (period_id) REFERENCES registration_periods (id),
    CONSTRAINT fk_room_applications_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_room_applications_building FOREIGN KEY (preferred_building_id) REFERENCES buildings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.10 allocation_runs / allocation_items
-- ---------------------------------------------------------------------------
CREATE TABLE allocation_runs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    period_id       BIGINT       NOT NULL,
    dry_run         BOOLEAN      NOT NULL DEFAULT TRUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    started_at      DATETIME     NULL,
    finished_at     DATETIME     NULL,
    run_by          BIGINT       NULL,
    summary_json    TEXT         NULL,
    seed_note       VARCHAR(255) NULL,
    weights_json    TEXT         NULL,
    PRIMARY KEY (id),
    KEY idx_allocation_runs_period (period_id),
    CONSTRAINT fk_allocation_runs_period FOREIGN KEY (period_id) REFERENCES registration_periods (id),
    CONSTRAINT fk_allocation_runs_run_by FOREIGN KEY (run_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE allocation_items (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    run_id              BIGINT      NOT NULL,
    application_id      BIGINT      NOT NULL,
    student_id          BIGINT      NOT NULL,
    bed_id              BIGINT      NULL,
    rank_no             INT         NOT NULL,
    score               INT         NOT NULL,
    result              VARCHAR(20) NOT NULL,
    reason              VARCHAR(40) NULL,
    PRIMARY KEY (id),
    KEY idx_allocation_items_run (run_id),
    KEY idx_allocation_items_application (application_id),
    CONSTRAINT fk_allocation_items_run FOREIGN KEY (run_id) REFERENCES allocation_runs (id),
    CONSTRAINT fk_allocation_items_application FOREIGN KEY (application_id) REFERENCES room_applications (id),
    CONSTRAINT fk_allocation_items_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_allocation_items_bed FOREIGN KEY (bed_id) REFERENCES beds (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.11 contracts — generated UNIQUE active_bed_key / active_student_key
-- Tập OCCUPYING: DRAFT, ACTIVE, PENDING_RENEWAL, EXPIRED, TERMINATED (§5.0)
-- Nhiều NULL được phép trên UNIQUE (MySQL 8.4).
-- Không ON DELETE CASCADE contract → bed.
-- ---------------------------------------------------------------------------
CREATE TABLE contracts (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    contract_no             VARCHAR(30)     NOT NULL,
    student_id              BIGINT          NOT NULL,
    bed_id                  BIGINT          NOT NULL,
    application_id          BIGINT          NULL,
    start_date              DATE            NOT NULL,
    end_date                DATE            NOT NULL,
    room_fee                DECIMAL(12, 0)  NOT NULL,
    deposit_amount          DECIMAL(12, 0)  NOT NULL,
    deposit_status          VARCHAR(20)     NOT NULL DEFAULT 'HELD',
    status                  VARCHAR(20)     NOT NULL,
    completion_reason       VARCHAR(40)     NULL,
    terms_version           VARCHAR(20)     NULL,
    signed_at               DATETIME        NULL,
    active_bed_key          BIGINT
        GENERATED ALWAYS AS (
            IF(status IN ('DRAFT', 'ACTIVE', 'PENDING_RENEWAL', 'EXPIRED', 'TERMINATED'), bed_id, NULL)
        ) STORED,
    active_student_key      BIGINT
        GENERATED ALWAYS AS (
            IF(status IN ('DRAFT', 'ACTIVE', 'PENDING_RENEWAL', 'EXPIRED', 'TERMINATED'), student_id, NULL)
        ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contracts_contract_no (contract_no),
    UNIQUE KEY uk_contract_active_bed (active_bed_key),
    UNIQUE KEY uk_contract_active_student (active_student_key),
    KEY idx_contracts_student_status (student_id, status),
    KEY idx_contracts_bed_status (bed_id, status),
    KEY idx_contracts_end_date (end_date),
    CONSTRAINT fk_contracts_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_contracts_bed FOREIGN KEY (bed_id) REFERENCES beds (id) ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_application FOREIGN KEY (application_id) REFERENCES room_applications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.12 check_in_outs
-- ---------------------------------------------------------------------------
CREATE TABLE check_in_outs (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    contract_id     BIGINT      NOT NULL,
    event_type      VARCHAR(20) NOT NULL,
    performed_at    DATETIME    NOT NULL,
    performed_by    BIGINT      NOT NULL,
    asset_note      TEXT        NULL,
    ok              BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_check_in_outs_contract (contract_id),
    CONSTRAINT fk_check_in_outs_contract FOREIGN KEY (contract_id) REFERENCES contracts (id),
    CONSTRAINT fk_check_in_outs_performed_by FOREIGN KEY (performed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.13 room_change_requests — request_kind = CHANGE | RETURN
-- ---------------------------------------------------------------------------
CREATE TABLE room_change_requests (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    student_id                  BIGINT       NOT NULL,
    contract_id                 BIGINT       NOT NULL,
    request_kind                VARCHAR(20)  NOT NULL DEFAULT 'CHANGE',
    current_bed_id              BIGINT       NOT NULL,
    requested_building_id       BIGINT       NULL,
    requested_room_type         VARCHAR(20)  NULL,
    reason                      VARCHAR(500) NULL,
    target_bed_id               BIGINT       NULL,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    admin_note                  VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_room_change_requests_student (student_id),
    KEY idx_room_change_requests_contract (contract_id),
    CONSTRAINT fk_room_change_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_room_change_contract FOREIGN KEY (contract_id) REFERENCES contracts (id),
    CONSTRAINT fk_room_change_current_bed FOREIGN KEY (current_bed_id) REFERENCES beds (id),
    CONSTRAINT fk_room_change_building FOREIGN KEY (requested_building_id) REFERENCES buildings (id),
    CONSTRAINT fk_room_change_target_bed FOREIGN KEY (target_bed_id) REFERENCES beds (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.14 renewal_requests (V1 — không chờ PR-11)
-- ---------------------------------------------------------------------------
CREATE TABLE renewal_requests (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    student_id          BIGINT       NOT NULL,
    contract_id         BIGINT       NOT NULL,
    requested_end       DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    admin_note          VARCHAR(500) NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at          DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_renewal_requests_student (student_id),
    KEY idx_renewal_requests_contract (contract_id),
    CONSTRAINT fk_renewal_requests_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_renewal_requests_contract FOREIGN KEY (contract_id) REFERENCES contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.15 utility_readings
-- ---------------------------------------------------------------------------
CREATE TABLE utility_readings (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    room_id                 BIGINT      NOT NULL,
    billing_month           DATE        NOT NULL,
    elec_prev               INT         NOT NULL,
    elec_curr               INT         NOT NULL,
    water_prev              INT         NOT NULL,
    water_curr              INT         NOT NULL,
    elec_replaced           BOOLEAN     NOT NULL DEFAULT FALSE,
    water_replaced          BOOLEAN     NOT NULL DEFAULT FALSE,
    elec_old_final          INT         NULL,
    elec_new_start          INT         NULL,
    water_old_final         INT         NULL,
    water_new_start         INT         NULL,
    new_building_meter      BOOLEAN     NOT NULL DEFAULT FALSE,
    recorded_by             BIGINT      NOT NULL,
    recorded_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_utility_readings_room_month (room_id, billing_month),
    CONSTRAINT fk_utility_readings_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_utility_readings_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id),
    CONSTRAINT chk_utility_readings_nonneg CHECK (
        elec_prev >= 0 AND elec_curr >= 0 AND water_prev >= 0 AND water_curr >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.16 invoices / invoice_items / payments
-- ---------------------------------------------------------------------------
CREATE TABLE invoices (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_no          VARCHAR(30)     NOT NULL,
    student_id          BIGINT          NOT NULL,
    room_id             BIGINT          NULL,
    contract_id         BIGINT          NULL,
    invoice_type        VARCHAR(20)     NOT NULL,
    billing_month       DATE            NULL,
    subtotal            DECIMAL(12, 0)  NOT NULL,
    late_fee            DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    total               DECIMAL(12, 0)  NOT NULL,
    due_date            DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    paid_at             DATETIME        NULL,
    idempotency_key     VARCHAR(80)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoices_invoice_no (invoice_no),
    UNIQUE KEY uk_invoices_idempotency_key (idempotency_key),
    KEY idx_invoices_student_status (student_id, status),
    CONSTRAINT fk_invoices_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_invoices_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_invoices_contract FOREIGN KEY (contract_id) REFERENCES contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_id      BIGINT          NOT NULL,
    description     VARCHAR(255)    NOT NULL,
    qty             DECIMAL(12, 3)  NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12, 0)  NOT NULL,
    amount          DECIMAL(12, 0)  NOT NULL,
    item_code       VARCHAR(20)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_invoice_items_invoice (invoice_id),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_id      BIGINT          NOT NULL,
    amount          DECIMAL(12, 0)  NOT NULL,
    method          VARCHAR(20)     NOT NULL,
    paid_at         DATETIME        NOT NULL,
    recorded_by     BIGINT          NOT NULL,
    reference_no    VARCHAR(50)     NULL,
    PRIMARY KEY (id),
    KEY idx_payments_invoice (invoice_id),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.19 tickets
-- ---------------------------------------------------------------------------
CREATE TABLE tickets (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    student_id      BIGINT       NOT NULL,
    room_id         BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NOT NULL,
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at     DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_tickets_student (student_id),
    KEY idx_tickets_room (room_id),
    KEY idx_tickets_status_resolved (status, resolved_at),
    CONSTRAINT fk_tickets_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_tickets_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.20 violations
-- ---------------------------------------------------------------------------
CREATE TABLE violations (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    student_id          BIGINT       NOT NULL,
    recorded_by         BIGINT       NOT NULL,
    violation_type      VARCHAR(30)  NOT NULL,
    severity            VARCHAR(10)  NOT NULL,
    points_deducted     INT          NOT NULL DEFAULT 0,
    description         TEXT         NULL,
    occurred_at         DATETIME     NOT NULL,
    action              VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_violations_student (student_id),
    CONSTRAINT fk_violations_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_violations_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id),
    CONSTRAINT chk_violations_points CHECK (points_deducted >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.21 notifications
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    type            VARCHAR(30)  NOT NULL DEFAULT 'GENERIC',
    read_flag       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_sent      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_notifications_user_read (user_id, read_flag),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.22 system_configs
-- ---------------------------------------------------------------------------
CREATE TABLE system_configs (
    config_key      VARCHAR(80)  NOT NULL,
    config_value    TEXT         NOT NULL,
    value_type      VARCHAR(20)  NOT NULL,
    description     VARCHAR(255) NULL,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.17 document_sequences — PK (kind, year)
-- ---------------------------------------------------------------------------
CREATE TABLE document_sequences (
    kind            VARCHAR(20)  NOT NULL,
    `year`          INT          NOT NULL,
    `last_value`    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (kind, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5.2.18 system_locks
-- ---------------------------------------------------------------------------
CREATE TABLE system_locks (
    lock_name       VARCHAR(40)  NOT NULL,
    PRIMARY KEY (lock_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- §5.4 seed: khóa ALLOCATION + sequence năm hiện tại = 0
INSERT INTO system_locks (lock_name) VALUES ('ALLOCATION');

INSERT INTO document_sequences (kind, `year`, `last_value`) VALUES
    ('CONTRACT_NO', 2026, 0),
    ('INVOICE_NO', 2026, 0);
