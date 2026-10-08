-- 3NF: student_id trên allocation_items / room_change_requests / renewal_requests
-- suy ra từ application hoặc contract. invoices.total = subtotal + late_fee.
-- invoice_items.amount giữ nguyên: dòng điện bậc thang và phần dư chia phòng
-- không thỏa amount = qty * unit_price.

ALTER TABLE allocation_items DROP FOREIGN KEY fk_allocation_items_student;
ALTER TABLE allocation_items DROP INDEX fk_allocation_items_student;
ALTER TABLE allocation_items DROP COLUMN student_id;

ALTER TABLE room_change_requests DROP FOREIGN KEY fk_room_change_student;
ALTER TABLE room_change_requests DROP INDEX idx_room_change_requests_student;
ALTER TABLE room_change_requests DROP COLUMN student_id;

ALTER TABLE renewal_requests DROP FOREIGN KEY fk_renewal_requests_student;
ALTER TABLE renewal_requests DROP INDEX idx_renewal_requests_student;
ALTER TABLE renewal_requests DROP COLUMN student_id;

ALTER TABLE invoices DROP COLUMN total;
ALTER TABLE invoices
    ADD COLUMN total DECIMAL(12, 0)
        GENERATED ALWAYS AS (subtotal + late_fee) STORED NOT NULL;
