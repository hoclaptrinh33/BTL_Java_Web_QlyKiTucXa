package com.ktx.history;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ktx.history.HistoryRules.Outcome;

/**
 * Nạp lịch sử vận hành nhiều cơ sở, nhiều năm vào database của profile {@code history}.
 * Ghi JDBC theo lô. Không đi qua DataSeeder, AllocationEngine hay BillingEngine.
 */
@Component
@Profile("history")
@Order(100)
public class HistorySeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HistorySeedRunner.class);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);
    private static final String MARKER = "seed.history.loaded";
    private static final String[] HO = {
            "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"
    };
    private static final String[] TEN = {
            "An", "Bình", "Chi", "Dũng", "Hà", "Hương", "Khoa", "Lan", "Long", "Mai",
            "Nam", "Oanh", "Phúc", "Quân", "Sơn", "Trang", "Uyên", "Vy", "Yến", "Đăng"
    };
    private static final String[] FACULTY = {"CNTT", "QTKD", "NNA", "KTDT", "COKI"};
    private static final String[] HOME = {"Hà Nội", "Bắc Ninh", "Hải Dương", "Nam Định", "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Sơn La"};
    private static final String[] TICKET_STATUS = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "REJECTED"};
    private static final String[] TICKET_PRIO = {"LOW", "MEDIUM", "HIGH"};
    private static final String[] VIO_TYPE = {"LATE_RETURN", "ILLEGAL_COOKING", "DISTURBANCE", "DAMAGE", "OTHER"};
    private static final String[] VIO_SEV = {"MINOR", "MAJOR", "SEVERE"};
    private static final String[] VIO_ACT = {"WARNING", "POINT_DEDUCT", "TERMINATE"};
    private static final String[] NOTI = {"CONTRACT_EXPIRY", "INVOICE", "ALLOCATION", "TICKET", "GENERIC"};
    private static final String[] ASSET_CAT = {"FAN", "BED_FRAME", "CABINET", "DESK", "ELECTRIC_METER", "WATER_METER", "AC", "OTHER"};
    private static final String WEIGHTS = "{\"policy\":1000,\"remote\":500,\"prevGood\":200,\"mode\":\"SOFT\"}";

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationContext applicationContext;
    private final String scaleName;
    private final boolean exitWhenDone;

    private final List<Campus> campuses = new ArrayList<>();
    private final List<Bld> buildings = new ArrayList<>();
    private final List<Rm> rooms = new ArrayList<>();
    private final List<Bd> beds = new ArrayList<>();
    private final List<Stu> students = new ArrayList<>();
    private final ArrayDeque<Integer> waitMale = new ArrayDeque<>();
    private final ArrayDeque<Integer> waitFemale = new ArrayDeque<>();

    private long nextUser = 1;
    private long nextStudent = 1;
    private long nextBuilding = 1;
    private long nextRoom = 1;
    private long nextBed = 1;
    private long nextPeriod = 1;
    private long nextApp = 1;
    private long nextRun = 1;
    private long nextContract = 1;
    private long nextInvoice = 1;
    private final int[] contractSeq = new int[16];
    private final int[] invoiceSeq = new int[16];
    private int cohortSeq = 0;

    private long adminId;
    private long quanLyId;
    private long roleAdmin;
    private long roleQuanLy;
    private long roleCanBo;
    private String passwordHash;
    private int invoicesWritten;
    private int contractsWritten;

    public HistorySeedRunner(DataSource dataSource,
                             PasswordEncoder passwordEncoder,
                             ApplicationContext applicationContext,
                             @Value("${ktx.history.scale:full}") String scaleName,
                             @Value("${ktx.history.exit-when-done:false}") boolean exitWhenDone) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.applicationContext = applicationContext;
        this.scaleName = scaleName;
        this.exitWhenDone = exitWhenDone;
    }

    @Override
    public void run(String... args) {
        long started = System.nanoTime();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            if (alreadyLoaded(conn)) {
                log.info("Đã có mốc {} — bỏ qua seed lịch sử.", MARKER);
            } else {
                passwordHash = passwordEncoder.encode("Admin@123");
                loadRoles(conn);
                Scale scale = "smoke".equalsIgnoreCase(scaleName) ? Scale.smoke() : Scale.full();
                log.info("Seed lịch sử scale={} các năm {}–{} trên database profile history.",
                        scale.name, scale.yearFrom, scale.yearTo);
                seedInventory(conn, scale);
                for (int year = scale.yearFrom; year <= scale.yearTo; year++) {
                    seedYear(conn, year, year == scale.yearTo);
                }
                seedSupplemental(conn, scale);
                flushPeople(conn);
                flushBeds(conn);
                flushSequences(conn);
                writeMarker(conn);
                conn.commit();
                log.info("Seed lịch sử xong: {} sinh viên, {} hợp đồng, {} hóa đơn, {} giây.",
                        students.size(), contractsWritten, invoicesWritten,
                        (System.nanoTime() - started) / 1_000_000_000L);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Seed lịch sử thất bại. Nếu dừng giữa chừng, DROP DATABASE ktx_history rồi chạy lại. " + ex.getMessage(), ex);
        }
        if (exitWhenDone) {
            int code = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(code);
        }
    }

    private boolean alreadyLoaded(Connection conn) throws SQLException {
        int users = count(conn, "SELECT COUNT(*) FROM users");
        int marker = count(conn, "SELECT COUNT(*) FROM system_configs WHERE config_key = '" + MARKER + "'");
        if (marker > 0) {
            return true;
        }
        if (users > 0) {
            throw new IllegalStateException(
                    "Database đã có người dùng nhưng chưa có mốc seed.history.loaded. "
                            + "Hãy trỏ profile history vào database trống ktx_history. "
                            + "Nếu lần seed trước bị đứt: DROP DATABASE ktx_history và tạo lại.");
        }
        return false;
    }

    private void loadRoles(Connection conn) throws SQLException {
        Map<String, Long> roles = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, code FROM roles")) {
            while (rs.next()) {
                roles.put(rs.getString("code"), rs.getLong("id"));
            }
        }
        if (!roles.containsKey("SYSTEM_ADMIN") || !roles.containsKey("QUAN_LY") || !roles.containsKey("CAN_BO")) {
            throw new IllegalStateException("Chưa có vai RBAC. Flyway phải chạy xong trên database trống trước seed lịch sử.");
        }
        roleAdmin = roles.get("SYSTEM_ADMIN");
        roleQuanLy = roles.get("QUAN_LY");
        roleCanBo = roles.get("CAN_BO");
    }

    private void seedInventory(Connection conn, Scale scale) throws SQLException {
        adminId = insertUser(conn, "admin", "admin@ktx.edu.vn", "ADMIN", "INTERNAL", true, LocalDateTime.of(2021, 8, 1, 8, 0));
        quanLyId = insertUser(conn, "quanly", "quanly@ktx.edu.vn", "ADMIN", "INTERNAL", true, LocalDateTime.of(2021, 8, 1, 8, 0));
        linkRole(conn, adminId, roleAdmin);
        linkRole(conn, quanLyId, roleQuanLy);

        int roomSeq = 0;
        for (CampusSpec spec : scale.campuses) {
            Campus campus = new Campus(spec);
            campuses.add(campus);
            for (int b = 0; b < spec.buildings; b++) {
                boolean male = b % 2 == 0;
                String letter = String.valueOf((char) ('A' + b));
                Bld bld = new Bld();
                bld.id = nextBuilding++;
                bld.campus = campuses.size() - 1;
                bld.code = spec.code + "-" + letter;
                bld.name = "Cơ sở " + spec.name + " — Tòa " + letter + (male ? " (Nam)" : " (Nữ)");
                bld.male = male;
                bld.staffUserId = insertUser(conn, "cb" + bld.code.toLowerCase(),
                        "cb." + bld.code.toLowerCase() + "@ktx.edu.vn", "STAFF", "INTERNAL", true,
                        LocalDateTime.of(2021, 8, 15, 8, 0));
                buildings.add(bld);
                insertBuilding(conn, bld);
                linkRole(conn, bld.staffUserId, roleCanBo);
                linkBuilding(conn, bld.staffUserId, bld.id);
                insertStaff(conn, bld);
                for (int floor = 1; floor <= spec.floors; floor++) {
                    for (int r = 1; r <= spec.roomsPerFloor; r++) {
                        Rm room = new Rm();
                        room.id = nextRoom++;
                        room.building = buildings.size() - 1;
                        room.floor = floor;
                        room.number = Integer.toString(floor * 100 + r);
                        room.type = HistoryRules.roomTypeIndex(roomSeq);
                        room.capacity = HistoryRules.CAPACITY[room.type];
                        int seq = roomSeq++;
                        if (seq % 50 == 0) {
                            room.status = "MAINTENANCE";
                        } else if (seq % 37 == 1) {
                            room.status = "INACTIVE";
                        } else {
                            room.status = "ACTIVE";
                        }
                        room.usable = "ACTIVE".equals(room.status);
                        room.elec = 1_000 + seq * 3;
                        room.water = 200 + seq;
                        rooms.add(room);
                        insertRoom(conn, room, bld.id);
                        for (int g = 1; g <= room.capacity; g++) {
                            Bd bed = new Bd();
                            bed.id = nextBed++;
                            bed.room = rooms.size() - 1;
                            bed.code = "G" + g;
                            bed.male = bld.male;
                            bed.usable = room.usable;
                            bed.student = -1;
                            beds.add(bed);
                            insertBed(conn, bed, room.id);
                        }
                        insertAssets(conn, room);
                    }
                }
                insertBuildingImages(conn, bld);
            }
        }
        insertRoomTypeImages(conn);
        conn.commit();
        log.info("Hạ tầng: {} cơ sở, {} tòa, {} phòng, {} giường.", campuses.size(), buildings.size(), rooms.size(), beds.size());
    }

    private void seedYear(Connection conn, int year, boolean current) throws SQLException {
        long[][] periodId = new long[campuses.size()][3];
        for (int c = 0; c < campuses.size(); c++) {
            periodId[c][0] = insertPeriod(conn, year, c, "FRESHMAN", "COMPLETED", "ALL", 50, 400, true);
            periodId[c][1] = insertPeriod(conn, year, c, "NEW_ACADEMIC_YEAR", "COMPLETED", "ALL", 0, 500, false);
            linkCampusBuildings(conn, periodId[c][0], c);
            linkCampusBuildings(conn, periodId[c][1], c);
        }

        List<Integer> returners = new ArrayList<>();
        List<Integer> slotsMale = new ArrayList<>();
        List<Integer> slotsFemale = new ArrayList<>();
        for (int i = 0; i < beds.size(); i++) {
            Bd bed = beds.get(i);
            if (bed.stuck || !bed.usable) {
                continue;
            }
            if (bed.student >= 0) {
                Stu stu = students.get(bed.student);
                if (stu.active && HistoryRules.enrolledAtStart(stu.entryYear, year)) {
                    returners.add(bed.student);
                    continue;
                }
                bed.student = -1;
                stu.bed = -1;
            }
            (bed.male ? slotsMale : slotsFemale).add(i);
        }

        List<Assign> assigned = new ArrayList<>();
        for (int idx : returners) {
            assigned.add(assign(students.get(idx), students.get(idx).bed, year));
        }
        fillSlots(conn, year, slotsMale, true, assigned);
        fillSlots(conn, year, slotsFemale, false, assigned);
        List<Assign> rejected = extraApplicants(conn, year, assigned.size());

        Map<String, List<Assign>> byPeriod = new HashMap<>();
        for (Assign a : assigned) {
            byPeriod.computeIfAbsent(a.campus + ":" + a.newcomer, k -> new ArrayList<>()).add(a);
        }
        for (Assign a : rejected) {
            byPeriod.computeIfAbsent(a.campus + ":" + a.newcomer, k -> new ArrayList<>()).add(a);
        }
        for (List<Assign> group : byPeriod.values()) {
            group.sort(Comparator.comparingInt((Assign a) -> a.score).reversed());
            insertRuns(conn, year, periodId, group);
        }

        List<Stay> stays = new ArrayList<>();
        List<Assign> swapsMale = new ArrayList<>();
        List<Assign> swapsFemale = new ArrayList<>();
        for (Assign a : assigned) {
            if (a.outcome == Outcome.ROOM_CHANGE) {
                (students.get(a.stu).male ? swapsMale : swapsFemale).add(a);
            } else {
                place(conn, year, current, a, -1, stays);
            }
        }
        pairSwaps(conn, year, current, swapsMale, stays);
        pairSwaps(conn, year, current, swapsFemale, stays);
        sideEvents(conn, year, current, assigned);
        bill(conn, year, current, stays);
        if (!current) {
            seedSummer(conn, year + 1, year + 1 == 2026);
        }
        for (int idx : returners) {
            Stu stu = students.get(idx);
            if (stu.active && stu.entryYear + 3 == year && stu.bed >= 0 && !beds.get(stu.bed).stuck) {
                beds.get(stu.bed).student = -1;
                stu.bed = -1;
            }
        }
        for (Assign a : assigned) {
            Stu stu = students.get(a.stu);
            if (stu.bed >= 0 && stu.entryYear + 3 == year && !beds.get(stu.bed).stuck) {
                beds.get(stu.bed).student = -1;
                stu.bed = -1;
            }
        }
        conn.commit();
        log.info("Năm học {}-{} xong (current={}). Hợp đồng cộng dồn {}, hóa đơn cộng dồn {}.",
                year, year + 1, current, contractsWritten, invoicesWritten);
    }

    private void fillSlots(Connection conn, int year, List<Integer> slots, boolean male, List<Assign> assigned) throws SQLException {
        ArrayDeque<Integer> queue = male ? waitMale : waitFemale;
        int extra = Math.max(3, slots.size() / 12);
        int newcomers = slots.size() + extra;
        List<Integer> pool = new ArrayList<>();
        while (!queue.isEmpty()) {
            pool.add(queue.removeFirst());
        }
        for (int i = 0; i < newcomers; i++) {
            pool.add(createStudent(conn, year, male, 100, false, "NONE"));
        }
        pool.removeIf(idx -> !students.get(idx).active || !HistoryRules.enrolledAtStart(students.get(idx).entryYear, year));
        pool.sort(Comparator.comparingInt((Integer idx) -> scoreOf(students.get(idx))).reversed());
        int n = Math.min(slots.size(), pool.size());
        for (int i = 0; i < n; i++) {
            assigned.add(assign(students.get(pool.get(i)), slots.get(i), year));
        }
        for (int i = n; i < pool.size(); i++) {
            queue.addLast(pool.get(i));
        }
    }

    private Assign assign(Stu stu, int bedIndex, int year) {
        Bd bed = beds.get(bedIndex);
        bed.student = stu.index;
        stu.bed = bedIndex;
        Bld bld = buildings.get(rooms.get(bed.room).building);
        Assign a = new Assign();
        a.stu = stu.index;
        a.bed = bedIndex;
        a.periodId = 0;
        a.newcomer = stu.entryYear == year && !stu.everHoused;
        a.score = scoreOf(stu);
        a.outcome = HistoryRules.outcome(stu.id, year);
        a.campus = bld.campus;
        return a;
    }

    private List<Assign> extraApplicants(Connection conn, int year, int base) throws SQLException {
        List<Assign> list = new ArrayList<>();
        int blockedN = Math.max(2, base / 80);
        int lowN = Math.max(2, base / 70);
        int withdrawN = Math.max(2, base / 60);
        for (int i = 0; i < blockedN; i++) {
            boolean male = i % 2 == 0;
            int idx = createStudent(conn, year, male, 20, true, i % 3 == 0 ? "POLICY" : "NONE");
            list.add(unplaced(students.get(idx), year, "REJECTED", "CAM_O"));
        }
        for (int i = 0; i < lowN; i++) {
            boolean male = i % 2 == 1;
            int idx = createStudent(conn, year, male, 40, false, "NONE");
            list.add(unplaced(students.get(idx), year, "REJECTED", "DIEM_THAP"));
        }
        for (int i = 0; i < withdrawN; i++) {
            boolean male = i % 2 == 0;
            int idx = createStudent(conn, year, male, 100, false, i % 2 == 0 ? "REMOTE_AREA" : "NONE");
            list.add(unplaced(students.get(idx), year, "WITHDRAWN", "RUT_DON"));
        }
        int campus = 0;
        for (Integer idx : waitMale) {
            Assign a = unplaced(students.get(idx), year, "WAITLISTED", "HET_GIUONG");
            a.campus = campus % campuses.size();
            list.add(a);
            campus++;
        }
        for (Integer idx : waitFemale) {
            Assign a = unplaced(students.get(idx), year, "WAITLISTED", "HET_GIUONG");
            a.campus = campus % campuses.size();
            list.add(a);
            campus++;
        }
        return list;
    }

    private Assign unplaced(Stu stu, int year, String appStatus, String reason) {
        Assign a = new Assign();
        a.stu = stu.index;
        a.bed = -1;
        a.newcomer = stu.entryYear == year && !stu.everHoused;
        a.score = scoreOf(stu);
        a.appStatus = appStatus;
        a.reason = reason;
        a.outcome = null;
        a.campus = Math.floorMod(stu.index, campuses.size());
        a.periodId = 0;
        return a;
    }

    private void insertRuns(Connection conn, int year, long[][] periodIds, List<Assign> group) throws SQLException {
        if (group.isEmpty()) {
            return;
        }
        int campus = group.get(0).campus;
        boolean freshman = group.get(0).newcomer;
        long period = periodIds[campus][freshman ? 0 : 1];
        for (Assign a : group) {
            a.periodId = period;
            boolean placed = a.bed >= 0;
            String status = a.appStatus != null ? a.appStatus : "ALLOCATED";
            long buildingId = placed
                    ? buildings.get(rooms.get(beds.get(a.bed).room).building).id
                    : firstBuilding(campus, students.get(a.stu).male);
            String roomType = placed ? HistoryRules.ROOM_TYPES[rooms.get(beds.get(a.bed).room).type] : null;
            a.applicationId = nextApp++;
            insertApplication(conn, a, buildingId, roomType, status, year);
        }
        long dry = nextRun++;
        long real = nextRun++;
        int assigned = (int) group.stream().filter(a -> a.bed >= 0).count();
        int wait = (int) group.stream().filter(a -> "WAITLISTED".equals(a.appStatus)).count();
        insertRun(conn, dry, period, true, "DISCARDED", year, assigned, wait, "Xét thử, bỏ vì lệch chỉ tiêu");
        insertRun(conn, real, period, false, "COMMITTED", year, assigned, wait, "Chốt phân bổ");
        int rank = 1;
        try (Batch items = batch(conn, """
                INSERT INTO allocation_items (run_id, application_id, bed_id, rank_no, score, result, reason)
                VALUES (?,?,?,?,?,?,?)
                """)) {
            for (Assign a : group) {
                if ("WITHDRAWN".equals(a.appStatus)) {
                    bindItem(items.ps, dry, a, rank, "SKIPPED", "RUT_DON", false);
                    items.add();
                    rank++;
                    continue;
                }
                String result = a.bed >= 0 ? "ASSIGNED" : ("WAITLISTED".equals(a.appStatus) ? "WAITLISTED" : "SKIPPED");
                String reason = a.bed >= 0 ? "OK" : a.reason;
                String dryResult = result;
                if ("ASSIGNED".equals(result) && rank % 20 == 0) {
                    dryResult = "WAITLISTED";
                }
                bindItem(items.ps, dry, a, rank, dryResult, reason, "ASSIGNED".equals(dryResult));
                items.add();
                bindItem(items.ps, real, a, rank, result, reason, "ASSIGNED".equals(result));
                items.add();
                rank++;
            }
        }
        if (year == 2023 && campus == 0 && freshman) {
            long failed = nextRun++;
            insertRun(conn, failed, period, true, "FAILED", year, 0, 0, "Lỗi giữa chừng khi xét thử");
        }
    }

    private void place(Connection conn, int year, boolean current, Assign a, int destBed, List<Stay> stays) throws SQLException {
        Stu stu = students.get(a.stu);
        Bd bed = beds.get(a.bed);
        Rm room = rooms.get(bed.room);
        long fee = HistoryRules.roomFee(year, room.type);
        int ratio = a.newcomer ? 400 : 500;
        LocalDate start = LocalDate.of(year, 9, 1);
        LocalDate fullEnd = LocalDate.of(year + 1, 6, 30);
        LocalDate shortEnd = LocalDate.of(year + 1, 1, 31);
        Outcome outcome = a.outcome == null ? Outcome.NORMAL : a.outcome;
        if (destBed >= 0) {
            outcome = Outcome.ROOM_CHANGE;
        }
        if (!current && outcome == Outcome.NORMAL && HistoryRules.mix(stu.id, year + 3L) % 5 == 0) {
            writeContract(conn, a, stu, bed, start, shortEnd, fee, ratio, "COMPLETED", "NORMAL_CHECKOUT", "REFUNDED", true, true, year);
            insertRenewal(conn, a.contractId, fullEnd, "REJECTED", LocalDateTime.of(year + 1, 1, 6, 9, 0), LocalDateTime.of(year + 1, 1, 12, 9, 0));
            stays.add(stay(a.contractId, stu.index, bed.room, start, shortEnd));
            stu.everHoused = true;
            stu.prevGood = true;
            return;
        }
        if (outcome == Outcome.CANCEL_BEFORE) {
            writeContract(conn, a, stu, bed, start, fullEnd, fee, ratio, "COMPLETED", "CANCELLED_BEFORE_CHECKIN", "REFUNDED", false, false, year);
            bed.student = -1;
            stu.bed = -1;
            return;
        }
        if (outcome == Outcome.DROPOUT && !current) {
            LocalDate leave = LocalDate.of(year, 12, 15);
            writeContract(conn, a, stu, bed, start, leave, fee, ratio, "COMPLETED", "NORMAL_CHECKOUT", "REFUNDED", true, true, year);
            insertChange(conn, a.contractId, bed, null, null, "RETURN", "COMPLETED", "Nghỉ học giữa kỳ");
            stays.add(stay(a.contractId, stu.index, bed.room, start, leave));
            stu.active = false;
            stu.enabled = false;
            stu.everHoused = true;
            bed.student = -1;
            stu.bed = -1;
            return;
        }
        if (outcome == Outcome.DROPOUT) {
            writeContract(conn, a, stu, bed, start, fullEnd, fee, ratio, "ACTIVE", null, "HELD", true, false, year);
            insertChange(conn, a.contractId, bed, null, null, "RETURN", "SUBMITTED", "Xin nghỉ học, chưa trả phòng");
            stays.add(stay(a.contractId, stu.index, bed.room, start, LocalDate.of(2026, 9, 30)));
            bed.currentContract = a.contractId;
            stu.everHoused = true;
            return;
        }
        if (outcome == Outcome.FORCED && !current) {
            LocalDate leave = LocalDate.of(year, 11, 20);
            writeContract(conn, a, stu, bed, start, leave, fee, ratio, "COMPLETED", "FORCED_AFTER_CHECKOUT", "FORFEITED", true, true, year);
            addViolation(conn, stu, LocalDateTime.of(year, 11, 18, 21, 0), "SEVERE", "TERMINATE", 40);
            stays.add(stay(a.contractId, stu.index, bed.room, start, leave));
            stu.active = false;
            stu.enabled = false;
            stu.blocked = true;
            stu.everHoused = true;
            bed.student = -1;
            stu.bed = -1;
            return;
        }
        if (outcome == Outcome.FORCED) {
            writeContract(conn, a, stu, bed, start, fullEnd, fee, ratio, "TERMINATED", null, "FORFEITED", true, false, year);
            addViolation(conn, stu, LocalDateTime.of(2026, 9, 10, 22, 0), "SEVERE", "TERMINATE", 40);
            stays.add(stay(a.contractId, stu.index, bed.room, start, LocalDate.of(2026, 9, 30)));
            bed.currentContract = a.contractId;
            stu.blocked = true;
            stu.everHoused = true;
            return;
        }
        if (outcome == Outcome.ROOM_CHANGE && destBed >= 0) {
            LocalDate mid = current ? LocalDate.of(2026, 9, 20) : LocalDate.of(year + 1, 1, 14);
            LocalDate next = mid.plusDays(1);
            Bd other = beds.get(destBed);
            writeContract(conn, a, stu, bed, start, mid, fee, ratio, "COMPLETED", "NORMAL_CHECKOUT", "REFUNDED", true, true, year);
            stays.add(stay(a.contractId, stu.index, bed.room, start, mid));
            long first = a.contractId;
            long fee2 = HistoryRules.roomFee(year, rooms.get(other.room).type);
            String secondStatus = current ? "ACTIVE" : "COMPLETED";
            int origin = a.bed;
            a.bed = destBed;
            writeContract(conn, a, stu, other, next, fullEnd, fee2, ratio, secondStatus, current ? null : "NORMAL_CHECKOUT", current ? "HELD" : "REFUNDED", true, !current, year);
            stays.add(stay(a.contractId, stu.index, other.room, next, current ? LocalDate.of(2026, 9, 30) : fullEnd));
            insertChange(conn, first, beds.get(origin), other, HistoryRules.ROOM_TYPES[rooms.get(other.room).type], "CHANGE", "COMPLETED", "Đổi phòng giữa kỳ");
            stu.everHoused = true;
            stu.prevGood = !current;
            return;
        }
        if (current) {
            int kind = HistoryRules.mix(stu.id, year + 99L) % 20;
            if (kind == 0) {
                writeContract(conn, a, stu, bed, start, fullEnd, fee, ratio, "DRAFT", null, "HELD", false, false, year);
                bed.currentContract = a.contractId;
                return;
            }
            if (kind == 1) {
                writeContract(conn, a, stu, bed, start, shortEnd, fee, ratio, "PENDING_RENEWAL", null, "HELD", true, false, year);
                insertRenewal(conn, a.contractId, fullEnd, "SUBMITTED", LocalDateTime.of(2026, 9, 12, 10, 0), null);
                stays.add(stay(a.contractId, stu.index, bed.room, start, LocalDate.of(2026, 9, 30)));
                bed.currentContract = a.contractId;
                stu.everHoused = true;
                return;
            }
        }
        String status = current ? "ACTIVE" : "COMPLETED";
        writeContract(conn, a, stu, bed, start, fullEnd, fee, ratio, status, current ? null : "NORMAL_CHECKOUT", current ? "HELD" : "REFUNDED", true, !current, year);
        stays.add(stay(a.contractId, stu.index, bed.room, start, current ? LocalDate.of(2026, 9, 30) : fullEnd));
        if (!current) {
            insertRenewal(conn, a.contractId, fullEnd, "APPROVED", LocalDateTime.of(year + 1, 1, 5, 9, 0), LocalDateTime.of(year + 1, 1, 10, 9, 0));
            stu.prevGood = true;
        } else if (HistoryRules.mix(stu.id, 23) % 23 == 0) {
            insertRenewal(conn, a.contractId, fullEnd, "CANCELLED", LocalDateTime.of(2026, 9, 8, 9, 0), LocalDateTime.of(2026, 9, 9, 9, 0));
        }
        if (current) {
            bed.currentContract = a.contractId;
        }
        stu.everHoused = true;
    }

    private void pairSwaps(Connection conn, int year, boolean current, List<Assign> swaps, List<Stay> stays) throws SQLException {
        for (int i = 0; i + 1 < swaps.size(); i += 2) {
            Assign left = swaps.get(i);
            Assign right = swaps.get(i + 1);
            int bedL = left.bed;
            int bedR = right.bed;
            place(conn, year, current, left, bedR, stays);
            Assign mirror = new Assign();
            mirror.stu = right.stu;
            mirror.bed = bedR;
            mirror.applicationId = right.applicationId;
            mirror.outcome = Outcome.ROOM_CHANGE;
            place(conn, year, current, mirror, bedL, stays);
            beds.get(bedL).student = right.stu;
            beds.get(bedR).student = left.stu;
            students.get(left.stu).bed = bedR;
            students.get(right.stu).bed = bedL;
            beds.get(bedL).currentContract = current ? mirror.contractId : 0;
            beds.get(bedR).currentContract = current ? left.contractId : 0;
        }
        if (swaps.size() % 2 == 1) {
            Assign last = swaps.get(swaps.size() - 1);
            last.outcome = Outcome.NORMAL;
            place(conn, year, current, last, -1, stays);
        }
    }

    private void seedSummer(Connection conn, int summerYear, boolean expireSome) throws SQLException {
        for (int c = 0; c < campuses.size(); c++) {
            long period = insertPeriod(conn, summerYear - 1, c, "SUMMER", "COMPLETED", "ALL", 0, 1000, false);
            linkCampusBuildings(conn, period, c);
        }
        List<Stay> stays = new ArrayList<>();
        List<Assign> group = new ArrayList<>();
        for (Stu stu : students) {
            if (!stu.active || stu.bed < 0 || beds.get(stu.bed).stuck) {
                continue;
            }
            if (HistoryRules.mix(stu.id, summerYear) % 8 != 0) {
                continue;
            }
            Bd bed = beds.get(stu.bed);
            if (!bed.usable) {
                continue;
            }
            int campus = buildings.get(rooms.get(bed.room).building).campus;
            Assign a = new Assign();
            a.stu = stu.index;
            a.bed = stu.bed;
            a.campus = campus;
            a.score = scoreOf(stu);
            a.newcomer = false;
            a.appStatus = "ALLOCATED";
            a.periodId = findSummerPeriod(conn, summerYear, campus);
            a.applicationId = nextApp++;
            insertApplication(conn, a, buildings.get(rooms.get(bed.room).building).id,
                    HistoryRules.ROOM_TYPES[rooms.get(bed.room).type], "ALLOCATED", summerYear - 1);
            group.add(a);
        }
        if (group.isEmpty()) {
            return;
        }
        Map<Long, List<Assign>> byPeriod = new HashMap<>();
        for (Assign a : group) {
            byPeriod.computeIfAbsent(a.periodId, k -> new ArrayList<>()).add(a);
        }
        for (List<Assign> g : byPeriod.values()) {
            g.sort(Comparator.comparingInt((Assign a) -> a.score).reversed());
            long dry = nextRun++;
            long real = nextRun++;
            insertRun(conn, dry, g.get(0).periodId, true, "DISCARDED", summerYear, g.size(), 0, "Xét thử đợt hè");
            insertRun(conn, real, g.get(0).periodId, false, "COMMITTED", summerYear, g.size(), 0, "Chốt đợt hè");
            int rank = 1;
            try (Batch items = batch(conn, """
                    INSERT INTO allocation_items (run_id, application_id, bed_id, rank_no, score, result, reason)
                    VALUES (?,?,?,?,?,?,?)
                    """)) {
                for (Assign a : g) {
                    bindItem(items.ps, dry, a, rank, "ASSIGNED", "OK", true);
                    items.add();
                    bindItem(items.ps, real, a, rank, "ASSIGNED", "OK", true);
                    items.add();
                    rank++;
                }
            }
        }
        LocalDate start = LocalDate.of(summerYear, 7, 1);
        LocalDate end = LocalDate.of(summerYear, 8, 20);
        for (Assign a : group) {
            Stu stu = students.get(a.stu);
            Bd bed = beds.get(a.bed);
            long fee = HistoryRules.roomFee(summerYear, rooms.get(bed.room).type) * 40 / 100;
            boolean expire = expireSome && HistoryRules.mix(stu.id, 2026) % 211 == 0 && stu.entryYear + 3 != summerYear - 1;
            String status = expire ? "EXPIRED" : "COMPLETED";
            writeContract(conn, a, stu, bed, start, end, fee, 1000, status, expire ? null : "NORMAL_CHECKOUT",
                    expire ? "HELD" : "REFUNDED", true, !expire, summerYear);
            stays.add(stay(a.contractId, stu.index, bed.room, start, end));
            if (expire) {
                bed.stuck = true;
                bed.currentContract = a.contractId;
                bed.student = stu.index;
                stu.bed = a.bed;
            }
        }
        bill(conn, summerYear, false, stays);
    }

    private long findSummerPeriod(Connection conn, int summerYear, int campus) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM registration_periods WHERE period_type = 'SUMMER' AND academic_year = ? AND name LIKE ?")) {
            ps.setString(1, (summerYear - 1) + "-" + summerYear);
            ps.setString(2, "%Hè " + summerYear + "%" + campuses.get(campus).spec.name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không thấy đợt hè " + summerYear);
    }

    private void seedSupplemental(Connection conn, Scale scale) throws SQLException {
        int year = scale.yearTo;
        int campusOpen = 0;
        int campusAlloc = Math.min(1, campuses.size() - 1);
        int campusClosed = Math.min(2, campuses.size() - 1);
        long open = insertPeriod(conn, year, campusOpen, "FRESHMAN", "OPEN", "MALE_ONLY", 50, 400, true);
        long allocating = insertPeriod(conn, year, campusAlloc, "FRESHMAN", "ALLOCATING", "FEMALE_ONLY", 50, 400, true);
        long closed = insertPeriod(conn, year, campusClosed, "NEW_ACADEMIC_YEAR", "CLOSED", "ALL", 0, 500, false);
        long draft = insertPeriod(conn, year, 0, "SUMMER", "DRAFT", "ALL", 0, 1000, false);
        renamePeriod(conn, open, "Đợt bổ sung tân sinh viên K" + (year % 100) + " — Cơ sở " + campuses.get(campusOpen).spec.name);
        renamePeriod(conn, allocating, "Đợt đang xét tân sinh viên K" + (year % 100) + " — Cơ sở " + campuses.get(campusAlloc).spec.name);
        renamePeriod(conn, closed, "Đợt ở tiếp đã đóng đơn " + year + "-" + (year + 1) + " — Cơ sở " + campuses.get(campusClosed).spec.name);
        linkCampusBuildings(conn, open, campusOpen);
        linkCampusBuildings(conn, allocating, campusAlloc);
        linkCampusBuildings(conn, closed, campusClosed);
        linkCampusBuildings(conn, draft, 0);
        List<Assign> openApps = lateApps(conn, open, year, true, 24, "OPEN");
        List<Assign> allocApps = lateApps(conn, allocating, year, false, 24, "ALLOCATING");
        lateApps(conn, closed, year, true, 16, "CLOSED");
        if (!openApps.isEmpty()) {
            long preview = nextRun++;
            insertRun(conn, preview, open, true, "COMPLETED", year, 0, openApps.size(), "Xem trước đợt đang mở, chưa chốt");
            writeSimpleItems(conn, preview, openApps, "WAITLISTED");
        }
        if (!allocApps.isEmpty()) {
            insertRun(conn, nextRun++, allocating, true, "PENDING", year, 0, allocApps.size(), "Chờ xét");
            long running = nextRun++;
            insertRun(conn, running, allocating, true, "RUNNING", year, 0, allocApps.size(), "Đang xét");
            writeSimpleItems(conn, running, allocApps, "SKIPPED");
        }
        conn.commit();
    }

    private List<Assign> lateApps(Connection conn, long period, int year, boolean male, int count, String periodStatus) throws SQLException {
        List<Assign> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int idx = createStudent(conn, year, male, 90, false, i % 5 == 0 ? "POLICY" : "NONE");
            Stu stu = students.get(idx);
            Assign a = new Assign();
            a.stu = idx;
            a.bed = -1;
            a.periodId = period;
            a.score = scoreOf(stu);
            a.applicationId = nextApp++;
            a.newcomer = true;
            String status = "OPEN".equals(periodStatus) && i % 2 == 0 ? "DRAFT" : "SUBMITTED";
            a.appStatus = status;
            insertApplication(conn, a, firstBuilding(0, male), null, status, year);
            list.add(a);
        }
        return list;
    }

    private void sideEvents(Connection conn, int year, boolean current, List<Assign> assigned) throws SQLException {
        int n = 0;
        for (Assign a : assigned) {
            if (a.contractId == 0 || a.bed < 0) {
                continue;
            }
            Stu stu = students.get(a.stu);
            Rm room = rooms.get(beds.get(a.bed).room);
            n++;
            if (n % 35 == 0) {
                insertTicket(conn, stu, room, year, n);
            }
            if (n % 40 == 0 && stu.active) {
                String type = VIO_TYPE[n % VIO_TYPE.length];
                String sev = VIO_SEV[n % VIO_SEV.length];
                String act = "DAMAGE".equals(type) ? "POINT_DEDUCT" : VIO_ACT[n % VIO_ACT.length];
                if ("TERMINATE".equals(act) && current) {
                    act = "POINT_DEDUCT";
                }
                int points = "WARNING".equals(act) ? 0 : ("MAJOR".equals(sev) ? 15 : 5);
                addViolation(conn, stu, LocalDateTime.of(year, 10, 1 + (n % 20), 20, 0), sev, act, points);
                if ("DAMAGE".equals(type)) {
                    insertOtherInvoice(conn, stu, room, a.contractId, year);
                }
            }
            if (n % 30 == 0) {
                insertNotification(conn, stu, year, n);
            }
            if (n % 29 == 0 && a.outcome == Outcome.NORMAL) {
                String st = current ? "SUBMITTED" : new String[] {"REJECTED", "CANCELLED", "APPROVED"}[n % 3];
                if ("APPROVED".equals(st) && !current) {
                    st = "REJECTED";
                }
                insertChange(conn, a.contractId, beds.get(a.bed), null, null, "CHANGE", st, "Đơn đổi phòng không thực hiện");
            }
        }
        insertAudit(conn, quanLyId, "quanly", "QUAN_LY", "PERIOD_COMMIT", "REGISTRATION_PERIOD", Integer.toString(year),
                "Chốt các đợt năm học " + year + "-" + (year + 1), "SUCCESS", LocalDateTime.of(year, 8, 20, 9, 0));
        if (year == 2023) {
            insertAudit(conn, quanLyId, "quanly", "QUAN_LY", "ALLOCATION_DRY_RUN", "ALLOCATION_RUN", "2023",
                    "Lần xét thử năm 2023 lỗi giữa chừng", "FAILURE", LocalDateTime.of(2023, 8, 18, 11, 0));
        }
    }

    private void bill(Connection conn, int priceYear, boolean current, List<Stay> stays) throws SQLException {
        if (stays.isEmpty()) {
            return;
        }
        LocalDate from = stays.stream().map(s -> s.from.withDayOfMonth(1)).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = stays.stream().map(s -> s.to.withDayOfMonth(1)).max(LocalDate::compareTo).orElseThrow();
        if (current) {
            to = LocalDate.of(2026, 9, 1);
        }
        for (LocalDate month = from; !month.isAfter(to); month = month.plusMonths(1)) {
            LocalDate mid = month.withDayOfMonth(Math.min(15, month.lengthOfMonth()));
            Map<Integer, List<Stay>> byRoom = new HashMap<>();
            for (Stay stay : stays) {
                if (!stay.from.isAfter(mid) && !stay.to.isBefore(mid)) {
                    byRoom.computeIfAbsent(stay.room, k -> new ArrayList<>()).add(stay);
                }
            }
            for (Map.Entry<Integer, List<Stay>> entry : byRoom.entrySet()) {
                billRoomMonth(conn, priceYear, current, month, entry.getKey(), entry.getValue());
            }
            conn.commit();
        }
    }

    private void billRoomMonth(Connection conn, int priceYear, boolean current, LocalDate month, int roomIndex, List<Stay> occupants) throws SQLException {
        Rm room = rooms.get(roomIndex);
        Bld bld = buildings.get(room.building);
        int kwh = 40 + Math.floorMod(roomIndex * 13 + month.getMonthValue(), 500);
        int m3 = 4 + Math.floorMod(roomIndex + month.getMonthValue(), 28);
        boolean first = !room.meterStarted;
        boolean elecReplaced = room.meterStarted && Math.floorMod(roomIndex + month.getMonthValue(), 47) == 0;
        boolean waterReplaced = room.meterStarted && Math.floorMod(roomIndex + month.getMonthValue(), 53) == 0;
        int elecPrev = first && roomIndex % 17 == 0 ? 0 : room.elec;
        int waterPrev = first && roomIndex % 19 == 0 ? 0 : room.water;
        boolean newMeter = first && (roomIndex % 17 == 0 || roomIndex % 19 == 0);
        int elecOldFinal = elecReplaced ? elecPrev + kwh / 2 : 0;
        int elecNewStart = elecReplaced ? 0 : 0;
        int elecCurr = elecReplaced ? kwh - kwh / 2 : elecPrev + kwh;
        int waterOldFinal = waterReplaced ? waterPrev + m3 / 2 : 0;
        int waterCurr = waterReplaced ? Math.max(1, m3 - m3 / 2) : waterPrev + m3;
        room.elec = elecCurr;
        room.water = waterCurr;
        room.meterStarted = true;
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO utility_readings (
                    room_id, billing_month, elec_prev, elec_curr, water_prev, water_curr,
                    elec_replaced, water_replaced, elec_old_final, elec_new_start, water_old_final, water_new_start,
                    new_building_meter, recorded_by, recorded_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, room.id);
            ps.setObject(2, month);
            ps.setInt(3, elecPrev);
            ps.setInt(4, elecCurr);
            ps.setInt(5, waterPrev);
            ps.setInt(6, waterCurr);
            ps.setBoolean(7, elecReplaced);
            ps.setBoolean(8, waterReplaced);
            setIntOrNull(ps, 9, elecReplaced ? elecOldFinal : null);
            setIntOrNull(ps, 10, elecReplaced ? elecNewStart : null);
            setIntOrNull(ps, 11, waterReplaced ? waterOldFinal : null);
            setIntOrNull(ps, 12, waterReplaced ? 0 : null);
            ps.setBoolean(13, newMeter);
            ps.setLong(14, bld.staffUserId);
            ps.setObject(15, month.plusDays(2).atTime(9, 0));
            ps.executeUpdate();
        }
        int n = occupants.size();
        int[] prices = HistoryRules.electricity(month);
        int[] units = HistoryRules.tierUnits(kwh);
        long waterUnit = HistoryRules.waterPerM3(month);
        long roomWater = m3 * waterUnit;
        long inet = HistoryRules.internetPerRoom(priceYear);
        long san = HistoryRules.sanitation(priceYear);
        long park = HistoryRules.parking(priceYear);
        long[] elecShare = new long[n];
        long[][][] lines = new long[n][][];
        for (int t = 0; t < units.length; t++) {
            if (units[t] == 0) {
                continue;
            }
            long tierCost = (long) units[t] * prices[t];
            long given = 0;
            for (int i = 0; i < n; i++) {
                int u = (int) HistoryRules.share(units[t], n, i);
                long amt = (i == n - 1) ? tierCost - given : (long) u * prices[t];
                given += (i == n - 1) ? 0 : amt;
                if (i == n - 1) {
                    given = tierCost;
                }
                elecShare[i] += amt;
                lines[i] = append(lines[i], new long[] {u, prices[t], amt});
            }
        }
        for (int i = 0; i < n; i++) {
            Stay stay = occupants.get(i);
            Stu stu = students.get(stay.stu);
            long water = HistoryRules.share(roomWater, n, i);
            long internet = HistoryRules.share(inet, n, i);
            long subtotal = elecShare[i] + water + san + park + internet;
            boolean cancelled = false;
            writeUtilityInvoice(conn, priceYear, current, month, stu, room, stay.contractId, subtotal, cancelled,
                    lines[i], water, waterUnit, m3, n, internet, inet, san, park);
        }
    }

    private void writeUtilityInvoice(Connection conn, int priceYear, boolean current, LocalDate month, Stu stu, Rm room,
                                     long contractId, long subtotal, boolean cancelled, long[][] elecLines, long water,
                                     long waterUnit, int m3, int people, long internet, long internetFull, long san, long park) throws SQLException {
        int salt = HistoryRules.mix(contractId, month.toEpochDay());
        String status;
        LocalDate due = month.plusDays(10);
        LocalDateTime paidAt = null;
        if (cancelled) {
            status = "CANCELLED";
        } else if (!current && month.getYear() == 2026 && month.getMonthValue() <= 6 && (salt % 10 == 2 || salt % 10 == 5)) {
            status = "OVERDUE";
        } else if (!current && salt % 500 == 0) {
            status = "OVERDUE";
        } else if (!current) {
            status = "PAID";
            paidAt = due.minusDays(1).atTime(10, 0);
        } else if (salt % 100 < 80) {
            status = "PAID";
            paidAt = LocalDateTime.of(2026, 9, 12, 10, 0);
        } else if (salt % 100 < 90) {
            status = "OVERDUE";
        } else {
            status = "UNPAID";
            due = LocalDate.of(2026, 10, 10);
        }
        long late = "OVERDUE".equals(status) ? subtotal * 5 / 100 : 0;
        long invoiceId = nextInvoice++;
        String no = invoiceNo(month.getYear());
        insertInvoiceRow(conn, invoiceId, no, stu.id, room.id, contractId, "UTILITY", month, subtotal, late, due, status, paidAt,
                "U:" + contractId + ":" + month);
        try (Batch items = batch(conn, """
                INSERT INTO invoice_items (invoice_id, description, qty, unit_price, amount, item_code)
                VALUES (?,?,?,?,?,?)
                """)) {
            if (elecLines != null) {
                for (long[] line : elecLines) {
                    bindItemMoney(items.ps, invoiceId, "Tiền điện bậc " + line[1] + " đ/kWh tháng " + month,
                            line[0], line[1], line[2], "ELEC");
                    items.add();
                }
            }
            bindItemMoney(items.ps, invoiceId, "Tiền nước " + m3 + " m³ / " + people + " người",
                    Math.max(1, m3), waterUnit, water, "WATER");
            items.add();
            bindItemMoney(items.ps, invoiceId, "Phí internet phòng chia đều", 1, internetFull, internet, "INTERNET");
            items.add();
            bindItemMoney(items.ps, invoiceId, "Phí vệ sinh", 1, san, san, "SANITATION");
            items.add();
            bindItemMoney(items.ps, invoiceId, "Phí gửi xe", 1, park, park, "PARKING");
            items.add();
        }
        if ("PAID".equals(status)) {
            insertPayment(conn, invoiceId, subtotal + late, salt % 2 == 0 ? "CASH" : "BANK_TRANSFER", paidAt, "PT" + invoiceId);
        }
        invoicesWritten++;
    }

    private void writeContract(Connection conn, Assign a, Stu stu, Bd bed, LocalDate start, LocalDate end, long fee, int ratioMillis,
                               String status, String reason, String depositStatus, boolean checkIn, boolean checkOut, int numberYear) throws SQLException {
        long id = nextContract++;
        a.contractId = id;
        long deposit = HistoryRules.deposit(fee, ratioMillis);
        LocalDateTime signed = checkIn ? start.atTime(8, 0) : null;
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO contracts (
                    id, contract_no, student_id, bed_id, application_id, start_date, end_date,
                    room_fee, deposit_amount, deposit_status, status, completion_reason, terms_version, signed_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, id);
            ps.setString(2, contractNo(numberYear));
            ps.setLong(3, stu.id);
            ps.setLong(4, bed.id);
            if (a.applicationId == 0) {
                ps.setNull(5, Types.BIGINT);
            } else {
                ps.setLong(5, a.applicationId);
            }
            ps.setObject(6, start);
            ps.setObject(7, end);
            ps.setLong(8, fee);
            ps.setLong(9, deposit);
            ps.setString(10, depositStatus);
            ps.setString(11, status);
            if (reason == null) {
                ps.setNull(12, Types.VARCHAR);
            } else {
                ps.setString(12, reason);
            }
            ps.setString(13, "DK-" + start.getYear());
            if (signed == null) {
                ps.setNull(14, Types.TIMESTAMP);
            } else {
                ps.setObject(14, signed);
            }
            ps.executeUpdate();
        }
        contractsWritten++;
        long staff = buildings.get(rooms.get(bed.room).building).staffUserId;
        if (checkIn) {
            insertCheck(conn, id, "CHECK_IN", start.atTime(8, 30), staff, "Bàn giao tài sản đủ, tình trạng tốt");
        }
        if (checkOut) {
            insertCheck(conn, id, "CHECK_OUT", end.atTime(16, 0), staff, "Đối soát tài sản khi trả phòng");
        }
        issueFixedInvoice(conn, stu, rooms.get(bed.room), id, "ROOM_TERM", fee, start, "R:" + id,
                "CANCELLED_BEFORE_CHECKIN".equals(reason), numberYear);
        issueFixedInvoice(conn, stu, rooms.get(bed.room), id, "DEPOSIT", deposit, start, "D:" + id,
                "CANCELLED_BEFORE_CHECKIN".equals(reason), numberYear);
        if ("COMPLETED".equals(status) || "ACTIVE".equals(status) || "PENDING_RENEWAL".equals(status) || "TERMINATED".equals(status) || "EXPIRED".equals(status)) {
            // occupying or finished contracts keep the bed pointer only when still occupying
        }
    }

    private void issueFixedInvoice(Connection conn, Stu stu, Rm room, long contractId, String type, long amount, LocalDate day,
                                   String key, boolean cancelled, int numberYear) throws SQLException {
        int salt = HistoryRules.mix(contractId, type.hashCode());
        String status;
        LocalDate due = day.plusDays(7);
        LocalDateTime paidAt = null;
        boolean current = !day.isBefore(TODAY.withDayOfMonth(1)) && day.getYear() == 2026 && day.getMonthValue() >= 9;
        if (cancelled) {
            status = "CANCELLED";
        } else if (current && salt % 100 >= 85) {
            status = salt % 100 >= 95 ? "OVERDUE" : "UNPAID";
            if ("UNPAID".equals(status)) {
                due = LocalDate.of(2026, 10, 10);
            }
        } else if (!current && !cancelled && salt % 500 == 0) {
            status = "OVERDUE";
        } else {
            status = "PAID";
            paidAt = due.minusDays(1).atTime(11, 0);
        }
        long late = "OVERDUE".equals(status) ? amount * 5 / 100 : 0;
        long invoiceId = nextInvoice++;
        insertInvoiceRow(conn, invoiceId, invoiceNo(numberYear), stu.id, room.id, contractId, type, day, amount, late, due, status, paidAt, key);
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO invoice_items (invoice_id, description, qty, unit_price, amount, item_code)
                VALUES (?,?,?,?,?,?)
                """)) {
            bindItemMoney(ps, invoiceId, "ROOM_TERM".equals(type) ? "Tiền phòng kỳ " + day : "Tiền đặt cọc", 1, amount, amount, type);
            ps.executeUpdate();
        }
        if ("PAID".equals(status)) {
            insertPayment(conn, invoiceId, amount + late, salt % 2 == 0 ? "BANK_TRANSFER" : "CASH", paidAt, "PT" + invoiceId);
        }
        invoicesWritten++;
    }

    private void insertOtherInvoice(Connection conn, Stu stu, Rm room, long contractId, int year) throws SQLException {
        long amount = 200_000L;
        long invoiceId = nextInvoice++;
        LocalDate day = LocalDate.of(year, 10, 20);
        LocalDateTime paidAt = day.plusDays(3).atTime(14, 0);
        insertInvoiceRow(conn, invoiceId, invoiceNo(year), stu.id, room.id, contractId, "OTHER", day, amount, 0, day.plusDays(10),
                "PAID", paidAt, "O:" + contractId + ":DMG");
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO invoice_items (invoice_id, description, qty, unit_price, amount, item_code)
                VALUES (?,?,?,?,?,?)
                """)) {
            bindItemMoney(ps, invoiceId, "Bồi thường tài sản hư hỏng", 1, amount, amount, "OTHER");
            ps.executeUpdate();
        }
        insertPayment(conn, invoiceId, amount, "CASH", paidAt, "PT" + invoiceId);
        invoicesWritten++;
    }

    private int createStudent(Connection conn, int entryYear, boolean male, int conduct, boolean blocked, String priority) throws SQLException {
        cohortSeq++;
        Stu stu = new Stu();
        stu.index = students.size();
        stu.id = nextStudent++;
        stu.userId = nextUser++;
        stu.entryYear = entryYear;
        stu.male = male;
        stu.conduct = conduct;
        stu.blocked = blocked;
        stu.active = true;
        stu.enabled = true;
        stu.priority = priority;
        String faculty = FACULTY[cohortSeq % FACULTY.length];
        stu.code = String.format("D%02d%s%05d", entryYear % 100, faculty, cohortSeq);
        stu.name = HO[cohortSeq % HO.length] + " " + (male ? (cohortSeq % 5 == 0 ? "Minh" : "Văn") : (cohortSeq % 5 == 0 ? "Ngọc" : "Thị"))
                + " " + TEN[cohortSeq % TEN.length];
        stu.bed = -1;
        LocalDateTime created = LocalDateTime.of(entryYear, 8, 20, 9, 0);
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, username, email, password_hash, role, enabled, last_login_at, created_at, updated_at, account_kind)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, stu.userId);
            ps.setString(2, stu.code);
            ps.setString(3, stu.code.toLowerCase() + "@sv.ktx.edu.vn");
            ps.setString(4, passwordHash);
            ps.setString(5, "STUDENT");
            ps.setBoolean(6, true);
            if (cohortSeq % 3 == 0) {
                ps.setObject(7, created.plusDays(3));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setObject(8, created);
            ps.setObject(9, created);
            ps.setString(10, "STUDENT");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO students (
                    id, user_id, student_code, full_name, gender, date_of_birth, faculty_code, class_code,
                    phone, emergency_name, emergency_phone, hometown, priority_category, previous_stay_good,
                    conduct_score, blocked_from_housing)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, stu.id);
            ps.setLong(2, stu.userId);
            ps.setString(3, stu.code);
            ps.setString(4, stu.name);
            ps.setString(5, male ? "MALE" : "FEMALE");
            ps.setObject(6, LocalDate.of(entryYear - 18, 1 + (cohortSeq % 12), 1 + (cohortSeq % 28)));
            ps.setString(7, faculty);
            ps.setString(8, String.format("D%02d%s%02d", entryYear % 100, faculty, 1 + (cohortSeq % 8)));
            ps.setString(9, String.format("09%08d", cohortSeq % 100_000_000));
            ps.setString(10, "Phụ huynh " + HO[cohortSeq % HO.length]);
            ps.setString(11, String.format("08%08d", (cohortSeq * 3) % 100_000_000));
            ps.setString(12, HOME[cohortSeq % HOME.length]);
            ps.setString(13, priority);
            ps.setBoolean(14, false);
            ps.setInt(15, conduct);
            ps.setBoolean(16, blocked);
            ps.executeUpdate();
        }
        students.add(stu);
        return stu.index;
    }

    private long insertUser(Connection conn, String username, String email, String role, String kind, boolean enabled, LocalDateTime created) throws SQLException {
        long id = nextUser++;
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, username, email, password_hash, role, enabled, last_login_at, created_at, updated_at, account_kind)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, id);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.setString(5, role);
            ps.setBoolean(6, enabled);
            ps.setObject(7, created.plusDays(1));
            ps.setObject(8, created);
            ps.setObject(9, created);
            ps.setString(10, kind);
            ps.executeUpdate();
        }
        return id;
    }

    private void insertBuilding(Connection conn, Bld bld) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO buildings (id, code, name, gender_policy, active) VALUES (?,?,?,?,?)")) {
            ps.setLong(1, bld.id);
            ps.setString(2, bld.code);
            ps.setString(3, bld.name);
            ps.setString(4, bld.male ? "MALE" : "FEMALE");
            ps.setBoolean(5, true);
            ps.executeUpdate();
        }
    }

    private void insertStaff(Connection conn, Bld bld) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO staff (user_id, full_name, phone, assigned_building_id) VALUES (?,?,?,?)")) {
            ps.setLong(1, bld.staffUserId);
            ps.setString(2, "Cán bộ " + bld.code);
            ps.setString(3, String.format("024%07d", bld.id));
            ps.setLong(4, bld.id);
            ps.executeUpdate();
        }
    }

    private void insertRoom(Connection conn, Rm room, long buildingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO rooms (id, building_id, room_number, floor, room_type, capacity, price_per_term, status)
                VALUES (?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, room.id);
            ps.setLong(2, buildingId);
            ps.setString(3, room.number);
            ps.setInt(4, room.floor);
            ps.setString(5, HistoryRules.ROOM_TYPES[room.type]);
            ps.setInt(6, room.capacity);
            ps.setLong(7, HistoryRules.roomFee(2026, room.type));
            ps.setString(8, room.status);
            ps.executeUpdate();
        }
    }

    private void insertBed(Connection conn, Bd bed, long roomId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO beds (id, room_id, bed_code, status, current_contract_id, version) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, bed.id);
            ps.setLong(2, roomId);
            ps.setString(3, bed.code);
            ps.setString(4, bed.usable ? "VACANT" : "MAINTENANCE");
            ps.setNull(5, Types.BIGINT);
            ps.setLong(6, 0);
            ps.executeUpdate();
        }
    }

    private void insertAssets(Connection conn, Rm room) throws SQLException {
        try (Batch batch = batch(conn, """
                INSERT INTO room_assets (room_id, name, category, quantity, `condition`, note, serial_number)
                VALUES (?,?,?,?,?,?,?)
                """)) {
            for (int i = 0; i < ASSET_CAT.length; i++) {
                String cond = "GOOD";
                if ("CABINET".equals(ASSET_CAT[i]) && room.id % 17 == 0) {
                    cond = "DAMAGED";
                } else if ("AC".equals(ASSET_CAT[i]) && room.id % 19 == 0) {
                    cond = "MAINTENANCE";
                }
                PreparedStatement ps = batch.ps;
                ps.setLong(1, room.id);
                ps.setString(2, ASSET_CAT[i]);
                ps.setString(3, ASSET_CAT[i]);
                ps.setInt(4, 1);
                ps.setString(5, cond);
                ps.setString(6, null);
                ps.setString(7, room.id + "-" + ASSET_CAT[i]);
                batch.add();
            }
        }
    }

    private void insertBuildingImages(Connection conn, Bld bld) throws SQLException {
        String[] maleImages = {
            "/images/buildings/building-male-1.jpg",
            "/images/buildings/building-sports-1.jpg",
            "/images/buildings/building-modern-1.jpg",
            "/images/buildings/building-urban-1.jpg"
        };
        String[] femaleImages = {
            "/images/buildings/building-female-1.jpg",
            "/images/buildings/building-garden-1.jpg"
        };

        boolean isFemale = (bld.genderPolicy != null && bld.genderPolicy.name().equalsIgnoreCase("FEMALE"))
                || (bld.name != null && bld.name.contains("Nữ"));
        String[] pool = isFemale ? femaleImages : maleImages;
        int primaryIdx = (int) (Math.abs(bld.id) % pool.length);
        int secondaryIdx = (primaryIdx + 1) % pool.length;

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO building_images (building_id, image_url, caption, is_primary, display_order, created_at)
                VALUES (?,?,?,?,?,?)
                """)) {
            for (int i = 0; i < 2; i++) {
                String imgUrl = (i == 0) ? pool[primaryIdx] : pool[secondaryIdx];
                ps.setLong(1, bld.id);
                ps.setString(2, imgUrl);
                ps.setString(3, (i == 0) ? (bld.name + " - Mặt tiền chính") : (bld.name + " - Khuôn viên & tiện ích"));
                ps.setBoolean(4, i == 0);
                ps.setInt(5, i);
                ps.setObject(6, LocalDateTime.of(2021, 9, 1, 8, 0));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private String getRoomImageUrl(String roomType) {
        if ("VIP_AC".equalsIgnoreCase(roomType)) {
            return "/images/rooms/room-vip-ac.jpg";
        } else if ("STANDARD_8".equalsIgnoreCase(roomType)) {
            return "/images/rooms/room-standard-8.jpg";
        } else if ("STANDARD_6".equalsIgnoreCase(roomType)) {
            return "/images/rooms/room-standard-6.jpg";
        } else {
            return "/images/rooms/room-standard-4.jpg";
        }
    }

    private void insertRoomTypeImages(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO room_images (room_type, building_id, room_id, image_url, caption, is_primary, display_order, created_at)
                VALUES (?,?,?,?,?,?,?,?)
                """)) {
            for (Bld bld : buildings) {
                for (int t = 0; t < HistoryRules.ROOM_TYPES.length; t++) {
                    String rt = HistoryRules.ROOM_TYPES[t];
                    ps.setString(1, rt);
                    ps.setLong(2, bld.id);
                    ps.setNull(3, Types.BIGINT);
                    ps.setString(4, getRoomImageUrl(rt));
                    ps.setString(5, "Loại " + rt + " tại " + bld.code);
                    ps.setBoolean(6, true);
                    ps.setInt(7, t);
                    ps.setObject(8, LocalDateTime.of(2021, 9, 1, 8, 0));
                    ps.addBatch();
                }
            }
            int n = 0;
            for (Rm room : rooms) {
                if (n++ % 10 != 0) {
                    continue;
                }
                String rt = HistoryRules.ROOM_TYPES[room.type];
                ps.setString(1, rt);
                ps.setLong(2, buildings.get(room.building).id);
                ps.setLong(3, room.id);
                ps.setString(4, getRoomImageUrl(rt));
                ps.setString(5, "Phòng " + room.number);
                ps.setBoolean(6, false);
                ps.setInt(7, 1);
                ps.setObject(8, LocalDateTime.of(2021, 9, 1, 8, 0));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private long insertPeriod(Connection conn, int academicStart, int campus, String type, String status, String genderScope,
                              int minConduct, int ratioMillis, boolean proof) throws SQLException {
        Campus c = campuses.get(campus);
        long id = nextPeriod++;
        LocalDate termStart;
        LocalDate termEnd;
        LocalDateTime open;
        LocalDateTime close;
        String name;
        String cohort;
        if ("SUMMER".equals(type)) {
            int summerYear = academicStart + 1;
            termStart = LocalDate.of(summerYear, 7, 1);
            termEnd = LocalDate.of(summerYear, 8, 20);
            open = LocalDateTime.of(summerYear, 6, 1, 8, 0);
            close = open.plusDays(20);
            name = "Đợt Hè " + summerYear + " — Cơ sở " + c.spec.name;
            cohort = null;
        } else if ("FRESHMAN".equals(type)) {
            termStart = LocalDate.of(academicStart, 9, 1);
            termEnd = LocalDate.of(academicStart + 1, 6, 30);
            open = LocalDateTime.of(academicStart, 5, 1, 8, 0);
            close = open.plusDays(21);
            name = "Đợt tân sinh viên K" + (academicStart % 100) + " — Cơ sở " + c.spec.name;
            cohort = "K" + (academicStart % 100);
        } else {
            termStart = LocalDate.of(academicStart, 9, 1);
            termEnd = LocalDate.of(academicStart + 1, 6, 30);
            open = LocalDateTime.of(academicStart, 8, 1, 8, 0);
            close = open.plusDays(15);
            name = "Đợt ở tiếp " + academicStart + "-" + (academicStart + 1) + " — Cơ sở " + c.spec.name;
            cohort = null;
        }
        if ("DRAFT".equals(status)) {
            name = "Đợt Hè " + (academicStart + 1) + " (nháp) — Cơ sở " + c.spec.name;
        }
        int quota = 0;
        for (Bd bed : beds) {
            if (!bed.usable) {
                continue;
            }
            if (buildings.get(rooms.get(bed.room).building).campus == campus) {
                quota++;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO registration_periods (
                    id, name, period_type, academic_year, open_at, close_at, term_start, term_end, status, created_by,
                    gender_scope, min_conduct_score, target_cohort, target_quota, payment_deadline, checkin_start, checkin_end,
                    deposit_ratio, payment_guide, require_document_proof, terms_and_conditions, description, contact_phone, contact_email)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, id);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setString(4, academicStart + "-" + (academicStart + 1));
            ps.setObject(5, open);
            ps.setObject(6, close);
            ps.setObject(7, termStart);
            ps.setObject(8, termEnd);
            ps.setString(9, status);
            ps.setLong(10, quanLyId);
            ps.setString(11, genderScope);
            ps.setInt(12, minConduct);
            if (cohort == null) {
                ps.setNull(13, Types.VARCHAR);
            } else {
                ps.setString(13, cohort);
            }
            ps.setInt(14, quota);
            ps.setObject(15, close.plusDays(5));
            ps.setObject(16, termStart);
            ps.setObject(17, termStart.plusDays(6));
            ps.setBigDecimal(18, BigDecimal.valueOf(ratioMillis, 3));
            ps.setString(19, "Chuyển khoản nội dung mã sinh viên, hoặc nộp tiền mặt tại bàn lễ tân cơ sở.");
            ps.setBoolean(20, proof);
            ps.setString(21, "Nội quy ký túc xá năm học " + academicStart + "-" + (academicStart + 1));
            ps.setString(22, "Đợt đăng ký chỗ ở " + c.spec.name);
            ps.setString(23, String.format("0243800%04d", campus + 1));
            ps.setString(24, "ktx." + c.spec.code.toLowerCase() + "@ktx.edu.vn");
            ps.executeUpdate();
        }
        return id;
    }

    private void linkCampusBuildings(Connection conn, long periodId, int campus) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO registration_period_buildings (period_id, building_id) VALUES (?,?)")) {
            for (Bld bld : buildings) {
                if (bld.campus != campus) {
                    continue;
                }
                ps.setLong(1, periodId);
                ps.setLong(2, bld.id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertApplication(Connection conn, Assign a, long buildingId, String roomType, String status, int year) throws SQLException {
        Stu stu = students.get(a.stu);
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO room_applications (
                    id, period_id, student_id, preferred_building_id, preferred_room_type, priority_snapshot,
                    previous_stay_good_snapshot, status, submitted_at, computed_score, note)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, a.applicationId);
            ps.setLong(2, a.periodId);
            ps.setLong(3, stu.id);
            ps.setLong(4, buildingId);
            if (roomType == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, roomType);
            }
            ps.setString(6, stu.priority);
            ps.setBoolean(7, stu.prevGood);
            ps.setString(8, status);
            if ("DRAFT".equals(status)) {
                ps.setNull(9, Types.TIMESTAMP);
            } else {
                ps.setObject(9, LocalDateTime.of(year, 8, 10, 9, 0).plusMinutes(stu.index % 500));
            }
            ps.setInt(10, a.score);
            ps.setString(11, "WITHDRAWN".equals(status) ? "Sinh viên rút đơn" : null);
            ps.executeUpdate();
        }
    }

    private void insertRun(Connection conn, long id, long period, boolean dry, String status, int year, int assigned, int wait, String note) throws SQLException {
        LocalDateTime started = LocalDateTime.of(Math.min(year, 2026), 8, 15, 8, 0);
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO allocation_runs (
                    id, period_id, dry_run, status, started_at, finished_at, run_by, summary_json, seed_note, weights_json)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, id);
            ps.setLong(2, period);
            ps.setBoolean(3, dry);
            ps.setString(4, status);
            ps.setObject(5, started);
            if ("PENDING".equals(status) || "RUNNING".equals(status)) {
                ps.setNull(6, Types.TIMESTAMP);
            } else {
                ps.setObject(6, started.plusHours(2));
            }
            ps.setLong(7, quanLyId);
            ps.setString(8, "{\"assigned\":" + assigned + ",\"waitlisted\":" + wait + "}");
            ps.setString(9, note);
            ps.setString(10, WEIGHTS);
            ps.executeUpdate();
        }
    }

    private void insertRenewal(Connection conn, long contractId, LocalDate requestedEnd, String status, LocalDateTime created, LocalDateTime decided) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO renewal_requests (contract_id, requested_end, status, admin_note, created_at, decided_at)
                VALUES (?,?,?,?,?,?)
                """)) {
            ps.setLong(1, contractId);
            ps.setObject(2, requestedEnd);
            ps.setString(3, status);
            ps.setString(4, "APPROVED".equals(status) ? "Gia hạn đến hết năm học" : "Xử lý đơn gia hạn");
            ps.setObject(5, created);
            if (decided == null) {
                ps.setNull(6, Types.TIMESTAMP);
            } else {
                ps.setObject(6, decided);
            }
            ps.executeUpdate();
        }
    }

    private void insertChange(Connection conn, long contractId, Bd current, Bd target, String roomType, String kind, String status, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO room_change_requests (
                    contract_id, request_kind, current_bed_id, requested_building_id, requested_room_type,
                    reason, target_bed_id, status, admin_note)
                VALUES (?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, contractId);
            ps.setString(2, kind);
            ps.setLong(3, current.id);
            if (target == null) {
                ps.setNull(4, Types.BIGINT);
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(7, Types.BIGINT);
            } else {
                long buildingId = buildings.get(rooms.get(target.room).building).id;
                ps.setLong(4, buildingId);
                ps.setString(5, roomType);
                ps.setLong(7, target.id);
            }
            ps.setString(6, reason);
            ps.setString(8, status);
            ps.setString(9, "COMPLETED".equals(status) || "APPROVED".equals(status) ? "Đã xử lý" : null);
            ps.executeUpdate();
        }
    }

    private void insertCheck(Connection conn, long contractId, String type, LocalDateTime at, long by, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO check_in_outs (contract_id, event_type, performed_at, performed_by, asset_note, ok)
                VALUES (?,?,?,?,?,?)
                """)) {
            ps.setLong(1, contractId);
            ps.setString(2, type);
            ps.setObject(3, at);
            ps.setLong(4, by);
            ps.setString(5, note);
            ps.setBoolean(6, true);
            ps.executeUpdate();
        }
    }

    private void addViolation(Connection conn, Stu stu, LocalDateTime at, String severity, String action, int points) throws SQLException {
        String type = "TERMINATE".equals(action) ? "DISTURBANCE" : VIO_TYPE[stu.index % VIO_TYPE.length];
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO violations (student_id, recorded_by, violation_type, severity, points_deducted, description, occurred_at, action)
                VALUES (?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, stu.id);
            ps.setLong(2, quanLyId);
            ps.setString(3, type);
            ps.setString(4, severity);
            ps.setInt(5, points);
            ps.setString(6, "Biên bản " + type + " mức " + severity);
            ps.setObject(7, at);
            ps.setString(8, action);
            ps.executeUpdate();
        }
        stu.conduct = Math.max(0, stu.conduct - points);
        if ("TERMINATE".equals(action)) {
            stu.blocked = true;
        }
    }

    private void insertTicket(Connection conn, Stu stu, Rm room, int year, int n) throws SQLException {
        String status = TICKET_STATUS[n % TICKET_STATUS.length];
        LocalDateTime created = LocalDateTime.of(year, 9, 5, 8, 0).plusDays(n % 40L);
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO tickets (student_id, room_id, title, description, priority, status, created_at, updated_at, resolved_at)
                VALUES (?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, stu.id);
            ps.setLong(2, room.id);
            ps.setString(3, "Sự cố " + TICKET_PRIO[n % TICKET_PRIO.length] + " phòng " + room.number);
            ps.setString(4, "Mô tả sự cố cơ sở vật chất, phiếu mẫu lịch sử.");
            ps.setString(5, TICKET_PRIO[n % TICKET_PRIO.length]);
            ps.setString(6, status);
            ps.setObject(7, created);
            ps.setObject(8, created.plusDays(1));
            if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
                ps.setObject(9, created.plusDays(3));
            } else {
                ps.setNull(9, Types.TIMESTAMP);
            }
            ps.executeUpdate();
        }
    }

    private void insertNotification(Connection conn, Stu stu, int year, int n) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO notifications (user_id, title, body, type, read_flag, created_at, email_sent)
                VALUES (?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, stu.userId);
            ps.setString(2, "Thông báo " + NOTI[n % NOTI.length]);
            ps.setString(3, "Nội dung thông báo năm học " + year + "-" + (year + 1));
            ps.setString(4, NOTI[n % NOTI.length]);
            ps.setBoolean(5, n % 2 == 0);
            ps.setObject(6, LocalDateTime.of(year, 9, 6, 7, 0));
            ps.setBoolean(7, n % 3 == 0);
            ps.executeUpdate();
        }
    }

    private void insertAudit(Connection conn, long userId, String username, String role, String action, String targetType,
                             String targetId, String description, String status, LocalDateTime at) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO audit_logs (user_id, username, user_role, action, target_type, target_id, description, ip_address, status, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, userId);
            ps.setString(2, username);
            ps.setString(3, role);
            ps.setString(4, action);
            ps.setString(5, targetType);
            ps.setString(6, targetId);
            ps.setString(7, description);
            ps.setString(8, "10.0.0.8");
            ps.setString(9, status);
            ps.setObject(10, at);
            ps.executeUpdate();
        }
    }

    private void insertInvoiceRow(Connection conn, long id, String no, long studentId, long roomId, long contractId, String type,
                                  LocalDate month, long subtotal, long late, LocalDate due, String status, LocalDateTime paidAt, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO invoices (
                    id, invoice_no, student_id, room_id, contract_id, invoice_type, billing_month,
                    subtotal, late_fee, due_date, status, paid_at, idempotency_key)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setLong(1, id);
            ps.setString(2, no);
            ps.setLong(3, studentId);
            ps.setLong(4, roomId);
            ps.setLong(5, contractId);
            ps.setString(6, type);
            ps.setObject(7, month);
            ps.setLong(8, subtotal);
            ps.setLong(9, late);
            ps.setObject(10, due);
            ps.setString(11, status);
            if (paidAt == null) {
                ps.setNull(12, Types.TIMESTAMP);
            } else {
                ps.setObject(12, paidAt);
            }
            ps.setString(13, key);
            ps.executeUpdate();
        }
    }

    private void insertPayment(Connection conn, long invoiceId, long amount, String method, LocalDateTime at, String ref) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO payments (invoice_id, amount, method, paid_at, recorded_by, reference_no)
                VALUES (?,?,?,?,?,?)
                """)) {
            ps.setLong(1, invoiceId);
            ps.setLong(2, amount);
            ps.setString(3, method);
            ps.setObject(4, at);
            ps.setLong(5, quanLyId);
            if ("CASH".equals(method)) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, ref);
            }
            ps.executeUpdate();
        }
    }

    private void flushPeople(Connection conn) throws SQLException {
        try (Batch studentsBatch = batch(conn, """
                UPDATE students SET conduct_score=?, blocked_from_housing=?, previous_stay_good=? WHERE id=?
                """);
             Batch usersBatch = batch(conn, "UPDATE users SET enabled=? WHERE id=?")) {
            for (Stu stu : students) {
                PreparedStatement sp = studentsBatch.ps;
                sp.setInt(1, stu.conduct);
                sp.setBoolean(2, stu.blocked);
                sp.setBoolean(3, stu.prevGood);
                sp.setLong(4, stu.id);
                studentsBatch.add();
                PreparedStatement up = usersBatch.ps;
                up.setBoolean(1, stu.enabled);
                up.setLong(2, stu.userId);
                usersBatch.add();
            }
        }
    }

    private void flushBeds(Connection conn) throws SQLException {
        try (Batch batch = batch(conn, "UPDATE beds SET status=?, current_contract_id=? WHERE id=?")) {
            for (Bd bed : beds) {
                String status;
                Long contract = bed.currentContract == 0 ? null : bed.currentContract;
                String roomStatus = rooms.get(bed.room).status;
                if ("MAINTENANCE".equals(roomStatus)) {
                    status = "MAINTENANCE";
                    contract = null;
                } else if ("INACTIVE".equals(roomStatus)) {
                    status = "VACANT";
                    contract = null;
                } else if (contract != null) {
                    status = "OCCUPIED";
                } else {
                    status = "VACANT";
                }
                batch.ps.setString(1, status);
                if (contract == null) {
                    batch.ps.setNull(2, Types.BIGINT);
                } else {
                    batch.ps.setLong(2, contract);
                }
                batch.ps.setLong(3, bed.id);
                batch.add();
            }
        }
    }

    private void flushSequences(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO document_sequences (kind, `year`, `last_value`) VALUES (?,?,?) AS new_row
                ON DUPLICATE KEY UPDATE `last_value` = GREATEST(document_sequences.`last_value`, new_row.`last_value`)
                """)) {
            for (int i = 0; i < contractSeq.length; i++) {
                if (contractSeq[i] == 0 && invoiceSeq[i] == 0) {
                    continue;
                }
                int year = 2021 + i;
                ps.setString(1, "CONTRACT_NO");
                ps.setInt(2, year);
                ps.setInt(3, contractSeq[i]);
                ps.executeUpdate();
                ps.setString(1, "INVOICE_NO");
                ps.setInt(2, year);
                ps.setInt(3, invoiceSeq[i]);
                ps.executeUpdate();
            }
        }
    }

    private void writeMarker(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO system_configs (config_key, config_value, value_type, description) VALUES (?,?,?,?)
                """)) {
            ps.setString(1, MARKER);
            ps.setString(2, scaleName + ":" + students.size());
            ps.setString(3, "STRING");
            ps.setString(4, "Mốc đã nạp lịch sử. Xóa database nếu cần seed lại.");
            ps.executeUpdate();
        }
    }

    private void linkRole(Connection conn, long userId, long roleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?,?)")) {
            ps.setLong(1, userId);
            ps.setLong(2, roleId);
            ps.executeUpdate();
        }
    }

    private void linkBuilding(Connection conn, long userId, long buildingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO user_buildings (user_id, building_id) VALUES (?,?)")) {
            ps.setLong(1, userId);
            ps.setLong(2, buildingId);
            ps.executeUpdate();
        }
    }

    private long firstBuilding(int campus, boolean male) {
        for (Bld bld : buildings) {
            if (bld.campus == campus && bld.male == male) {
                return bld.id;
            }
        }
        for (Bld bld : buildings) {
            if (bld.male == male) {
                return bld.id;
            }
        }
        return buildings.get(0).id;
    }

    private int scoreOf(Stu stu) {
        int score = stu.conduct;
        if ("POLICY".equals(stu.priority)) {
            score += 1000;
        } else if ("REMOTE_AREA".equals(stu.priority)) {
            score += 500;
        }
        if (stu.prevGood) {
            score += 200;
        }
        return score;
    }

    private String contractNo(int year) {
        int idx = year - 2021;
        int n = ++contractSeq[idx];
        return String.format("HD-%d-%06d", year, n);
    }

    private String invoiceNo(int year) {
        int idx = Math.max(0, Math.min(invoiceSeq.length - 1, year - 2021));
        int n = ++invoiceSeq[idx];
        return String.format("INV-%d-%06d", 2021 + idx, n);
    }

    private void bindItem(PreparedStatement ps, long runId, Assign a, int rank, String result, String reason, boolean withBed) throws SQLException {
        ps.setLong(1, runId);
        ps.setLong(2, a.applicationId);
        if (withBed && a.bed >= 0) {
            ps.setLong(3, beds.get(a.bed).id);
        } else {
            ps.setNull(3, Types.BIGINT);
        }
        ps.setInt(4, rank);
        ps.setInt(5, a.score);
        ps.setString(6, result);
        ps.setString(7, reason);
    }

    private static Stay stay(long contractId, int stu, int room, LocalDate from, LocalDate to) {
        return new Stay(contractId, stu, room, from, to);
    }

    private void renamePeriod(Connection conn, long id, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE registration_periods SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void writeSimpleItems(Connection conn, long runId, List<Assign> apps, String result) throws SQLException {
        int rank = 1;
        try (Batch items = batch(conn, """
                INSERT INTO allocation_items (run_id, application_id, bed_id, rank_no, score, result, reason)
                VALUES (?,?,?,?,?,?,?)
                """)) {
            for (Assign a : apps) {
                items.ps.setLong(1, runId);
                items.ps.setLong(2, a.applicationId);
                items.ps.setNull(3, Types.BIGINT);
                items.ps.setInt(4, rank++);
                items.ps.setInt(5, a.score);
                items.ps.setString(6, result);
                items.ps.setString(7, "CHUA_CHOT");
                items.add();
            }
        }
    }

    private static void bindItemMoney(PreparedStatement ps, long invoiceId, String description, long qty, long unit, long amount, String code) throws SQLException {
        ps.setLong(1, invoiceId);
        ps.setString(2, description);
        ps.setBigDecimal(3, BigDecimal.valueOf(qty).setScale(3));
        ps.setLong(4, unit);
        ps.setLong(5, amount);
        ps.setString(6, code);
    }

    private static long[][] append(long[][] src, long[] row) {
        if (src == null) {
            return new long[][] {row};
        }
        long[][] next = new long[src.length + 1][];
        System.arraycopy(src, 0, next, 0, src.length);
        next[src.length] = row;
        return next;
    }

    private static void setIntOrNull(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static int count(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Batch batch(Connection conn, String sql) throws SQLException {
        return new Batch(conn.prepareStatement(sql));
    }

    private static final class Batch implements AutoCloseable {
        private final PreparedStatement ps;
        private int n;

        private Batch(PreparedStatement ps) {
            this.ps = ps;
        }

        private void add() throws SQLException {
            ps.addBatch();
            if (++n >= 400) {
                ps.executeBatch();
                n = 0;
            }
        }

        @Override
        public void close() throws SQLException {
            if (n > 0) {
                ps.executeBatch();
            }
            ps.close();
        }
    }

    private static final class Scale {
        private final String name;
        private final int yearFrom;
        private final int yearTo;
        private final CampusSpec[] campuses;

        private Scale(String name, int yearFrom, int yearTo, CampusSpec[] campuses) {
            this.name = name;
            this.yearFrom = yearFrom;
            this.yearTo = yearTo;
            this.campuses = campuses;
        }

        private static Scale full() {
            return new Scale("full", 2021, 2026, new CampusSpec[] {
                    new CampusSpec("MD", "Mỹ Đình", 8, 6, 20),
                    new CampusSpec("HD", "Hà Đông", 4, 5, 18),
                    new CampusSpec("BN", "Bắc Ninh", 3, 5, 16)
            });
        }

        private static Scale smoke() {
            return new Scale("smoke", 2024, 2026, new CampusSpec[] {
                    new CampusSpec("MD", "Mỹ Đình", 2, 2, 10)
            });
        }
    }

    private static final class CampusSpec {
        private final String code;
        private final String name;
        private final int buildings;
        private final int floors;
        private final int roomsPerFloor;

        private CampusSpec(String code, String name, int buildings, int floors, int roomsPerFloor) {
            this.code = code;
            this.name = name;
            this.buildings = buildings;
            this.floors = floors;
            this.roomsPerFloor = roomsPerFloor;
        }
    }

    private static final class Campus {
        private final CampusSpec spec;

        private Campus(CampusSpec spec) {
            this.spec = spec;
        }
    }

    private static final class Bld {
        private long id;
        private int campus;
        private String code;
        private String name;
        private boolean male;
        private long staffUserId;
    }

    private static final class Rm {
        private long id;
        private int building;
        private int floor;
        private String number;
        private int type;
        private int capacity;
        private String status;
        private boolean usable;
        private int elec;
        private int water;
        private boolean meterStarted;
    }

    private static final class Bd {
        private long id;
        private int room;
        private String code;
        private boolean male;
        private boolean usable;
        private int student = -1;
        private boolean stuck;
        private long currentContract;
    }

    private static final class Stu {
        private int index;
        private long id;
        private long userId;
        private int entryYear;
        private boolean male;
        private int conduct;
        private boolean blocked;
        private boolean active;
        private boolean enabled;
        private boolean prevGood;
        private boolean everHoused;
        private String priority;
        private String code;
        private String name;
        private int bed;
    }

    private static final class Assign {
        private int stu;
        private int bed;
        private int campus;
        private long periodId;
        private long applicationId;
        private long contractId;
        private int score;
        private boolean newcomer;
        private Outcome outcome;
        private String appStatus;
        private String reason;
    }

    private static final class Stay {
        private final long contractId;
        private final int stu;
        private final int room;
        private final LocalDate from;
        private final LocalDate to;

        private Stay(long contractId, int stu, int room, LocalDate from, LocalDate to) {
            this.contractId = contractId;
            this.stu = stu;
            this.room = room;
            this.from = from;
            this.to = to;
        }
    }
}
