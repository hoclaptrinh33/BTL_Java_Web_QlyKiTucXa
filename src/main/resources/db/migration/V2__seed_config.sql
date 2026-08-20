-- Flyway V2 — seed system_configs nguyên văn Phụ lục C (docs/11-phu-luc.md)
-- Giá phòng từng loại không nằm config — cột rooms.price_per_term.

INSERT INTO system_configs (config_key, config_value, value_type, description) VALUES
    ('alloc.weight.policy',               '1000',  'INT',     'AllocationEngine.plan — trọng số diện chính sách'),
    ('alloc.weight.remote',               '500',   'INT',     'plan — trọng số vùng sâu vùng xa'),
    ('alloc.weight.prev_good',            '200',   'INT',     'plan — trọng số nội trú kỳ trước tốt'),
    ('alloc.preference.mode',             'SOFT',  'STRING',  'plan — SOFT/STRICT khi lệch nguyện vọng'),
    ('room.type.vip.capacity',            '2',     'INT',     'RoomService.create — sức chứa VIP_AC mặc định'),
    ('contract.deposit.ratio',            '0.5',   'DECIMAL', 'createDraft — HALF_UP(price_per_term * ratio) VND nguyên'),
    ('contract.term.months',              '5',     'INT',     'Độ dài kỳ mặc định (không nhân vào cọc)'),
    ('contract.expiry.remind.days',       '30',    'INT',     'ContractExpiryReminderJob'),
    ('billing.electricity.tiers',         '[{"to":50,"price":1984},{"to":100,"price":2050},{"to":200,"price":2380},{"to":300,"price":2998},{"to":400,"price":3350},{"to":null,"price":3460}]', 'JSON', 'BillingEngine.tieredElectricity — 6 bậc'),
    ('billing.water.price_per_m3',        '15000', 'INT',     'Đơn giá nước (đ/m3)'),
    ('billing.fee.sanitation_per_person', '20000', 'INT',     'Phí vệ sinh theo người'),
    ('billing.fee.internet_per_room',     '50000', 'INT',     'Phí internet theo phòng, chia đều'),
    ('billing.fee.parking_per_person',    '30000', 'INT',     'Phí gửi xe theo người'),
    ('billing.room.split_monthly',        'false', 'BOOLEAN', 'v1 bỏ qua nhánh true — tiền phòng theo kỳ'),
    ('billing.late.rate',                 '0.05',  'DECIMAL', 'applyLateFees trên subtotal'),
    ('billing.due.days',                  '10',    'INT',     'Hạn thanh toán (ngày)'),
    ('conduct.initial',                   '100',   'INT',     'Điểm rèn luyện đầu kỳ / reset'),
    ('conduct.warn.threshold',            '50',    'INT',     'UI cảnh cáo khi điểm dưới ngưỡng'),
    ('ticket.autoclose.days',             '7',     'INT',     'TicketAutoCloseJob');
