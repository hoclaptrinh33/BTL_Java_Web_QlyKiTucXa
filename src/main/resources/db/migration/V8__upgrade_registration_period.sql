-- ---------------------------------------------------------------------------
-- V8: Upgrade registration_periods with eligibility, quota, timeline, financial & contact fields
-- ---------------------------------------------------------------------------

ALTER TABLE registration_periods
    ADD COLUMN gender_scope             VARCHAR(20)   NOT NULL DEFAULT 'ALL',
    ADD COLUMN min_conduct_score        INT           NULL DEFAULT 0,
    ADD COLUMN target_cohort            VARCHAR(100)  NULL,
    ADD COLUMN target_quota             INT           NULL,
    ADD COLUMN payment_deadline         DATETIME      NULL,
    ADD COLUMN checkin_start            DATE          NULL,
    ADD COLUMN checkin_end              DATE          NULL,
    ADD COLUMN deposit_ratio            DECIMAL(4,2)  NULL DEFAULT 0.50,
    ADD COLUMN payment_guide            TEXT          NULL,
    ADD COLUMN require_document_proof   TINYINT(1)    NOT NULL DEFAULT 0,
    ADD COLUMN terms_and_conditions     TEXT          NULL,
    ADD COLUMN description              TEXT          NULL,
    ADD COLUMN contact_phone            VARCHAR(30)   NULL,
    ADD COLUMN contact_email            VARCHAR(100)  NULL;

CREATE TABLE IF NOT EXISTS registration_period_buildings (
    period_id   BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    PRIMARY KEY (period_id, building_id),
    CONSTRAINT fk_rpb_period FOREIGN KEY (period_id) REFERENCES registration_periods (id) ON DELETE CASCADE,
    CONSTRAINT fk_rpb_building FOREIGN KEY (building_id) REFERENCES buildings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
