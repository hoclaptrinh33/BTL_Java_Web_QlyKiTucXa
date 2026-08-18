> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← API & Java](./05-api-va-giao-dien-java.md) · [Mục lục](./README.md) · [Sequence →](./07-sequence.md)

# 9. Máy trạng thái (State machines)

```mermaid
stateDiagram-v2
    [*] --> DRAFT: tao_don
    DRAFT --> SUBMITTED: nop
    DRAFT --> [*]: xoa
    SUBMITTED --> WITHDRAWN: rut_truoc_close
    SUBMITTED --> ALLOCATED: commit_ASSIGNED
    SUBMITTED --> WAITLISTED: commit_het_cho
    SUBMITTED --> REJECTED: admin_hoac_SKIPPED_BLOCKED_hoac_SKIPPED_ALREADY_HOUSED
    WAITLISTED --> ALLOCATED: gan_tay
    WAITLISTED --> REJECTED: het_dot
    ALLOCATED --> [*]
    REJECTED --> [*]
    WITHDRAWN --> [*]
```

```mermaid
stateDiagram-v2
    [*] --> DRAFT: tao_dot
    DRAFT --> OPEN: mo_dang_ky
    OPEN --> CLOSED: dong_cong
    CLOSED --> ALLOCATING: bat_dau_commit
    ALLOCATING --> COMPLETED: commit_xong
    ALLOCATING --> CLOSED: commit_loi
    OPEN --> CLOSED: het_han_tu_dong
```

```mermaid
stateDiagram-v2
    [*] --> VACANT
    VACANT --> OCCUPIED: allocate_hoac_gan_tay
    VACANT --> MAINTENANCE: hong
    OCCUPIED --> VACANT: checkout
    OCCUPIED --> VACANT: huy_DRAFT_truoc_checkin
    OCCUPIED --> VACANT: chuyen_di
    MAINTENANCE --> VACANT: sua_xong
    note right of OCCUPIED: khong_chuyen_thang_sang_MAINTENANCE
```

```mermaid
stateDiagram-v2
    [*] --> DRAFT: sau_allocate
    DRAFT --> ACTIVE: checkin
    DRAFT --> COMPLETED: huy_truoc_nhan_nha_giuong_ngay
    ACTIVE --> PENDING_RENEWAL: co_don_gia_han
    PENDING_RENEWAL --> ACTIVE: duyet_gia_han
    PENDING_RENEWAL --> ACTIVE: tu_choi_hoac_huy_don_giu_end_date
    PENDING_RENEWAL --> EXPIRED: qua_end_date_don_con_SUBMITTED
    ACTIVE --> EXPIRED: qua_end_date
    ACTIVE --> TERMINATED: buoc_roi_van_chiem_giuong
    EXPIRED --> COMPLETED: checkout
    TERMINATED --> COMPLETED: checkout
    ACTIVE --> COMPLETED: checkout_dung_han
    note right of TERMINATED: OCCUPYING_den_khi_checkout
    note right of COMPLETED: FREE_unique_key_NULL
```

```mermaid
stateDiagram-v2
    [*] --> OPEN
    OPEN --> IN_PROGRESS: staff_nhan
    IN_PROGRESS --> RESOLVED: sua_xong
    RESOLVED --> CLOSED: sv_xac_nhan_hoac_timeout
    OPEN --> REJECTED: khong_hop_le
    IN_PROGRESS --> OPEN: tra_lai
```

```mermaid
stateDiagram-v2
    [*] --> UNPAID: phat_hanh
    UNPAID --> PAID: du_tien
    UNPAID --> OVERDUE: qua_han
    OVERDUE --> PAID: thanh_toan
    UNPAID --> CANCELLED: admin_huy
    OVERDUE --> CANCELLED: admin_huy
```

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED
    SUBMITTED --> APPROVED: admin_chon_giuong
    SUBMITTED --> REJECTED
    SUBMITTED --> CANCELLED: sv_huy
    APPROVED --> COMPLETED: doi_bed_trong_tx
    APPROVED --> SUBMITTED: bed_mat_cho
```

---
