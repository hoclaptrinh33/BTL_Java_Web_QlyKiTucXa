package com.ktx.it;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.AllocationService;

/**
 * Integration test kiểm tra khóa bi quan (pessimistic write lock) trên MySQL InnoDB
 * khi 2 luồng gán cùng 1 giường đồng thời (§6.3.6 bước 9).
 * Test này opt-in qua biến môi trường KTX_IT_MYSQL=true và profile it-mysql.
 */
@SpringBootTest
@ActiveProfiles("it-mysql")
@EnabledIfEnvironmentVariable(named = "KTX_IT_MYSQL", matches = "true")
class AllocationLockIT {

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BedRepository bedRepository;

    @Test
    @DisplayName("Hai luồng gán tay đồng thời cùng 1 giường trên MySQL InnoDB -> đúng 1 thành công, 1 thất bại")
    void testConcurrentManualAllocationSameBed() throws InterruptedException {
        // 1. Chuẩn bị dữ liệu mẫu trong DB
        Building building = new Building();
        building.setCode("IT_BLD");
        building.setName("Tòa Lock IT");
        building.setGenderPolicy(BuildingGenderPolicy.MALE);
        building.setActive(true);
        building = buildingRepository.save(building);

        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNumber("IT-101");
        room.setFloor(1);
        room.setRoomType(RoomType.STANDARD_4);
        room.setCapacity(2);
        room.setPricePerTerm(BigDecimal.valueOf(2000000));
        room.setStatus(RoomStatus.ACTIVE);
        room = roomRepository.save(room);

        Bed bed = new Bed();
        bed.setRoom(room);
        bed.setBedCode("IT-101-B1");
        bed.setStatus(BedStatus.VACANT);
        bed = bedRepository.save(bed);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        User user1 = new User();
        user1.setUsername("it_user_1_" + System.currentTimeMillis());
        user1.setPasswordHash("hashedpassword");
        user1.setEmail("it1_" + System.currentTimeMillis() + "@ktx.test");
        user1.setRole(Role.STUDENT);
        user1.setEnabled(true);
        user1.setCreatedAt(now);
        user1.setUpdatedAt(now);
        user1 = userRepository.save(user1);

        Student student1 = new Student();
        student1.setUser(user1);
        student1.setStudentCode("IT_SV001_" + System.currentTimeMillis());
        student1.setFullName("Nguyen Van IT1");
        student1.setGender(Gender.MALE);
        student1.setBlockedFromHousing(false);
        student1 = studentRepository.save(student1);

        User user2 = new User();
        user2.setUsername("it_user_2_" + System.currentTimeMillis());
        user2.setPasswordHash("hashedpassword");
        user2.setEmail("it2_" + System.currentTimeMillis() + "@ktx.test");
        user2.setRole(Role.STUDENT);
        user2.setEnabled(true);
        user2.setCreatedAt(now);
        user2.setUpdatedAt(now);
        user2 = userRepository.save(user2);

        Student student2 = new Student();
        student2.setUser(user2);
        student2.setStudentCode("IT_SV002_" + System.currentTimeMillis());
        student2.setFullName("Nguyen Van IT2");
        student2.setGender(Gender.MALE);
        student2.setBlockedFromHousing(false);
        student2 = studentRepository.save(student2);

        final Long bedId = bed.getId();
        final Long s1Id = student1.getId();
        final Long s2Id = student2.getId();

        // 2. Chạy 2 luồng đồng thời gọi assignManual cùng bedId
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                allocationService.assignManual(s1Id, bedId, null, "IT thread 1");
                successCount.incrementAndGet();
            } catch (BusinessException | org.springframework.dao.DataAccessException ex) {
                failCount.incrementAndGet();
            } catch (Exception ex) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                allocationService.assignManual(s2Id, bedId, null, "IT thread 2");
                successCount.incrementAndGet();
            } catch (BusinessException | org.springframework.dao.DataAccessException ex) {
                failCount.incrementAndGet();
            } catch (Exception ex) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Đúng 1 luồng thành công, 1 luồng thất bại do xung đột khóa bi quan
        assertEquals(1, successCount.get(), "Chỉ có duy nhất 1 luồng gán thành công");
        assertEquals(1, failCount.get(), "Luồng còn lại phải thất bại do giường đã bị chiếm");
    }
}
