> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Máy trạng thái](./06-may-trang-thai.md) · [Mục lục](./README.md) · [Bảo mật →](./08-bao-mat-va-van-hanh.md)

# 10. Sequence diagrams

## 10.1. Đăng nhập

```mermaid
sequenceDiagram
    actor U as User
    participant C as AuthController
    participant SS as SpringSecurity
    participant UD as KtxUserDetailsService
    participant DB as MySQL
    participant H as LoginSuccessHandler
    U->>C: GET /login
    U->>SS: POST /login username+password
    SS->>UD: loadUserByUsername
    UD->>DB: findByUsernameOrEmail
    SS->>SS: BCrypt matches
    alt sai
        SS-->>U: redirect /login?error
    else dung
        SS->>H: onAuthenticationSuccess
        H-->>U: redirect /admin|/staff|/student/dashboard
    end
```

## 10.2. Sinh viên nộp đơn

```mermaid
sequenceDiagram
    actor SV as Student
    participant C as StudentApplicationController
    participant S as RoomApplicationService
    participant DB as MySQL
    SV->>C: GET /student/applications/new
    C->>S: currentOpenPeriod()
    S->>DB: periods status OPEN
    SV->>C: POST form toa + loai phong
    C->>S: submit(studentId, form)
    S->>S: validate period window, gender, block, unique
    S->>DB: insert SUBMITTED + snapshot priority + score
    S-->>SV: flash OK
```

## 10.3. Auto-allocate (preview + commit)

```mermaid
sequenceDiagram
    actor A as Admin
    participant C as AdminAllocationController
    participant AS as AllocationService
    participant E as AllocationEngine
    participant DB as MySQL
    A->>C: POST /preview
    C->>AS: previewAndStore
    AS->>E: plan(periodId)
    E->>DB: read apps + beds (no lock)
    E-->>AS: items
    AS->>DB: save AllocationRun dry_run
    AS-->>A: trang preview
    A->>C: POST /commit
    C->>AS: commit
    AS->>DB: FOR UPDATE period
    AS->>E: plan lai
    loop moi item ASSIGNED
        AS->>DB: FOR UPDATE bed
        AS->>DB: FOR UPDATE beds ORDER BY id
        AS->>DB: INSERT contract DRAFT roi UPDATE bed OCCUPIED
    end
    AS->>DB: period COMPLETED
    AS-->>A: bao cao ket qua
```

## 10.4. Check-in

```mermaid
sequenceDiagram
    actor ST as Staff
    participant C as StaffCheckInController
    participant S as CheckInOutService
    participant B as BillingEngine
    participant DB as MySQL
    ST->>C: POST /staff/checkin/{contractId}
    C->>S: checkIn
    S->>DB: load contract DRAFT + assets
    S->>DB: insert check_in_outs
    S->>DB: contract ACTIVE
    S->>B: issueRoomFee + issueDeposit
    B->>DB: insert invoices UNPAID
    S-->>ST: OK
```

## 10.5. Billing tháng

```mermaid
sequenceDiagram
    actor ST as Staff
    participant C as ReadingController
    participant R as UtilityReadingService
    participant B as BillingEngine
    participant DB as MySQL
    ST->>C: POST chi so phong+thang
    C->>R: save reading
    R->>DB: upsert unique room+month
    ST->>C: POST lap hoa don
    C->>B: issueUtilityInvoices
    B->>DB: dem N hop dong OCCUPYING
    B->>B: tiered elec + split
    B->>DB: invoices + items / student
    B-->>ST: so hoa don tao
```

---
