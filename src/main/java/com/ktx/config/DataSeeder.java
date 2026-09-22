package com.ktx.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.CheckInOut;
import com.ktx.domain.Contract;
import com.ktx.domain.DocumentSequence;
import com.ktx.domain.DocumentSequenceId;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.Notification;
import com.ktx.domain.Payment;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.Staff;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.AccountKind;
import com.ktx.repository.RoleRepository;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.AssetCategory;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.CheckInOutType;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.domain.enums.NotificationType;
import com.ktx.domain.enums.PaymentMethod;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.domain.enums.TicketPriority;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.CheckInOutRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.DocumentSequenceRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.MaintenanceTicketRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.PaymentRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.UtilityReadingRepository;
import com.ktx.repository.ViolationRepository;

/**
 * Seeder dữ liệu mẫu cho môi trường dev và buổi demo chấm 15 phút.
 * Đáp ứng đầy đủ Phụ lục A (tài khoản dev), Phụ lục B (checklist nghiệm thu)
 * và kịch bản phân bổ chỗ ở (gom lớp, hết phòng waitlist, hóa đơn mẫu).
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BuildingRepository buildingRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final RoomAssetRepository roomAssetRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final ContractRepository contractRepository;
    private final CheckInOutRepository checkInOutRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final ViolationRepository violationRepository;
    private final NotificationRepository notificationRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final PasswordEncoder passwordEncoder;

    private int contractSeqCounter = 1;
    private int invoiceSeqCounter = 1;

    public DataSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      BuildingRepository buildingRepository,
                      StaffRepository staffRepository,
                      StudentRepository studentRepository,
                      RoomRepository roomRepository,
                      BedRepository bedRepository,
                      RoomAssetRepository roomAssetRepository,
                      RegistrationPeriodRepository registrationPeriodRepository,
                      RoomApplicationRepository roomApplicationRepository,
                      ContractRepository contractRepository,
                      CheckInOutRepository checkInOutRepository,
                      UtilityReadingRepository utilityReadingRepository,
                      InvoiceRepository invoiceRepository,
                      InvoiceItemRepository invoiceItemRepository,
                      PaymentRepository paymentRepository,
                      MaintenanceTicketRepository maintenanceTicketRepository,
                      ViolationRepository violationRepository,
                      NotificationRepository notificationRepository,
                      DocumentSequenceRepository documentSequenceRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.buildingRepository = buildingRepository;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.roomAssetRepository = roomAssetRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.roomApplicationRepository = roomApplicationRepository;
        this.contractRepository = contractRepository;
        this.checkInOutRepository = checkInOutRepository;
        this.utilityReadingRepository = utilityReadingRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentRepository = paymentRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.violationRepository = violationRepository;
        this.notificationRepository = notificationRepository;
        this.documentSequenceRepository = documentSequenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // DB đã có admin: không ghi đè demo, nhưng vẫn bổ sung tài khoản quản lý
        // vì V4 gắn admin cũ vào SYSTEM_ADMIN (không còn quyền vận hành).
        if (userRepository.existsByUsername("admin")) {
            ensureQuanLyAccount();
            return;
        }

        com.ktx.domain.Role systemAdminRole = roleRepository.findByCode("SYSTEM_ADMIN").orElse(null);
        com.ktx.domain.Role quanLyRole = roleRepository.findByCode("QUAN_LY").orElse(null);
        com.ktx.domain.Role canBoRole = roleRepository.findByCode("CAN_BO").orElse(null);

        // =========================================================================
        // 1. TÀI KHOẢN HỆ THỐNG (ADMIN & QUANLY & STAFF) - Phụ lục A + RBAC
        // =========================================================================
        User adminUser = createUser("admin", "admin@example.com", "Admin@123", Role.ADMIN);
        adminUser.setAccountKind(AccountKind.INTERNAL);
        if (systemAdminRole != null) {
            adminUser.getRoles().add(systemAdminRole);
        }
        adminUser = userRepository.save(adminUser);

        User quanLyUser = createUser("quanly", "quanly@example.com", "Admin@123", Role.ADMIN);
        quanLyUser.setAccountKind(AccountKind.INTERNAL);
        if (quanLyRole != null) {
            quanLyUser.getRoles().add(quanLyRole);
        }
        quanLyUser = userRepository.save(quanLyUser);

        // Tòa A (Nam), Tòa B (Nữ), Tòa C (Nam)
        Building buildingA = createBuilding("A", "Tòa A", BuildingGenderPolicy.MALE);
        Building buildingB = createBuilding("B", "Tòa B", BuildingGenderPolicy.FEMALE);
        Building buildingC = createBuilding("C", "Tòa C", BuildingGenderPolicy.MALE);

        User staffUserA = createUser("staffA", "staffa@example.com", "Admin@123", Role.STAFF);
        staffUserA.setAccountKind(AccountKind.INTERNAL);
        if (canBoRole != null) {
            staffUserA.getRoles().add(canBoRole);
        }
        staffUserA.getAssignedBuildings().add(buildingA);
        staffUserA = userRepository.save(staffUserA);
        createStaff(staffUserA, "Cán bộ A", "0912345678", buildingA);

        User staffUserB = createUser("staffB", "staffb@example.com", "Admin@123", Role.STAFF);
        staffUserB.setAccountKind(AccountKind.INTERNAL);
        if (canBoRole != null) {
            staffUserB.getRoles().add(canBoRole);
        }
        staffUserB.getAssignedBuildings().add(buildingB);
        staffUserB = userRepository.save(staffUserB);
        createStaff(staffUserB, "Cán bộ B", "0987654321", buildingB);

        // =========================================================================
        // 2. SINH VIÊN MẪU (Phụ lục A + Mở rộng kịch bản phân bổ & kỷ luật)
        // =========================================================================
        // Phụ lục A:
        // D22CQCN001: Nam, CNTT, D22CQCN01, POLICY, prevGood: true -> score 1200
        Student s1 = createStudentUser("D22CQCN001", "d22cqcn001@example.com", "Admin@123",
                "Nguyễn Văn Nam", Gender.MALE, "CNTT", "D22CQCN01", PriorityCategory.POLICY, true, 100, false);

        // D22CQCN002: Nam, CNTT, D22CQCN01 (CÙNG LỚP D22CQCN01), NONE, prevGood: true -> score 200
        Student s2 = createStudentUser("D22CQCN002", "d22cqcn002@example.com", "Admin@123",
                "Trần Văn Hùng", Gender.MALE, "CNTT", "D22CQCN01", PriorityCategory.NONE, true, 100, false);

        // D22CQDT001: Nữ, DT, D22CQDT01, NONE, prevGood: true -> score 200
        Student sFemale1 = createStudentUser("D22CQDT001", "d22cqdt001@example.com", "Admin@123",
                "Phạm Thị Lan", Gender.FEMALE, "DT", "D22CQDT01", PriorityCategory.NONE, true, 100, false);

        // Mở rộng gom lớp và test xếp hạng:
        Student s3 = createStudentUser("D22CQCN003", "d22cqcn003@example.com", "Admin@123",
                "Lê Hoàng Long", Gender.MALE, "CNTT", "D22CQCN02", PriorityCategory.REMOTE_AREA, false, 100, false);

        Student s4 = createStudentUser("D22CQCN004", "d22cqcn004@example.com", "Admin@123",
                "Vũ Hải Đăng", Gender.MALE, "CNTT", "D22CQCN03", PriorityCategory.NONE, false, 100, false);

        Student sFemale2 = createStudentUser("D22CQDT002", "d22cqdt002@example.com", "Admin@123",
                "Hoàng Thùy Linh", Gender.FEMALE, "DT", "D22CQDT01", PriorityCategory.NONE, false, 100, false);

        // Sinh viên nữ thứ 3 để demo hết giường tòa nữ -> WAITLIST
        Student sFemale3 = createStudentUser("D22CQDT003", "d22cqdt003@example.com", "Admin@123",
                "Nguyễn Mai Phương", Gender.FEMALE, "DT", "D22CQDT02", PriorityCategory.NONE, false, 100, false);

        // Sinh viên cư trú Case A (Phòng 6 giường - 5 sinh viên):
        Student sA1 = createStudentUser("D21CQCN010", "d21cqcn010@example.com", "Admin@123",
                "Hoàng Minh Tuấn", Gender.MALE, "CNTT", "D21CQCN01", PriorityCategory.NONE, true, 100, false);
        Student sA2 = createStudentUser("D21CQCN011", "d21cqcn011@example.com", "Admin@123",
                "Nguyễn Đức Anh", Gender.MALE, "CNTT", "D21CQCN01", PriorityCategory.NONE, true, 100, false);
        Student sA3 = createStudentUser("D21CQCN012", "d21cqcn012@example.com", "Admin@123",
                "Vũ Thành Đạt", Gender.MALE, "CNTT", "D21CQCN01", PriorityCategory.NONE, true, 100, false);
        Student sA4 = createStudentUser("D21CQCN013", "d21cqcn013@example.com", "Admin@123",
                "Bùi Quốc Huy", Gender.MALE, "CNTT", "D21CQCN01", PriorityCategory.NONE, true, 100, false);
        Student sA5 = createStudentUser("D21CQCN014", "d21cqcn014@example.com", "Admin@123",
                "Đỗ Hoàng Long", Gender.MALE, "CNTT", "D21CQCN01", PriorityCategory.NONE, true, 100, false);

        // Sinh viên cư trú Case B (Phòng 4 giường - 3 sinh viên):
        Student sB1 = createStudentUser("D21CQCN020", "d21cqcn020@example.com", "Admin@123",
                "Phan Anh Quân", Gender.MALE, "CNTT", "D21CQCN02", PriorityCategory.NONE, true, 100, false);
        Student sB2 = createStudentUser("D21CQCN021", "d21cqcn021@example.com", "Admin@123",
                "Lý Tuấn Kiệt", Gender.MALE, "CNTT", "D21CQCN02", PriorityCategory.NONE, true, 100, false);
        Student sB3 = createStudentUser("D21CQCN022", "d21cqcn022@example.com", "Admin@123",
                "Dương Gia Bảo", Gender.MALE, "CNTT", "D21CQCN02", PriorityCategory.NONE, true, 100, false);

        // Sinh viên nữ cư trú sẵn tòa B:
        Student sFB1 = createStudentUser("D21CQDT001", "d21cqdt001@example.com", "Admin@123",
                "Ngô Quỳnh Trang", Gender.FEMALE, "DT", "D21CQDT01", PriorityCategory.NONE, true, 100, false);
        Student sFB2 = createStudentUser("D21CQDT002", "d21cqdt002@example.com", "Admin@123",
                "Trịnh Kiều Oanh", Gender.FEMALE, "DT", "D21CQDT01", PriorityCategory.NONE, true, 100, false);

        // Sinh viên demo trạng thái hợp đồng đặc biệt:
        Student sDraft = createStudentUser("D20CQCN001", "d20cqcn001@example.com", "Admin@123",
                "Trần Lưu Trú 1", Gender.MALE, "CNTT", "D20CQCN01", PriorityCategory.NONE, false, 100, false);
        Student sTerm = createStudentUser("D20CQCN002", "d20cqcn002@example.com", "Admin@123",
                "Lê Lưu Trú 2", Gender.MALE, "CNTT", "D20CQCN01", PriorityCategory.NONE, false, 100, false);
        Student sExp = createStudentUser("D20CQCN003", "d20cqcn003@example.com", "Admin@123",
                "Phạm Hết Hạn", Gender.MALE, "CNTT", "D20CQCN01", PriorityCategory.NONE, false, 100, false);

        // Sinh viên demo kỷ luật (0 điểm rèn luyện & bị cấm ở):
        Student sBlocked = createStudentUser("D22CQCN098", "d22cqcn098@example.com", "Admin@123",
                "Triệu Bị Cấm", Gender.MALE, "CNTT", "D22CQCN01", PriorityCategory.NONE, false, 100, true);
        Student sConduct0 = createStudentUser("D22CQCN099", "d22cqcn099@example.com", "Admin@123",
                "Nguyễn Văn Vi Phạm", Gender.MALE, "CNTT", "D22CQCN01", PriorityCategory.NONE, false, 0, false);

        // =========================================================================
        // 3. PHÒNG & GIƯỜNG & TÀI SẢN (Rooms, Beds, Room Assets)
        // =========================================================================
        // TÒA A (Nam):
        // Room A101: 4 giường (2.400.000đ/kỳ) - Trống hoàn toàn để đón phân bổ (D22CQCN001 & D22CQCN002 gom lớp)
        Room roomA101 = createRoom(buildingA, "101", 1, RoomType.STANDARD_4, 4, new BigDecimal("2400000"), RoomStatus.ACTIVE);
        createBed(roomA101, "101-1", BedStatus.VACANT);
        createBed(roomA101, "101-2", BedStatus.VACANT);
        createBed(roomA101, "101-3", BedStatus.VACANT);
        createBed(roomA101, "101-4", BedStatus.VACANT);
        createAsset(roomA101, "Bàn học sinh viên", AssetCategory.DESK, 4, AssetCondition.GOOD);
        createAsset(roomA101, "Ghế xoay lưới", AssetCategory.OTHER, 4, AssetCondition.GOOD);
        createAsset(roomA101, "Giường tầng sắt 1m2", AssetCategory.BED_FRAME, 2, AssetCondition.GOOD);
        createAsset(roomA101, "Quạt trần Vinawind", AssetCategory.FAN, 2, AssetCondition.GOOD);

        // Room A102: 6 giường (1.800.000đ/kỳ) - Case A điện nước (5 SV đang ở, 1 giường trống)
        Room roomA102 = createRoom(buildingA, "102", 1, RoomType.STANDARD_6, 6, new BigDecimal("1800000"), RoomStatus.ACTIVE);
        Bed bedA102_1 = createBed(roomA102, "102-1", BedStatus.OCCUPIED);
        Bed bedA102_2 = createBed(roomA102, "102-2", BedStatus.OCCUPIED);
        Bed bedA102_3 = createBed(roomA102, "102-3", BedStatus.OCCUPIED);
        Bed bedA102_4 = createBed(roomA102, "102-4", BedStatus.OCCUPIED);
        Bed bedA102_5 = createBed(roomA102, "102-5", BedStatus.OCCUPIED);
        createBed(roomA102, "102-6", BedStatus.VACANT);
        createAsset(roomA102, "Bàn học dài", AssetCategory.DESK, 2, AssetCondition.GOOD);
        createAsset(roomA102, "Ghế tựa inox", AssetCategory.OTHER, 6, AssetCondition.GOOD);
        createAsset(roomA102, "Giường tầng sắt 1m2", AssetCategory.BED_FRAME, 3, AssetCondition.GOOD);
        createAsset(roomA102, "Điều hòa Casper 18000BTU", AssetCategory.AC, 1, AssetCondition.GOOD);

        // Room A103: 4 giường (2.400.000đ/kỳ) - Case B điện nước (3 SV đang ở, 1 giường trống)
        Room roomA103 = createRoom(buildingA, "103", 1, RoomType.STANDARD_4, 4, new BigDecimal("2400000"), RoomStatus.ACTIVE);
        Bed bedA103_1 = createBed(roomA103, "103-1", BedStatus.OCCUPIED);
        Bed bedA103_2 = createBed(roomA103, "103-2", BedStatus.OCCUPIED);
        Bed bedA103_3 = createBed(roomA103, "103-3", BedStatus.OCCUPIED);
        createBed(roomA103, "103-4", BedStatus.VACANT);
        createAsset(roomA103, "Bàn học sinh viên", AssetCategory.DESK, 4, AssetCondition.GOOD);
        createAsset(roomA103, "Ghế xoay lưới", AssetCategory.OTHER, 4, AssetCondition.GOOD);
        createAsset(roomA103, "Giường tầng sắt", AssetCategory.BED_FRAME, 2, AssetCondition.GOOD);

        // Room A104: 2 giường (4.000.000đ/kỳ) - VIP_AC (1 DRAFT, 1 TERMINATED chiếm giữ)
        Room roomA104 = createRoom(buildingA, "104", 1, RoomType.VIP_AC, 2, new BigDecimal("4000000"), RoomStatus.ACTIVE);
        Bed bedA104_1 = createBed(roomA104, "104-1", BedStatus.OCCUPIED);
        Bed bedA104_2 = createBed(roomA104, "104-2", BedStatus.OCCUPIED);
        createAsset(roomA104, "Bàn làm việc gỗ sồi cao cấp", AssetCategory.DESK, 2, AssetCondition.GOOD);
        createAsset(roomA104, "Ghế công thái học Sihoo", AssetCategory.OTHER, 2, AssetCondition.GOOD);
        createAsset(roomA104, "Giường đơn gỗ sồi 1m4", AssetCategory.BED_FRAME, 2, AssetCondition.GOOD);
        createAsset(roomA104, "Điều hòa Daikin Inverter 12000BTU", AssetCategory.AC, 1, AssetCondition.GOOD);
        createAsset(roomA104, "Tủ lạnh mini Funiki 90L", AssetCategory.OTHER, 1, AssetCondition.GOOD);

        // Room A105: 8 giường (1.200.000đ/kỳ) - STANDARD_8 (1 EXPIRED chiếm giữ, còn lại trống)
        Room roomA105 = createRoom(buildingA, "105", 1, RoomType.STANDARD_8, 8, new BigDecimal("1200000"), RoomStatus.ACTIVE);
        Bed bedA105_1 = createBed(roomA105, "105-1", BedStatus.OCCUPIED);
        for (int i = 2; i <= 8; i++) {
            createBed(roomA105, "105-" + i, BedStatus.VACANT);
        }

        // Room A201: Phòng đang bảo trì (MAINTENANCE)
        Room roomA201 = createRoom(buildingA, "201", 2, RoomType.STANDARD_4, 4, new BigDecimal("2400000"), RoomStatus.MAINTENANCE);
        for (int i = 1; i <= 4; i++) {
            createBed(roomA201, "201-" + i, BedStatus.MAINTENANCE);
        }

        // TÒA B (Nữ):
        // Room B101: 4 giường (2.400.000đ/kỳ) - 2 SV đang ở, ĐÚNG 2 GIƯỜNG TRỐNG (để demo 3 nữ nộp đơn -> 2 trúng, 1 waitlist)
        Room roomB101 = createRoom(buildingB, "101", 1, RoomType.STANDARD_4, 4, new BigDecimal("2400000"), RoomStatus.ACTIVE);
        createBed(roomB101, "101-1", BedStatus.VACANT);
        createBed(roomB101, "101-2", BedStatus.VACANT);
        Bed bedB101_3 = createBed(roomB101, "101-3", BedStatus.OCCUPIED);
        Bed bedB101_4 = createBed(roomB101, "101-4", BedStatus.OCCUPIED);
        createAsset(roomB101, "Bàn học sinh viên", AssetCategory.DESK, 4, AssetCondition.GOOD);
        createAsset(roomB101, "Gương đứng soi toàn thân", AssetCategory.OTHER, 1, AssetCondition.GOOD);

        // Room B102: 6 giường (1.800.000đ/kỳ) - Bảo trì (để giới hạn số giường trống nữ)
        Room roomB102 = createRoom(buildingB, "102", 1, RoomType.STANDARD_6, 6, new BigDecimal("1800000"), RoomStatus.MAINTENANCE);
        for (int i = 1; i <= 6; i++) {
            createBed(roomB102, "102-" + i, BedStatus.MAINTENANCE);
        }

        // TÒA C (Nam):
        Room roomC101 = createRoom(buildingC, "101", 1, RoomType.STANDARD_4, 4, new BigDecimal("2400000"), RoomStatus.ACTIVE);
        for (int i = 1; i <= 4; i++) {
            createBed(roomC101, "101-" + i, BedStatus.VACANT);
        }

        // =========================================================================
        // 4. HỢP ĐỒNG LƯU TRÚ (CONTRACTS) & CHECK-IN
        // =========================================================================
        LocalDate termStart = LocalDate.of(2026, 9, 1);
        LocalDate termEnd = LocalDate.of(2027, 1, 31);

        // Case A (5 hợp đồng ACTIVE tại A102):
        Contract cA1 = createContract(sA1, bedA102_1, termStart, termEnd, new BigDecimal("1800000"), new BigDecimal("900000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cA2 = createContract(sA2, bedA102_2, termStart, termEnd, new BigDecimal("1800000"), new BigDecimal("900000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cA3 = createContract(sA3, bedA102_3, termStart, termEnd, new BigDecimal("1800000"), new BigDecimal("900000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cA4 = createContract(sA4, bedA102_4, termStart, termEnd, new BigDecimal("1800000"), new BigDecimal("900000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cA5 = createContract(sA5, bedA102_5, termStart, termEnd, new BigDecimal("1800000"), new BigDecimal("900000"), DepositStatus.HELD, ContractStatus.ACTIVE);

        for (Contract c : List.of(cA1, cA2, cA3, cA4, cA5)) {
            createCheckInOut(c, CheckInOutType.CHECK_IN, LocalDateTime.of(2026, 9, 1, 8, 30), staffUserA, "Bàn giao phòng và chìa khóa đầy đủ", true);
        }

        // Case B (3 hợp đồng ACTIVE tại A103):
        Contract cB1 = createContract(sB1, bedA103_1, termStart, termEnd, new BigDecimal("2400000"), new BigDecimal("1200000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cB2 = createContract(sB2, bedA103_2, termStart, termEnd, new BigDecimal("2400000"), new BigDecimal("1200000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cB3 = createContract(sB3, bedA103_3, termStart, termEnd, new BigDecimal("2400000"), new BigDecimal("1200000"), DepositStatus.HELD, ContractStatus.ACTIVE);

        for (Contract c : List.of(cB1, cB2, cB3)) {
            createCheckInOut(c, CheckInOutType.CHECK_IN, LocalDateTime.of(2026, 9, 1, 9, 0), staffUserA, "Bàn giao phòng và thiết bị", true);
        }

        // Tòa B (2 hợp đồng ACTIVE tại B101):
        Contract cFB1 = createContract(sFB1, bedB101_3, termStart, termEnd, new BigDecimal("2400000"), new BigDecimal("1200000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        Contract cFB2 = createContract(sFB2, bedB101_4, termStart, termEnd, new BigDecimal("2400000"), new BigDecimal("1200000"), DepositStatus.HELD, ContractStatus.ACTIVE);
        createCheckInOut(cFB1, CheckInOutType.CHECK_IN, LocalDateTime.of(2026, 9, 1, 9, 30), staffUserB, "Bàn giao chìa khóa tòa B", true);
        createCheckInOut(cFB2, CheckInOutType.CHECK_IN, LocalDateTime.of(2026, 9, 1, 9, 45), staffUserB, "Bàn giao chìa khóa tòa B", true);

        // Hợp đồng DRAFT (Bed A104-1) - Demo hủy DRAFT nhả giường:
        createContract(sDraft, bedA104_1, termStart, termEnd, new BigDecimal("4000000"), new BigDecimal("2000000"), DepositStatus.HELD, ContractStatus.DRAFT);

        // Hợp đồng TERMINATED (Bed A104-2) - Demo đã terminate nhưng chưa checkout vẫn chiếm giường:
        Contract cTerm = createContract(sTerm, bedA104_2, termStart, termEnd, new BigDecimal("4000000"), new BigDecimal("2000000"), DepositStatus.FORFEITED, ContractStatus.TERMINATED);
        createCheckInOut(cTerm, CheckInOutType.CHECK_IN, LocalDateTime.of(2026, 9, 1, 10, 0), staffUserA, "Nhận phòng đầu kỳ", true);

        // Hợp đồng EXPIRED (Bed A105-1) - Demo hết hạn kỳ trước vẫn chiếm giường đến checkout:
        LocalDate prevTermStart = LocalDate.of(2026, 4, 1);
        LocalDate prevTermEnd = LocalDate.of(2026, 8, 31);
        Contract cExp = createContract(sExp, bedA105_1, prevTermStart, prevTermEnd, new BigDecimal("1200000"), new BigDecimal("600000"), DepositStatus.HELD, ContractStatus.EXPIRED);
        createCheckInOut(cExp, CheckInOutType.CHECK_IN, LocalDateTime.of(2026, 4, 1, 8, 0), staffUserA, "Check-in kỳ hè", true);

        // =========================================================================
        // 5. GHI CHỈ SỐ (UTILITY READINGS) & HÓA ĐƠN (INVOICES) - Phụ lục B
        // =========================================================================
        LocalDate billingMonth = LocalDate.of(2026, 9, 1);
        LocalDate invoiceDueDate = LocalDate.of(2026, 10, 10);

        // Case A: Phòng A102 (5 người) - 280 kWh điện, 18 m3 nước. Chia đều:
        // Divisible = Điện (679.540) + Nước (270.000) + Net (50.000) = 999.540 đ
        // Share = floor(999.540 / 5) = 199.908 đ. Residual = 0 đ.
        // Phụ phí người: Vệ sinh 20.000 đ + Gửi xe 30.000 đ = 50.000 đ.
        // Mỗi sinh viên subtotal = 199.908 + 50.000 = 249.908 đ. (Khớp từng đồng)
        createUtilityReading(roomA102, billingMonth, 1000, 1280, 50, 68, staffUserA);

        Student[] caseAStudents = {sA1, sA2, sA3, sA4, sA5};
        Contract[] caseAContracts = {cA1, cA2, cA3, cA4, cA5};
        for (int i = 0; i < caseAStudents.length; i++) {
            Student stu = caseAStudents[i];
            Contract con = caseAContracts[i];
            Invoice inv = createInvoice(stu, roomA102, con, InvoiceType.UTILITY, billingMonth,
                    new BigDecimal("249908"), BigDecimal.ZERO, new BigDecimal("249908"),
                    invoiceDueDate, i == 0 ? InvoiceStatus.PAID : InvoiceStatus.UNPAID,
                    "UTILITY:A102:2026-09:" + stu.getId());

            createInvoiceItem(inv, "Tiền điện sinh hoạt (280 kWh / 5 người, 6 bậc EVN)", new BigDecimal("56.000"), BigDecimal.ZERO, new BigDecimal("135908"), "ELEC");
            createInvoiceItem(inv, "Tiền nước sinh hoạt (18 m³ / 5 người × 15.000đ/m³)", new BigDecimal("3.600"), new BigDecimal("15000"), new BigDecimal("54000"), "WATER");
            createInvoiceItem(inv, "Phí internet phòng (50.000đ / 5 người)", new BigDecimal("1.000"), new BigDecimal("10000"), new BigDecimal("10000"), "INTERNET");
            createInvoiceItem(inv, "Phí vệ sinh môi trường", new BigDecimal("1.000"), new BigDecimal("20000"), new BigDecimal("20000"), "SANITATION");
            createInvoiceItem(inv, "Phí gửi xe sinh viên", new BigDecimal("1.000"), new BigDecimal("30000"), new BigDecimal("30000"), "PARKING");

            if (i == 0) {
                inv.setPaidAt(LocalDateTime.of(2026, 10, 5, 14, 0));
                invoiceRepository.save(inv);
                createPayment(inv, new BigDecimal("249908"), PaymentMethod.CASH, LocalDateTime.of(2026, 10, 5, 14, 0), staffUserA, "PT-2026-09-001");
            }
        }

        // Case B: Phòng A103 (3 người) - 51 kWh điện, 1 m3 nước, internet 50.000 đ.
        // Divisible = Điện (101.250) + Nước (15.000) + Net (50.000) = 166.250 đ
        // Share = floor(166.250 / 3) = 55.416 đ. Residual = 2 đ dồn cho min student_id (sB1).
        // SV sB1: 55.418 + 50.000 = 105.418 đ. Phạt chậm trả 5% = ceil(105.418 * 0.05) = 5.271 đ -> Tổng 110.689 đ (OVERDUE).
        // SV sB2, sB3: 55.416 + 50.000 = 105.416 đ (UNPAID).
        createUtilityReading(roomA103, billingMonth, 100, 151, 20, 21, staffUserA);

        // sB1: min student id
        Invoice invB1 = createInvoice(sB1, roomA103, cB1, InvoiceType.UTILITY, billingMonth,
                new BigDecimal("105418"), new BigDecimal("5271"), new BigDecimal("110689"),
                LocalDate.now().minusDays(5), InvoiceStatus.OVERDUE,
                "UTILITY:A103:2026-09:" + sB1.getId());
        createInvoiceItem(invB1, "Tiền điện sinh hoạt (51 kWh / 3 người + phần dồn)", new BigDecimal("17.000"), BigDecimal.ZERO, new BigDecimal("33750"), "ELEC");
        createInvoiceItem(invB1, "Tiền nước sinh hoạt (1 m³ / 3 người)", new BigDecimal("0.333"), new BigDecimal("15000"), new BigDecimal("5000"), "WATER");
        createInvoiceItem(invB1, "Phí internet phòng (50.000đ / 3 người + phần dồn)", new BigDecimal("1.000"), new BigDecimal("16668"), new BigDecimal("16668"), "INTERNET");
        createInvoiceItem(invB1, "Phí vệ sinh môi trường", new BigDecimal("1.000"), new BigDecimal("20000"), new BigDecimal("20000"), "SANITATION");
        createInvoiceItem(invB1, "Phí gửi xe sinh viên", new BigDecimal("1.000"), new BigDecimal("30000"), new BigDecimal("30000"), "PARKING");
        createInvoiceItem(invB1, "Phí phạt chậm thanh toán (5% subtotal)", new BigDecimal("1.000"), new BigDecimal("5271"), new BigDecimal("5271"), "LATE_FEE");

        // sB2:
        Invoice invB2 = createInvoice(sB2, roomA103, cB2, InvoiceType.UTILITY, billingMonth,
                new BigDecimal("105416"), BigDecimal.ZERO, new BigDecimal("105416"),
                LocalDate.now().plusDays(5), InvoiceStatus.UNPAID,
                "UTILITY:A103:2026-09:" + sB2.getId());
        createInvoiceItem(invB2, "Tiền điện sinh hoạt (51 kWh / 3 người)", new BigDecimal("17.000"), BigDecimal.ZERO, new BigDecimal("33750"), "ELEC");
        createInvoiceItem(invB2, "Tiền nước sinh hoạt (1 m³ / 3 người)", new BigDecimal("0.333"), new BigDecimal("15000"), new BigDecimal("5000"), "WATER");
        createInvoiceItem(invB2, "Phí internet phòng (50.000đ / 3 người)", new BigDecimal("1.000"), new BigDecimal("16666"), new BigDecimal("16666"), "INTERNET");
        createInvoiceItem(invB2, "Phí vệ sinh môi trường", new BigDecimal("1.000"), new BigDecimal("20000"), new BigDecimal("20000"), "SANITATION");
        createInvoiceItem(invB2, "Phí gửi xe sinh viên", new BigDecimal("1.000"), new BigDecimal("30000"), new BigDecimal("30000"), "PARKING");

        // sB3:
        Invoice invB3 = createInvoice(sB3, roomA103, cB3, InvoiceType.UTILITY, billingMonth,
                new BigDecimal("105416"), BigDecimal.ZERO, new BigDecimal("105416"),
                LocalDate.now().plusDays(5), InvoiceStatus.UNPAID,
                "UTILITY:A103:2026-09:" + sB3.getId());
        createInvoiceItem(invB3, "Tiền điện sinh hoạt (51 kWh / 3 người)", new BigDecimal("17.000"), BigDecimal.ZERO, new BigDecimal("33750"), "ELEC");
        createInvoiceItem(invB3, "Tiền nước sinh hoạt (1 m³ / 3 người)", new BigDecimal("0.333"), new BigDecimal("15000"), new BigDecimal("5000"), "WATER");
        createInvoiceItem(invB3, "Phí internet phòng (50.000đ / 3 người)", new BigDecimal("1.000"), new BigDecimal("16666"), new BigDecimal("16666"), "INTERNET");
        createInvoiceItem(invB3, "Phí vệ sinh môi trường", new BigDecimal("1.000"), new BigDecimal("20000"), new BigDecimal("20000"), "SANITATION");
        createInvoiceItem(invB3, "Phí gửi xe sinh viên", new BigDecimal("1.000"), new BigDecimal("30000"), new BigDecimal("30000"), "PARKING");

        // =========================================================================
        // 6. ĐỢT ĐĂNG KÝ (PERIODS) & ĐƠN NỘP (APPLICATIONS)
        // =========================================================================
        // Đợt 1: Trạng thái CLOSED để Admin lập tức chạy Preview / Commit demo
        RegistrationPeriod period1 = new RegistrationPeriod();
        period1.setName("Đợt 1 — Đăng ký KTX Học kỳ 1 (2026-2027)");
        period1.setPeriodType(PeriodType.NEW_ACADEMIC_YEAR);
        period1.setAcademicYear("2026-2027");
        period1.setOpenAt(LocalDateTime.now().minusDays(10));
        period1.setCloseAt(LocalDateTime.now().minusDays(1));
        period1.setTermStart(termStart);
        period1.setTermEnd(termEnd);
        period1.setStatus(PeriodStatus.CLOSED);
        period1.setCreatedBy(adminUser);
        period1 = registrationPeriodRepository.save(period1);

        // Đơn nộp đợt 1 (Xếp hạng & Gom lớp & Waitlist):
        // 1. D22CQCN001: POLICY (1000) + prevGood (200) = 1200 điểm -> Rank 1 -> Vào A101 (giường 1)
        createRoomApplication(period1, s1, buildingA, RoomType.STANDARD_4, PriorityCategory.POLICY, true,
                LocalDateTime.now().minusDays(5), 1200, "Đơn diện chính sách ưu tiên");

        // 2. D22CQCN003: REMOTE_AREA (500) = 500 điểm -> Rank 2 -> Vào A101
        createRoomApplication(period1, s3, buildingA, RoomType.STANDARD_4, PriorityCategory.REMOTE_AREA, false,
                LocalDateTime.now().minusDays(4), 500, "Đơn vùng sâu vùng xa");

        // 3. D22CQCN002: NONE (0) + prevGood (200) = 200 điểm -> Rank 3 -> CÙNG LỚP D22CQCN01 VỚI D22CQCN001 -> GOM VÀO A101!
        createRoomApplication(period1, s2, buildingA, RoomType.STANDARD_4, PriorityCategory.NONE, true,
                LocalDateTime.now().minusDays(4).plusHours(2), 200, "Đơn sinh viên ngoan kỳ trước");

        // 4. D22CQDT001: Nữ, NONE + prevGood (200) = 200 điểm -> Tòa B -> Vào B101
        createRoomApplication(period1, sFemale1, buildingB, RoomType.STANDARD_4, PriorityCategory.NONE, true,
                LocalDateTime.now().minusDays(3), 200, "Đơn nữ sinh viên tòa B");

        // 5. D22CQDT002: Nữ, NONE = 0 điểm (nộp sớm) -> Tòa B -> Vào B101
        createRoomApplication(period1, sFemale2, buildingB, RoomType.STANDARD_4, PriorityCategory.NONE, false,
                LocalDateTime.now().minusDays(2), 0, "Đơn sinh viên nữ");

        // 6. D22CQDT003: Nữ, NONE = 0 điểm (nộp muộn) -> Tòa B hết chỗ -> KẾT QUẢ SẼ LÀ WAITLISTED (NO_VACANT_BED)!
        createRoomApplication(period1, sFemale3, buildingB, RoomType.STANDARD_4, PriorityCategory.NONE, false,
                LocalDateTime.now().minusDays(1), 0, "Đơn sinh viên nữ nộp sau");

        // 7. D22CQCN098: Bị cấm ở KTX -> KẾT QUẢ SẼ LÀ SKIPPED (SKIPPED_BLOCKED)!
        createRoomApplication(period1, sBlocked, buildingA, RoomType.STANDARD_4, PriorityCategory.NONE, false,
                LocalDateTime.now().minusDays(1).minusHours(1), 0, "Đơn sinh viên thuộc diện kỷ luật cấm ở");

        // Đợt 2: Trạng thái OPEN để sinh viên nộp đơn trực tiếp trên web
        RegistrationPeriod period2 = new RegistrationPeriod();
        period2.setName("Đợt Đăng ký Tân SV K22 (Đang nhận đơn)");
        period2.setPeriodType(PeriodType.FRESHMAN);
        period2.setAcademicYear("2026-2027");
        period2.setOpenAt(LocalDateTime.now().minusDays(2));
        period2.setCloseAt(LocalDateTime.now().plusDays(10));
        period2.setTermStart(termStart);
        period2.setTermEnd(termEnd);
        period2.setStatus(PeriodStatus.OPEN);
        period2.setCreatedBy(adminUser);
        registrationPeriodRepository.save(period2);

        // =========================================================================
        // 7. YÊU CẦU SỬA CHỮA (TICKETS), VI PHẠM (VIOLATIONS), THÔNG BÁO (NOTIFICATIONS)
        // =========================================================================
        createTicket(sA1, roomA102, "Hỏng bóng đèn LED phòng tắm",
                "Bóng đèn nhấp nháy liên tục rồi tắt hẳn tối qua", TicketPriority.HIGH, TicketStatus.OPEN,
                LocalDateTime.now().minusDays(2));
        createTicket(sB1, roomA103, "Vòi sen tắm bị rò rỉ nước",
                "Vòi sen rò rỉ nước ở khớp nối dây kim loại", TicketPriority.MEDIUM, TicketStatus.IN_PROGRESS,
                LocalDateTime.now().minusDays(3));

        // Vi phạm trừ điểm rèn luyện của D22CQCN099:
        createViolation(sConduct0, adminUser, ViolationType.DAMAGE, ViolationSeverity.SEVERE, 100,
                "Cố ý phá hoại tài sản công cộng tại sảnh KTX. Trừ 100 điểm rèn luyện.",
                LocalDateTime.now().minusDays(5), ViolationAction.POINT_DEDUCT);

        // Thông báo hệ thống:
        createNotification(adminUser, "Cập nhật chỉ số điện nước tháng 09/2026",
                "Hệ thống đã tự động ghi nhận chỉ số điện nước cho các phòng tòa A.", NotificationType.GENERIC);
        createNotification(s1.getUser(), "Mở đợt đăng ký phòng ở KTX kỳ 1",
                "Đợt đăng ký phòng ở đã mở. Vui lòng kiểm tra thông tin và nộp đơn sớm.", NotificationType.ALLOCATION);
        createNotification(sB1.getUser(), "Cảnh báo hóa đơn tiền điện nước quá hạn",
                "Hóa đơn tháng 09/2026 của bạn đã quá hạn thanh toán. Vui lòng nộp tiền tại quầy.", NotificationType.INVOICE);

        // Đồng bộ sequence năm hiện tại
        updateDocumentSequence("CONTRACT_NO", 2026, contractSeqCounter);
        updateDocumentSequence("INVOICE_NO", 2026, invoiceSeqCounter);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void ensureQuanLyAccount() {
        if (userRepository.existsByUsername("quanly")) {
            return;
        }
        Optional<com.ktx.domain.Role> found = roleRepository.findByCode("QUAN_LY");
        if (found == null || found.isEmpty()) {
            return;
        }
        User quanLyUser = createUser("quanly", "quanly@example.com", "Admin@123", Role.ADMIN);
        quanLyUser.setAccountKind(AccountKind.INTERNAL);
        quanLyUser.getRoles().add(found.get());
        userRepository.save(quanLyUser);
    }

    private User createUser(String username, String email, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountKind(role == Role.STUDENT ? AccountKind.STUDENT : AccountKind.INTERNAL);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private void createStaff(User user, String fullName, String phone, Building building) {
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setFullName(fullName);
        staff.setPhone(phone);
        staff.setAssignedBuilding(building);
        staffRepository.save(staff);
    }

    private Student createStudentUser(String studentCode, String email, String password,
                                      String fullName, Gender gender, String facultyCode, String classCode,
                                      PriorityCategory priority, boolean prevStayGood, int conductScore, boolean blocked) {
        User user = createUser(studentCode, email, password, Role.STUDENT);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(studentCode);
        student.setFullName(fullName);
        student.setGender(gender);
        student.setFacultyCode(facultyCode);
        student.setClassCode(classCode);
        student.setPriorityCategory(priority);
        student.setPreviousStayGood(prevStayGood);
        student.setConductScore(conductScore);
        student.setBlockedFromHousing(blocked);
        return studentRepository.save(student);
    }

    private Building createBuilding(String code, String name, BuildingGenderPolicy genderPolicy) {
        Building b = new Building();
        b.setCode(code);
        b.setName(name);
        b.setGenderPolicy(genderPolicy);
        b.setActive(true);
        return buildingRepository.save(b);
    }

    private Room createRoom(Building building, String roomNumber, int floor, RoomType type, int capacity, BigDecimal price, RoomStatus status) {
        Room r = new Room();
        r.setBuilding(building);
        r.setRoomNumber(roomNumber);
        r.setFloor(floor);
        r.setRoomType(type);
        r.setCapacity(capacity);
        r.setPricePerTerm(price);
        r.setStatus(status);
        return roomRepository.save(r);
    }

    private Bed createBed(Room room, String bedCode, BedStatus status) {
        Bed b = new Bed();
        b.setRoom(room);
        b.setBedCode(bedCode);
        b.setStatus(status);
        b.setVersion(0L);
        return bedRepository.save(b);
    }

    private void createAsset(Room room, String name, AssetCategory category, int quantity, AssetCondition condition) {
        RoomAsset asset = new RoomAsset();
        asset.setRoom(room);
        asset.setName(name);
        asset.setCategory(category);
        asset.setQuantity(quantity);
        asset.setCondition(condition);
        roomAssetRepository.save(asset);
    }

    private Contract createContract(Student student, Bed bed, LocalDate start, LocalDate end,
                                    BigDecimal fee, BigDecimal deposit, DepositStatus depStatus, ContractStatus status) {
        String contractNo = String.format("HD-%d-%06d", start.getYear(), contractSeqCounter++);
        Contract c = new Contract();
        c.setContractNo(contractNo);
        c.setStudent(student);
        c.setBed(bed);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setRoomFee(fee);
        c.setDepositAmount(deposit);
        c.setDepositStatus(depStatus);
        c.setStatus(status);
        c.setTermsVersion("v1.6");
        c.setSignedAt(LocalDateTime.of(start.getYear(), start.getMonthValue(), start.getDayOfMonth(), 8, 0));
        c = contractRepository.save(c);

        // Gắn cache current_contract_id lên giường
        bed.setCurrentContractId(c.getId());
        bedRepository.save(bed);
        return c;
    }

    private void createCheckInOut(Contract contract, CheckInOutType eventType, LocalDateTime performedAt, User staff, String note, boolean ok) {
        CheckInOut cio = new CheckInOut();
        cio.setContract(contract);
        cio.setEventType(eventType);
        cio.setPerformedAt(performedAt);
        cio.setPerformedBy(staff);
        cio.setAssetNote(note);
        cio.setOk(ok);
        checkInOutRepository.save(cio);
    }

    private void createUtilityReading(Room room, LocalDate billingMonth, int elecPrev, int elecCurr, int waterPrev, int waterCurr, User recordedBy) {
        UtilityReading ur = new UtilityReading();
        ur.setRoom(room);
        ur.setBillingMonth(billingMonth);
        ur.setElecPrev(elecPrev);
        ur.setElecCurr(elecCurr);
        ur.setWaterPrev(waterPrev);
        ur.setWaterCurr(waterCurr);
        ur.setElecReplaced(false);
        ur.setWaterReplaced(false);
        ur.setNewBuildingMeter(false);
        ur.setRecordedBy(recordedBy);
        ur.setRecordedAt(LocalDateTime.now().minusDays(10));
        utilityReadingRepository.save(ur);
    }

    private Invoice createInvoice(Student student, Room room, Contract contract, InvoiceType type, LocalDate month,
                                  BigDecimal subtotal, BigDecimal lateFee, BigDecimal total, LocalDate due, InvoiceStatus status, String idemKey) {
        String invoiceNo = String.format("INV-%d-%06d", month.getYear(), invoiceSeqCounter++);
        Invoice inv = new Invoice();
        inv.setInvoiceNo(invoiceNo);
        inv.setStudent(student);
        inv.setRoom(room);
        inv.setContract(contract);
        inv.setInvoiceType(type);
        inv.setBillingMonth(month);
        inv.setSubtotal(subtotal);
        inv.setLateFee(lateFee);
        inv.setTotal(total);
        inv.setDueDate(due);
        inv.setStatus(status);
        inv.setIdempotencyKey(idemKey);
        return invoiceRepository.save(inv);
    }

    private void createInvoiceItem(Invoice invoice, String description, BigDecimal qty, BigDecimal unitPrice, BigDecimal amount, String code) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(description);
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setAmount(amount);
        item.setItemCode(code);
        invoiceItemRepository.save(item);
    }

    private void createPayment(Invoice invoice, BigDecimal amount, PaymentMethod method, LocalDateTime paidAt, User recordedBy, String refNo) {
        Payment p = new Payment();
        p.setInvoice(invoice);
        p.setAmount(amount);
        p.setMethod(method);
        p.setPaidAt(paidAt);
        p.setRecordedBy(recordedBy);
        p.setReferenceNo(refNo);
        paymentRepository.save(p);
    }

    private void createRoomApplication(RegistrationPeriod period, Student student, Building building, RoomType roomType,
                                       PriorityCategory priority, boolean prevGood, LocalDateTime submittedAt, int score, String note) {
        RoomApplication app = new RoomApplication();
        app.setPeriod(period);
        app.setStudent(student);
        app.setPreferredBuilding(building);
        app.setPreferredRoomType(roomType);
        app.setPrioritySnapshot(priority);
        app.setPreviousStayGoodSnapshot(prevGood);
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(submittedAt);
        app.setComputedScore(score);
        app.setNote(note);
        roomApplicationRepository.save(app);
    }

    private void createTicket(Student student, Room room, String title, String desc, TicketPriority priority, TicketStatus status, LocalDateTime at) {
        MaintenanceTicket t = new MaintenanceTicket();
        t.setStudent(student);
        t.setRoom(room);
        t.setTitle(title);
        t.setDescription(desc);
        t.setPriority(priority);
        t.setStatus(status);
        t.setCreatedAt(at);
        t.setUpdatedAt(at);
        maintenanceTicketRepository.save(t);
    }

    private void createViolation(Student student, User recorder, ViolationType type, ViolationSeverity severity, int points, String desc, LocalDateTime at, ViolationAction action) {
        Violation v = new Violation();
        v.setStudent(student);
        v.setRecordedBy(recorder);
        v.setViolationType(type);
        v.setSeverity(severity);
        v.setPointsDeducted(points);
        v.setDescription(desc);
        v.setOccurredAt(at);
        v.setAction(action);
        violationRepository.save(v);
    }

    private void createNotification(User user, String title, String body, NotificationType type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setBody(body);
        n.setType(type);
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now().minusHours(4));
        n.setEmailSent(false);
        notificationRepository.save(n);
    }

    private void updateDocumentSequence(String kind, int year, int lastValue) {
        documentSequenceRepository.findById(new DocumentSequenceId(kind, year)).ifPresentOrElse(seq -> {
            int current = seq.getLastValue() != null ? seq.getLastValue() : 0;
            seq.setLastValue(Math.max(current, lastValue));
            documentSequenceRepository.save(seq);
        }, () -> {
            DocumentSequence seq = new DocumentSequence();
            seq.setKind(kind);
            seq.setYear(year);
            seq.setLastValue(lastValue);
            documentSequenceRepository.save(seq);
        });
    }
}
