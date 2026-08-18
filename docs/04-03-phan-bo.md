> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module hạ tầng](./04-02-ha-tang.md) · [Mục lục](./README.md) · [Module hợp đồng →](./04-04-hop-dong.md)

# 6.3. Module 3 — Phân bổ chỗ ở & đăng ký phòng (cốt lõi)

## 6.3.1. Actors & stories

- Admin mở đợt “Tân SV 2026” (open/close, term_start/end).
- Sinh viên chọn nguyện vọng: loại phòng, tòa mong muốn; hệ thống ghi nhận diện ưu tiên từ hồ sơ.
- Admin bấm **Xem trước phân bổ (dry-run)** → bảng xếp hạng + giường dự kiến + danh sách chờ.
- Admin **chốt (commit)** → tạo hợp đồng DRAFT, Occupied giường.
- Admin gán tay một sinh viên ngoại lệ vào giường cụ thể.
- Sinh viên xin chuyển phòng / trả phòng; admin duyệt.

## 6.3.2. Quy tắc nghiệp vụ

1. Chỉ nộp đơn khi period `OPEN` và `now ∈ [open_at, close_at]`.
2. Một sinh viên / một đợt đúng một đơn (unique).
3. Không nộp nếu sinh viên đang có **bất kỳ** hợp đồng `OCCUPYING` (kể cả `DRAFT`, `PENDING_RENEWAL`, `EXPIRED`, `TERMINATED` chưa checkout). Đợt hè và đợt năm học mới **không** được tạo HĐ thứ hai — ở tiếp = gia hạn HĐ cũ ([mục 6.4](./04-04-hop-dong.md)).
4. `blocked_from_housing` hoặc `conduct_score == 0` → từ chối nộp.
5. Giới tính sinh viên **phải** khớp `Building.gender_policy`. Nguyện vọng tòa khác giới → validate fail ngay lúc nộp.
6. Ưu tiên lấy snapshot lúc SUBMITTED (tránh sinh viên/admin sửa hồ sơ giữa chừng để leo rank).
7. Sau `CLOSED`, sinh viên không sửa đơn; admin có thể `REJECTED` thủ công.
8. Commit allocation chỉ khi period `CLOSED` hoặc `ALLOCATING` (khóa nộp đơn trước khi xếp).
9. Dry-run **không** đụng `beds`/`contracts`.
10. Commit chỉ chấp nhận run `COMPLETED` mới nhất của period, `dry_run=true` hoặc `false` — nếu dry-run, commit sẽ **chạy lại engine** (không tin preview cũ nếu beds đã đổi) trong một transaction.

## 6.3.3. Mã hóa diện ưu tiên & điểm

Enum `PriorityCategory`:

| Enum            | Ý nghĩa                                                         | Điểm cộng`W_PRIORITY` |
| --------------- | ----------------------------------------------------------------- | -------------------------- |
| `POLICY`      | Con liệt sĩ/thương binh, hộ nghèo/cận nghèo có giấy tờ | **1000**             |
| `REMOTE_AREA` | Vùng sâu, vùng xa, hải đảo                                  | **500**              |
| `NONE`        | Không thuộc diện trên                                         | **0**                |

Cờ độc lập `previous_stay_good`:

| Điều kiện                                                                                                          | Điểm         |
| --------------------------------------------------------------------------------------------------------------------- | -------------- |
| `previous_stay_good = true` (kỳ trước không vi phạm MAJOR/SEVERE **và** không còn hóa đơn OVERDUE) | **+200** |

**Không cộng dồn hai diện POLICY + REMOTE** — một sinh viên chỉ mang **một** `PriorityCategory` (admin chọn diện cao nhất). `previous_stay_good` **cộng thêm** được.

Điểm nộp sớm: không cộng số thô (tránh overflow / phụ thuộc đồng hồ). Dùng **thứ tự `submitted_at ASC`, rồi `student_id ASC`** làm tie-break. `computed_score` trên đơn là cache lúc nộp để SV xem.

`AllocationEngine.plan()` **luôn tính lại** `score` từ snapshot đơn + **weight hiện tại** trong `system_configs`. Preview/commit ghi `AllocationRun.weights_json` để audit/diff UI. **Không** có nút “chốt đúng preview” — commit không đọc lại weight của run cũ. Ranking không tin `computed_score` trên đơn.

Công thức:

```
score = W[priority_snapshot] + (previous_stay_good_snapshot ? W_prev_good : 0)
```

Hằng số trong `system_configs` ([Phụ lục C](./11-phu-luc.md)): `alloc.weight.policy` = 1000, `alloc.weight.remote` = 500, `alloc.weight.prev_good` = 200.

`previous_stay_good` **không** tự cập nhật cuối kỳ — admin tick trên hồ sơ SV (sau khi rà vi phạm + công nợ).

**Thứ tự xếp hạng (deterministic):**

1. `score DESC`
2. `submitted_at ASC`
3. `student.id ASC`

Cùng lớp/khoa **không** cộng điểm toàn cục (tránh một lớp “thắng” hết phòng VIP). Áp dụng ở **bước chọn giường** sau khi đã chốt thứ tự ứng viên (mục 6.3.5).

## 6.3.4. Khi không còn giường khớp

`AllocationItem.result = WAITLISTED`, `reason` cụ thể:

| reason                     | Nghĩa                                                                               |
| -------------------------- | ------------------------------------------------------------------------------------ |
| `NO_VACANT_BED`          | Hết giường trống toàn hệ (đúng giới tính, phòng ACTIVE, bed VACANT)       |
| `NO_TYPE_MATCH`          | Còn giường nhưng không đúng`preferred_room_type` (nếu SV bắt buộc loại) |
| `NO_BUILDING_MATCH`      | Còn giường nhưng không đúng tòa nguyện vọng                                |
| `SKIPPED_ALREADY_HOUSED` | Đã có hợp đồng OCCUPYING (item`result=SKIPPED`)                              |
| `SKIPPED_BLOCKED`        | Bị cấm ở (item`result=SKIPPED`)                                                 |
| `NO_GENDER_MATCH`        | Phòng thủ: giới tính/tòa đổi giữa lúc lập`C` và lúc khóa giường     |

**Map `AllocationResult` → `ApplicationStatus` khi commit** (`updateAppStatus`):

| Item result / reason                        | ApplicationStatus sau commit |
| ------------------------------------------- | ---------------------------- |
| `ASSIGNED`                                | `ALLOCATED`                |
| `WAITLISTED` (hết chỗ / nới vẫn fail) | `WAITLISTED`               |
| `SKIPPED` + `SKIPPED_BLOCKED`           | `REJECTED`                 |
| `SKIPPED` + `SKIPPED_ALREADY_HOUSED`    | `REJECTED`                 |

Preview (`dry_run`) **không** đổi `ApplicationStatus`. Đơn `REJECTED`/`ALLOCATED`/`WITHDRAWN` bị loại khỏi `findByPeriodIdAndStatus(..., SUBMITTED)` của lần `plan()` sau.

**Chính sách nguyện vọng (đề xuất mặc định, cấu hình `alloc.preference.mode`):**

- `SOFT` (mặc định): nếu không còn đúng loại/tòa, **nới** lần lượt: bỏ tòa → bỏ loại phòng → waitlist. Ghi `reason` lần nới cuối.
- `STRICT`: không đúng nguyện vọng thì WAITLISTED ngay.
- **Không bao giờ nới `gender_policy`.** Lọc giới tính là cứng trên mọi path (engine, relax, manual, đổi phòng).

Sinh viên waitlist: admin gán tay hoặc chờ người trả phòng.

## 6.3.5. Gom cùng lớp/khoa khi chọn giường

Với mỗi ứng viên theo rank, tập ứng viên giường `C`:

1. Lọc cứng: `Bed.status=VACANT`, `Room.status=ACTIVE`, `Building.gender_policy == student.gender`, bed không `MAINTENANCE`. **Không nới bước này.**
2. Lọc mềm theo nguyện vọng (tùy SOFT/STRICT): chỉ tòa và loại phòng.
3. Chấm điểm **phòng** rồi so giường bằng comparator đầy đủ (mục 6.3.7) — **không** trừ `room_number` trong bonus (số phòng là key sort riêng, vì `VARCHAR` không parse được mọi mã).

Occupant = sinh viên của hợp đồng **OCCUPYING** trên các giường phòng đó (kể cả `DRAFT`). Trong một run, engine **duy trì bản đồ in-memory** `bedId → student` gồm chỗ vừa gán (để 2 SV cùng lớp nộp gần nhau vẫn được gom).

`class_code` / `faculty_code` null-safe: `Objects.equals`; thiếu mã → không cộng bonus lớp/khoa.

## 6.3.6. Khóa & transaction — chống double-booking

**Mức nghiêm trọng: Critical.**

Commit path (`AllocationService.commit`):

1. `@Transactional(isolation = Isolation.READ_COMMITTED)` — MySQL InnoDB default; **không** dùng SERIALIZABLE (dễ lock wait timeout khi demo).
2. `periodRepository.findByIdForUpdate(periodId)` — `SELECT … FOR UPDATE` **chỉ** serialize commit/gán tay **cùng đợt**. Nhiều period **được phép** `ALLOCATING` song song.
3. Đặt `period.status = ALLOCATING`.
4. `plan()` lại với **config sống** (tính score mới, snapshot giường). Ghi `weights_json` audit.
5. Với mỗi item `ASSIGNED`, nếu cần khóa **nhiều** giường (chọn lại): `SELECT … FOR UPDATE … ORDER BY beds.id ASC` — **luôn tăng dần `id`** để tránh deadlock đổi chỗ chéo.
6. Re-check `status == VACANT` và không có HĐ OCCUPYING trên giường. Nếu fail → `reassignOrWaitlist` (dưới đây).
7. `ContractService.createDraftFromAllocation`: cấp `contract_no` (lock `document_sequences`) → **INSERT contract DRAFT** → **UPDATE bed** OCCUPIED + `current_contract_id` (mục 5.2.6).
8. UNIQUE `active_bed_key` / `active_student_key`: nếu `DataIntegrityViolationException` → **fail cả `commit`**. Một `@Transactional`: không try/catch từng item (TX đã rollback-only). Rollback đưa `period.status` về `CLOSED` (trạng thái trước khi vào method — `ALLOCATING` chưa commit). Flash constraint; không HĐ dở. Không `REQUIRES_NEW`. Sau `reassignOrWaitlist` đây là hiếm.
9. `assignManual(studentId, bedId, periodId, note)`:
   - Có `periodId`: `FOR UPDATE` **period đó** (cùng hàng với commit đợt ấy).
   - Không period (gán giữa kỳ): `FOR UPDATE` `system_locks.ALLOCATION`.
   - **`commit` không khóa `ALLOCATION`.** Đụng độ giữa hai đợt / gán tay đợt khác: unique key nổ → fail cả TX.
   - Khóa giường `ORDER BY id`; kiểm tra giới tính; SV chưa OCCUPYING; INSERT HĐ + UPDATE bed.
10. Dry-run: **không** lock beds; đọc snapshot `VACANT`. Race chấp nhận vì commit tính lại.

`reassignOrWaitlist` (không phải stub): đang giữ period lock; query lại ứng viên `VACANT` khớp giới tính (+ SOFT nới tòa/loại, **không** nới giới tính); `FOR UPDATE` các bed ứng viên `ORDER BY id ASC`; áp **cùng** `roomAwareComparator`; nếu còn giường hợp lệ → ASSIGNED giường mới; không thì `WAITLISTED` + `NO_VACANT_BED` (hoặc reason nới cuối).

Timeout lock: `innodb_lock_wait_timeout` mặc định 50s; bắt `PessimisticLockingFailureException` → flash “Hệ thống đang phân bổ, thử lại”.

**Test lock:** H2 **không** chứng minh InnoDB. `AllocationLockIT` (profile `it-mysql`) chạy máy local MySQL: hai thread `assignManual` cùng giường → một thành công, một `BusinessException`. Testcontainers **không** bắt buộc BTL; CI nhà trường không có MySQL thì skip IT (`@EnabledIfEnvironmentVariable(named="KTX_IT_MYSQL", matches="true")`).

## 6.3.7. Thuật toán (Java-like)

```java
public AllocationRunResult plan(long periodId, AllocConfig cfg) {
    List<RoomApplication> apps = appRepo
        .findByPeriodIdAndStatus(periodId, SUBMITTED);
    for (RoomApplication a : apps) {
        a.setPlanScore(scoreOf(a.getPrioritySnapshot(),
                               a.isPreviousStayGoodSnapshot(), cfg));
    }
    apps.sort(Comparator
        .comparingInt(RoomApplication::getPlanScore).reversed()
        .thenComparing(RoomApplication::getSubmittedAt)
        .thenComparing(a -> a.getStudent().getId()));

    OccupancySnapshot snap = OccupancySnapshot.from(bedRepo, contractRepo);
    // occupants = contract.status ∈ OCCUPYING

    List<AllocationItem> items = new ArrayList<>();
    int rank = 0;
    for (RoomApplication app : apps) {
        rank++;
        Student sv = app.getStudent();
        if (sv.isBlockedFromHousing()) {
            items.add(skip(app, rank, SKIPPED_BLOCKED));
            continue;
        }
        if (hasOccupyingContract(sv)) { // mọi OCCUPYING, không chỉ ACTIVE
            items.add(skip(app, rank, SKIPPED_ALREADY_HOUSED));
            continue;
        }
        List<Bed> candidates = snap.vacantMatching(sv, app, cfg); // gender cứng
        String lastReason = NO_VACANT_BED;
        if (candidates.isEmpty() && cfg.softPreference()) {
            candidates = snap.relaxBuildingThenType(sv, app, cfg); // không nới gender
            lastReason = candidates.isEmpty() ? lastRelaxReason : lastReason;
        }
        Optional<Bed> chosen = candidates.stream()
            .min(roomAwareComparator(sv, snap)); // min = "tốt nhất" theo comparator
        if (chosen.isEmpty()) {
            items.add(waitlist(app, rank, lastReason));
        } else {
            items.add(assign(app, rank, chosen.get()));
            snap.occupy(chosen.get(), sv);
        }
    }
    return new AllocationRunResult(items);
}

@Transactional(isolation = Isolation.READ_COMMITTED)
public AllocationRun commit(long periodId, long actorId) {
    RegistrationPeriod p = periodRepo.lockById(periodId);
    AllocationRunResult planned = plan(periodId, config());
    AllocationRun run = runRepo.save(newRun(p, /*dryRun*/ false, actorId));
    for (AllocationItem it : planned.items()) {
        if (it.getResult() != ASSIGNED) {
            persistItem(run, it);
            updateAppStatus(it); // SKIPPED_* → REJECTED; WAITLISTED → WAITLISTED
            continue;
        }
        Bed bed = bedRepo.lockById(it.getBedId());
        if (bed.getStatus() != VACANT || bed.getCurrentContractId() != null) {
            it = reassignOrWaitlist(it, p); // khóa ứng viên ORDER BY id ASC
        }
        if (it.getResult() == ASSIGNED) {
            // INSERT contract trước, UPDATE bed sau — bắt buộc
            contractService.createDraftFromAllocation(it, p);
        }
        persistItem(run, it);
        updateAppStatus(it);
    }
    p.setStatus(COMPLETED);
    run.markCommitted();
    return run;
    // DataIntegrityViolationException: không bắt trong loop —
    // TX rollback → period trở lại CLOSED; controller flash constraint.
}

/** Comparator "tốt hơn" = nhỏ hơn khi dùng Stream.min */
Comparator<Bed> roomAwareComparator(Student sv, OccupancySnapshot snap) {
    return Comparator
        .comparingInt((Bed b) -> roomBonus(b.getRoom(), sv, snap)).reversed()
        .thenComparing(b -> parseRoomNumber(b.getRoom().getRoomNumber()))
        .thenComparing(Bed::getBedCode)
        .thenComparing(Bed::getId);
}

int roomBonus(Room r, Student sv, OccupancySnapshot snap) {
    boolean sameClass = snap.occupants(r).stream()
        .anyMatch(o -> eq(sv.getClassCode(), o.getClassCode()));
    boolean sameFac = snap.occupants(r).stream()
        .anyMatch(o -> eq(sv.getFacultyCode(), o.getFacultyCode()));
    int pack = snap.occupants(r).size() * 5;
    if (sameClass) return 100 + pack;
    if (sameFac) return 40 + pack;
    return pack;
}

static boolean eq(String a, String b) {
    return a != null && b != null && a.equals(b);
}

/** Số phòng parse được tăng dần; không parse được ("101A","B-02") xếp sau, rồi lexicographic. */
static final int NON_NUMERIC_LAST = Integer.MAX_VALUE;
record RoomSort(int numeric, String raw) implements Comparable<RoomSort> {
    public int compareTo(RoomSort o) {
        int c = Integer.compare(numeric, o.numeric);
        return c != 0 ? c : raw.compareTo(o.raw);
    }
}
RoomSort parseRoomNumber(String raw) {
    String s = raw == null ? "" : raw;
    try { return new RoomSort(Integer.parseInt(s), s); }
    catch (NumberFormatException e) { return new RoomSort(NON_NUMERIC_LAST, s); }
}
```

## 6.3.8. Dry-run / preview

- POST `/admin/allocations/periods/{id}/preview` → lưu `AllocationRun(dry_run=true, COMPLETED)` + items.
- GET `/admin/allocations/runs/{runId}` bảng: hạng, điểm, lý do, giường dự kiến, cảnh báo nới nguyện vọng.
- Nút **Chốt phân bổ** → POST `/admin/allocations/periods/{id}/commit` (confirm modal: “Sẽ khóa giường và tạo hợp đồng nháp”).
- POST discard run dry-run không ảnh hưởng beds.
- Nhiều period **có thể** cùng `ALLOCATING`. Cùng đợt: khóa hàng period. Gán tay không period: khóa `ALLOCATION`. Hai đợt giao giường/SV: unique key, không có mutex toàn cục.

## 6.3.9. Chuyển phòng & trả phòng

- Sinh viên: đơn `RoomChangeRequest` khi HĐ `ACTIVE` hoặc `PENDING_RENEWAL`.
- CHANGE: nêu lý do + nguyện vọng; admin chọn `target_bed` VACANT cùng giới tính → khóa **hai** giường `ORDER BY id ASC` → cập nhật `contract.bed_id`, nhả giường cũ (`VACANT`, `current_contract_id=null`), chiếm giường mới, request `COMPLETED`. Không tạo HĐ mới (v1 ghi note trên contract). Unique `active_bed_key` đổi theo `bed_id` cùng hàng OCCUPYING.
- RETURN: admin duyệt → checkout bắt buộc ([module 6.4](./04-04-hop-dong.md)) từ `ACTIVE|EXPIRED|TERMINATED` → `COMPLETED`, bed VACANT, xử lý cọc.

## 6.3.10. URL

| Method   | Path                                                     | Role    |
| -------- | -------------------------------------------------------- | ------- |
| CRUD     | `/admin/periods`                                       | ADMIN   |
| POST     | `/admin/periods/{id}/open`, `/close`                 | ADMIN   |
| GET/POST | `/student/applications`, `/student/applications/new` | STUDENT |
| POST     | `/student/applications/{id}/withdraw`                  | STUDENT |
| GET      | `/admin/applications?periodId=`                        | ADMIN   |
| GET      | `/admin/allocations/periods/{id}`                      | ADMIN   |
| POST     | `/admin/allocations/periods/{id}/preview`              | ADMIN   |
| GET      | `/admin/allocations/runs/{runId}`                      | ADMIN   |
| POST     | `/admin/allocations/periods/{id}/commit`               | ADMIN   |
| GET/POST | `/admin/allocations/manual`                            | ADMIN   |
| GET/POST | `/student/room-change`, `/student/return-room`       | STUDENT |
| POST     | `/admin/room-changes/{id}/approve`                     | ADMIN   |

**Service:** `RegistrationPeriodService`, `RoomApplicationService`, `AllocationEngine`, `AllocationService`, `RoomChangeService`.

**Edge cases:**

- Preview cũ, ai đó gán tay 1 giường → commit tính lại, UI hiển thị diff (giường đổi / rớt waitlist).
- Period close khi còn DRAFT application → coi như không nộp.
- Student withdraw sau CLOSED: không cho.
- Gán tay vào giường MAINTENANCE: reject.
- Nữ vào tòa Nam: reject mọi path (engine, manual, room-change).

---
