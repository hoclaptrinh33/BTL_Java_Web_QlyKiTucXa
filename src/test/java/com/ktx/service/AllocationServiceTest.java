package com.ktx.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.AllocationItem;
import com.ktx.domain.AllocationRun;
import com.ktx.domain.Bed;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.enums.AllocationResult;
import com.ktx.domain.enums.AllocationRunStatus;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.dto.AllocationRunResult;
import com.ktx.repository.AllocationItemRepository;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.BedRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.impl.AllocationServiceImpl;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private AllocationEngine allocationEngine;

    @Mock
    private AllocationRunRepository allocationRunRepository;

    @Mock
    private AllocationItemRepository allocationItemRepository;

    @Mock
    private RegistrationPeriodRepository periodRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private ContractService contractService;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private RoomApplicationRepository roomApplicationRepository;

    private AllocationService allocationService;

    private RegistrationPeriod samplePeriod;
    private User adminUser;

    private SystemConfig config(String key, String value) {
        SystemConfig sc = new SystemConfig();
        sc.setConfigKey(key);
        sc.setConfigValue(value);
        sc.setValueType("STRING");
        return sc;
    }

    @BeforeEach
    void setUp() {
        allocationService = new AllocationServiceImpl(
                allocationEngine,
                allocationRunRepository,
                allocationItemRepository,
                periodRepository,
                userRepository,
                systemConfigRepository,
                contractService,
                bedRepository,
                roomApplicationRepository
        );

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");

        samplePeriod = new RegistrationPeriod();
        samplePeriod.setId(100L);
        samplePeriod.setName("Đợt Tân SV 2026-2027");
        samplePeriod.setPeriodType(PeriodType.FRESHMAN);
        samplePeriod.setStatus(PeriodStatus.CLOSED);
    }

    @Test
    @DisplayName("previewAndStore tạo AllocationRun dryRun=true, status=COMPLETED và ghi weightsJson audit")
    void previewAndStore_success() {
        // Arrange
        when(periodRepository.findById(100L)).thenReturn(Optional.of(samplePeriod));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        when(systemConfigRepository.findById("alloc.weight.policy"))
                .thenReturn(Optional.of(config("alloc.weight.policy", "1000")));
        when(systemConfigRepository.findById("alloc.weight.remote"))
                .thenReturn(Optional.of(config("alloc.weight.remote", "500")));
        when(systemConfigRepository.findById("alloc.weight.prev_good"))
                .thenReturn(Optional.of(config("alloc.weight.prev_good", "200")));
        when(systemConfigRepository.findById("alloc.preference.mode"))
                .thenReturn(Optional.of(config("alloc.preference.mode", "SOFT")));

        // Mock 3 items: 1 ASSIGNED, 1 WAITLISTED, 1 SKIPPED
        List<AllocationItem> items = new ArrayList<>();

        Student sv1 = new Student();
        sv1.setId(10L);
        sv1.setStudentCode("SV001");
        sv1.setGender(Gender.MALE);

        RoomApplication app1 = new RoomApplication();
        app1.setId(101L);
        app1.setStudent(sv1);
        app1.setStatus(ApplicationStatus.SUBMITTED);

        Bed bed1 = new Bed();
        bed1.setId(50L);
        bed1.setBedCode("G1");
        Room room1 = new Room();
        room1.setRoomNumber("101");
        bed1.setRoom(room1);

        AllocationItem item1 = new AllocationItem();
        item1.setStudent(sv1);
        item1.setApplication(app1);
        item1.setBed(bed1);
        item1.setRankNo(1);
        item1.setScore(1200);
        item1.setResult(AllocationResult.ASSIGNED);
        items.add(item1);

        AllocationItem item2 = new AllocationItem();
        item2.setStudent(sv1);
        item2.setApplication(app1);
        item2.setRankNo(2);
        item2.setScore(500);
        item2.setResult(AllocationResult.WAITLISTED);
        item2.setReason("NO_VACANT_BED");
        items.add(item2);

        AllocationItem item3 = new AllocationItem();
        item3.setStudent(sv1);
        item3.setApplication(app1);
        item3.setRankNo(3);
        item3.setScore(0);
        item3.setResult(AllocationResult.SKIPPED);
        item3.setReason("SKIPPED_BLOCKED");
        items.add(item3);

        when(allocationEngine.plan(100L)).thenReturn(new AllocationRunResult(items));
        when(allocationRunRepository.save(any(AllocationRun.class))).thenAnswer(invocation -> {
            AllocationRun r = invocation.getArgument(0);
            r.setId(999L);
            return r;
        });

        // Act
        AllocationRun run = allocationService.previewAndStore(100L, 1L);

        // Assert
        assertNotNull(run);
        assertEquals(999L, run.getId());
        assertTrue(run.getDryRun(), "Dry-run phải là true");
        assertEquals(AllocationRunStatus.COMPLETED, run.getStatus());
        assertEquals(samplePeriod, run.getPeriod());
        assertEquals(adminUser, run.getRunBy());

        // Kiểm tra audit weightsJson
        assertNotNull(run.getWeightsJson());
        assertTrue(run.getWeightsJson().contains("\"alloc.weight.policy\":1000"));
        assertTrue(run.getWeightsJson().contains("\"alloc.weight.remote\":500"));
        assertTrue(run.getWeightsJson().contains("\"alloc.weight.prev_good\":200"));
        assertTrue(run.getWeightsJson().contains("\"alloc.preference.mode\":\"SOFT\""));

        // Kiểm tra summaryJson
        assertNotNull(run.getSummaryJson());
        assertTrue(run.getSummaryJson().contains("\"total\":3"));
        assertTrue(run.getSummaryJson().contains("\"assigned\":1"));
        assertTrue(run.getSummaryJson().contains("\"waitlisted\":1"));
        assertTrue(run.getSummaryJson().contains("\"skipped\":1"));

        // Đảm bảo các item đều được gắn run và lưu lại
        for (AllocationItem item : items) {
            assertEquals(run, item.getRun());
        }
        verify(allocationItemRepository).saveAll(items);

        // Tiêu chí bất biến: ApplicationStatus không bị thay đổi
        assertEquals(ApplicationStatus.SUBMITTED, app1.getStatus(), "ApplicationStatus vẫn giữ nguyên SUBMITTED");
    }

    @Test
    @DisplayName("previewAndStore ném ngoại lệ khi không tìm thấy Period")
    void previewAndStore_periodNotFound() {
        when(periodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> allocationService.previewAndStore(999L, 1L));
        verify(allocationEngine, never()).plan(any(Long.class));
    }

    @Test
    @DisplayName("discardRun chuyển trạng thái run thành DISCARDED nếu chưa COMMITTED")
    void discardRun_success() {
        AllocationRun run = new AllocationRun();
        run.setId(10L);
        run.setStatus(AllocationRunStatus.COMPLETED);
        run.setDryRun(true);

        when(allocationRunRepository.findById(10L)).thenReturn(Optional.of(run));

        allocationService.discardRun(10L);

        assertEquals(AllocationRunStatus.DISCARDED, run.getStatus());
        verify(allocationRunRepository).save(run);
    }

    @Test
    @DisplayName("discardRun ném ngoại lệ nếu run đã COMMITTED")
    void discardRun_cannotDiscardCommitted() {
        AllocationRun run = new AllocationRun();
        run.setId(10L);
        run.setStatus(AllocationRunStatus.COMMITTED);

        when(allocationRunRepository.findById(10L)).thenReturn(Optional.of(run));

        assertThrows(BusinessException.class, () -> allocationService.discardRun(10L));
        verify(allocationRunRepository, never()).save(run);
    }

    @Test
    @DisplayName("commit() chốt phân bổ: khóa giường, tạo HĐ DRAFT, đổi ApplicationStatus và chuyển đợt sang COMPLETED")
    void commit_success() {
        samplePeriod.setStatus(PeriodStatus.CLOSED);
        samplePeriod.setTermStart(java.time.LocalDate.of(2026, 9, 1));
        samplePeriod.setTermEnd(java.time.LocalDate.of(2027, 1, 31));

        when(periodRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(samplePeriod));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        when(systemConfigRepository.findById("alloc.weight.policy"))
                .thenReturn(Optional.of(config("alloc.weight.policy", "1000")));
        when(systemConfigRepository.findById("alloc.weight.remote"))
                .thenReturn(Optional.of(config("alloc.weight.remote", "500")));
        when(systemConfigRepository.findById("alloc.weight.prev_good"))
                .thenReturn(Optional.of(config("alloc.weight.prev_good", "200")));
        when(systemConfigRepository.findById("alloc.preference.mode"))
                .thenReturn(Optional.of(config("alloc.preference.mode", "SOFT")));

        // 3 items: 1 ASSIGNED, 1 WAITLISTED, 1 SKIPPED
        Student sv1 = new Student();
        sv1.setId(10L);

        RoomApplication app1 = new RoomApplication();
        app1.setId(101L);
        app1.setStudent(sv1);
        app1.setStatus(ApplicationStatus.SUBMITTED);

        Bed bed1 = new Bed();
        bed1.setId(50L);
        bed1.setBedCode("G1");

        AllocationItem item1 = new AllocationItem();
        item1.setStudent(sv1);
        item1.setApplication(app1);
        item1.setBed(bed1);
        item1.setRankNo(1);
        item1.setScore(1000);
        item1.setResult(AllocationResult.ASSIGNED);

        RoomApplication app2 = new RoomApplication();
        app2.setId(102L);
        app2.setStudent(sv1);
        app2.setStatus(ApplicationStatus.SUBMITTED);

        AllocationItem item2 = new AllocationItem();
        item2.setStudent(sv1);
        item2.setApplication(app2);
        item2.setRankNo(2);
        item2.setScore(500);
        item2.setResult(AllocationResult.WAITLISTED);

        RoomApplication app3 = new RoomApplication();
        app3.setId(103L);
        app3.setStudent(sv1);
        app3.setStatus(ApplicationStatus.SUBMITTED);

        AllocationItem item3 = new AllocationItem();
        item3.setStudent(sv1);
        item3.setApplication(app3);
        item3.setRankNo(3);
        item3.setScore(0);
        item3.setResult(AllocationResult.SKIPPED);

        List<AllocationItem> items = new java.util.ArrayList<>(List.of(item1, item2, item3));
        when(allocationEngine.plan(100L)).thenReturn(new AllocationRunResult(items));
        when(allocationRunRepository.save(any(AllocationRun.class))).thenAnswer(inv -> {
            AllocationRun r = inv.getArgument(0);
            r.setId(888L);
            return r;
        });

        // Act
        AllocationRun committedRun = allocationService.commit(100L, 1L);

        // Assert
        assertNotNull(committedRun);
        assertEquals(888L, committedRun.getId());
        assertEquals(Boolean.FALSE, committedRun.getDryRun(), "Commit phải dryRun=false");
        assertEquals(AllocationRunStatus.COMMITTED, committedRun.getStatus());

        // Kiểm tra khóa giường tăng dần ID
        verify(bedRepository).findByIdInForUpdate(List.of(50L));

        // Kiểm tra tạo HĐ DRAFT
        verify(contractService).createDraftFromAllocation(
                app1, bed1, samplePeriod.getTermStart(), samplePeriod.getTermEnd()
        );

        // Kiểm tra cập nhật ApplicationStatus theo §6.3.4
        assertEquals(ApplicationStatus.ALLOCATED, app1.getStatus());
        assertEquals(ApplicationStatus.WAITLISTED, app2.getStatus());
        assertEquals(ApplicationStatus.REJECTED, app3.getStatus());
        verify(roomApplicationRepository).save(app1);
        verify(roomApplicationRepository).save(app2);
        verify(roomApplicationRepository).save(app3);

        // Đợt phải chuyển sang COMPLETED
        assertEquals(PeriodStatus.COMPLETED, samplePeriod.getStatus());
    }

    @Test
    @DisplayName("commit() ném ngoại lệ nếu đợt không ở trạng thái CLOSED hoặc ALLOCATING")
    void commit_invalidPeriodStatus() {
        samplePeriod.setStatus(PeriodStatus.OPEN);
        when(periodRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(samplePeriod));

        assertThrows(BusinessException.class, () -> allocationService.commit(100L, 1L));
        verify(allocationEngine, never()).plan(any(Long.class));
    }
}
