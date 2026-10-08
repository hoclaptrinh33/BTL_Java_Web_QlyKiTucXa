-- ---------------------------------------------------------------------------
-- V9: Tối ưu hóa hiệu năng truy vấn và phân trang danh sách tài chính (payments & invoices)
-- ---------------------------------------------------------------------------
DELIMITER $$
CREATE PROCEDURE add_finance_perf_indexes()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'payments' AND index_name = 'idx_payments_paid_at_id'
    ) THEN
        ALTER TABLE payments ADD INDEX idx_payments_paid_at_id (paid_at DESC, id DESC);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'payments' AND index_name = 'idx_payments_reference_no'
    ) THEN
        ALTER TABLE payments ADD INDEX idx_payments_reference_no (reference_no);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'invoices' AND index_name = 'idx_invoices_due_date_id'
    ) THEN
        ALTER TABLE invoices ADD INDEX idx_invoices_due_date_id (due_date DESC, id DESC);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'invoices' AND index_name = 'idx_invoices_status'
    ) THEN
        ALTER TABLE invoices ADD INDEX idx_invoices_status (status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'invoices' AND index_name = 'idx_invoices_type'
    ) THEN
        ALTER TABLE invoices ADD INDEX idx_invoices_type (invoice_type);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'invoices' AND index_name = 'idx_invoices_room_month'
    ) THEN
        ALTER TABLE invoices ADD INDEX idx_invoices_room_month (room_id, billing_month);
    END IF;
END $$
DELIMITER ;

CALL add_finance_perf_indexes();
DROP PROCEDURE IF EXISTS add_finance_perf_indexes;
