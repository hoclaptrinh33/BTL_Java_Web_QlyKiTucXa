> Đặc tả chức năng & kỹ thuật **v1.6** (Approved) · [← Module dashboard](./04-07-dashboard.md) · [Mục lục](./README.md) · [Máy trạng thái →](./06-may-trang-thai.md)

# 7. API / giao diện HTTP (URL map)

Không public REST resource. Một số endpoint JSON nội bộ cho chart:

- `GET /admin/dashboard/api/occupancy` → `{labels, values}`
- `GET /admin/dashboard/api/debt-by-month`

Toàn bộ form: `application/x-www-form-urlencoded` + CSRF.

## 7.1. Phác thảo SecurityConfig

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(Customizer.withDefaults()) // bật; Thymeleaf hidden _csrf
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
        .requestMatchers("/login", "/register", "/error", "/error/403").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/staff/**").hasAnyRole("ADMIN", "STAFF")
        .requestMatchers("/student/**").hasRole("STUDENT")
        .anyRequest().authenticated())
      .formLogin(form -> form
        .loginPage("/login")
        .successHandler(loginSuccessHandler)
        .failureUrl("/login?error"))
      .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
      .exceptionHandling(e -> e.accessDeniedPage("/error/403"))
      .sessionManagement(s -> s
          .maximumSessions(1)
          .maxSessionsPreventsLogin(false)); // phiên cũ hết hạn
    return http.build();
}

@Bean
HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher(); // bắt buộc để maximumSessions hoạt động
}
```

`UserDetails`: authorities = `ROLE_` + `user.role`.

ADMIN trên `/staff/**` **được phép** và `StaffScope` trả `Optional.empty()` (không lọc tòa). STAFF trên `/admin/**` = 403.

Method security:

- `@PreAuthorize("hasRole('ADMIN')")` — `commit`, `assignManual`, `terminate`, `generateInvoices` (cả tòa / phòng ngoài scope).
- `@PreAuthorize("hasAnyRole('ADMIN','STAFF')")` — `issueUtilityInvoices(roomId, …)` + `StaffScope.assertRoom` (staff chỉ phòng tòa mình; admin không lọc).

Biến môi trường DB **một tên:** `KTX_DB_PASSWORD` (và `KTX_DB_USER`). Không dùng `KTX_DATASOURCE_PASSWORD`.

---

# 8. Giao diện Java cốt lõi

Tên chuẩn của engine là `plan()` (không có `preview()` trùng nghĩa). Preview HTTP gọi `previewAndStore` → `plan()`.

```java
public interface AllocationEngine {
    AllocationRunResult plan(long periodId);
}

public interface AllocationService {
    AllocationRun previewAndStore(long periodId, long actorId);
    AllocationRun commit(long periodId, long actorId);
    Contract assignManual(long studentId, long bedId, Long periodId, String note);
}

public interface ContractService {
    Contract createDraftFromAllocation(AllocationItem item, RegistrationPeriod period);
    void cancelDraft(long contractId); // DRAFT → COMPLETED + nhả giường
    void terminate(long contractId);
}

public interface BillingEngine {
    long tieredElectricity(int kwh); // VND nguyên
    List<Invoice> issueUtilityInvoices(long roomId, YearMonth month);
    Invoice issueRoomFee(long contractId);
    Invoice issueDeposit(long contractId);
    void applyLateFees(LocalDate today); // no-op nếu late_fee > 0
}

public interface InvoiceService {
    /** status=CANCELLED và đổi idempotency_key = old + ":cancelled:" + id, cùng TX. */
    void cancel(long invoiceId, long actorId);
}

public interface OccupancySnapshot {
    List<Bed> vacantMatching(Student s, RoomApplication a, AllocConfig c);
    List<Bed> relaxBuildingThenType(Student s, RoomApplication a, AllocConfig c);
    void occupy(Bed b, Student s);
}

public interface StaffScope {
    Optional<Long> buildingId(Authentication auth); // empty = ADMIN
    void assertBuilding(Authentication auth, long buildingId);
    void assertRoom(Authentication auth, Room room);
}

public final class OccupyingStatuses {
    public static final Set<ContractStatus> OCCUPYING = EnumSet.of(
        DRAFT, ACTIVE, PENDING_RENEWAL, EXPIRED, TERMINATED);
    public static final Set<ContractStatus> FREE = EnumSet.of(COMPLETED);
    public static boolean occupies(ContractStatus s) { return OCCUPYING.contains(s); }
}
```

Form objects tiêu biểu: `RegisterForm`, `RoomApplicationForm`, `UtilityReadingForm`, `ManualAssignForm`, `ViolationForm`.

---
