package com.ktx.it;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.AllocationService;

@SpringBootTest
@ActiveProfiles("it-mysql")
@EnabledIfEnvironmentVariable(named = "KTX_IT_MYSQL", matches = "true")
class AllocationLockIT {

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void verifyDatabaseIsMySqlNotH2() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase();
            assertFalse(dbProduct.contains("h2"),
                    "AllocationLockIT tuyệt đối không được chạy trên H2! Cần MySQL InnoDB để kiểm thử khóa.");
            assertTrue(dbProduct.contains("mysql"),
                    "AllocationLockIT yêu cầu kết nối tới cơ sở dữ liệu MySQL (hiện tại: " + dbProduct + ")");
        }
    }

    @Test
    @DisplayName("Hai thread cùng gán tay một giường trên MySQL: đúng một thread thành công, một thread báo lỗi rõ ràng")
    void twoThreadsAssignManualSameBedConcurrently_oneSucceedsOneFails() throws Exception {
        // Chuẩn bị dữ liệu: 1 tòa Nam, 1 phòng, 1 giường VACANT
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);

        Building building = new Building();
        building.setCode("IT" + suffix);
        building.setName("Tòa IT Lock " + suffix);
        building.setGenderPolicy(BuildingGenderPolicy.MALE);
        building.setActive(true);
        building = buildingRepository.save(building);

        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNumber("101");
        room.setFloor(1);
        room.setRoomType(RoomType.STANDARD_4);
        room.setCapacity(4);
        room.setPricePerTerm(new BigDecimal("1200000"));
        room.setStatus(RoomStatus.ACTIVE);
        room = roomRepository.save(room);

        Bed bed = new Bed();
        bed.setRoom(room);
        bed.setBedCode("B1");
        bed.setStatus(BedStatus.VACANT);
        bed = bedRepository.save(bed);

        // Chuẩn bị 2 sinh viên nam
        User user1 = createTestUser("sv_lock_1_" + suffix);
        Student student1 = createTestStudent(user1, "SVL1" + suffix, Gender.MALE);

        User user2 = createTestUser("sv_lock_2_" + suffix);
        Student student2 = createTestStudent(user2, "SVL2" + suffix, Gender.MALE);

        // Chuẩn bị đợt đăng ký
        RegistrationPeriod period = new RegistrationPeriod();
        period.setName("Đợt IT Lock " + suffix);
        period.setAcademicYear("2026-2027");
        period.setPeriodType(PeriodType.FRESHMAN);
        period.setStatus(PeriodStatus.CLOSED);
        period.setOpenAt(LocalDateTime.now().minusDays(10));
        period.setCloseAt(LocalDateTime.now().minusDays(1));
        period.setTermStart(LocalDate.now().plusDays(1));
        period.setTermEnd(LocalDate.now().plusMonths(5));
        period.setCreatedBy(user1);
        period = periodRepository.save(period);

        final Long periodId = period.getId();
        final Long bedId = bed.getId();
        final Long s1Id = student1.getId();
        final Long s2Id = student2.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Throwable> failureReason = new AtomicReference<>();
        AtomicReference<Contract> successfulContract = new AtomicReference<>();

        // Thread 1: assignManual cho student1
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                Contract c = allocationService.assignManual(s1Id, bedId, periodId, "IT Lock thread 1");
                successCount.incrementAndGet();
                successfulContract.set(c);
            } catch (Throwable t) {
                failureCount.incrementAndGet();
                failureReason.set(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: assignManual cho student2
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                Contract c = allocationService.assignManual(s2Id, bedId, periodId, "IT Lock thread 2");
                successCount.incrementAndGet();
                successfulContract.set(c);
            } catch (Throwable t) {
                failureCount.incrementAndGet();
                failureReason.set(t);
            } finally {
                doneLatch.countDown();
            }
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắt đầu chạy cả 2 thread đồng thời
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Cả hai thread phải hoàn thành trong thời gian quy định");
        assertEquals(1, successCount.get(), "Đúng 1 thread phải gán giường thành công");
        assertEquals(1, failureCount.get(), "Đúng 1 thread phải thất bại vì giường đã bị khóa/chiếm dụng");

        Throwable error = failureReason.get();
        assertNotNull(error, "Thread thất bại phải trả về lỗi rõ ràng");
        assertTrue(error instanceof BusinessException
                        || error.getCause() instanceof BusinessException
                        || (error.getMessage() != null && (error.getMessage().contains("không còn trống") || error.getMessage().contains("VACANT")))
                        || error instanceof org.springframework.dao.DataIntegrityViolationException,
                "Lỗi phải là BusinessException hoặc DataIntegrityViolationException rõ ràng: " + error.getMessage());

        // Kiểm tra trạng thái giường trong DB
        Bed finalBed = bedRepository.findById(bedId).orElseThrow();
        assertEquals(BedStatus.OCCUPIED, finalBed.getStatus(), "Giường phải ở trạng thái OCCUPIED");
        assertNotNull(finalBed.getCurrentContractId(), "Giường phải có currentContractId");
        assertEquals(successfulContract.get().getId(), finalBed.getCurrentContractId(), "currentContractId phải khớp với HĐ thành công");
    }

    private User createTestUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@ktx.edu.vn");
        u.setPasswordHash("$2a$10$dummyHashNotUsedDirectlyInThisTest");
        u.setRole(Role.STUDENT);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private Student createTestStudent(User user, String studentCode, Gender gender) {
        Student s = new Student();
        s.setUser(user);
        s.setStudentCode(studentCode);
        s.setFullName("Sinh Viên " + studentCode);
        s.setGender(gender);
        s.setPriorityCategory(PriorityCategory.NONE);
        s.setPreviousStayGood(false);
        s.setConductScore(80);
        s.setBlockedFromHousing(false);
        return studentRepository.save(s);
    }
}
