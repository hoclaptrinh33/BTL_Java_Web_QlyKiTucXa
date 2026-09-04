package com.ktx.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.domain.*;
import com.ktx.domain.enums.*;
import com.ktx.dto.AllocationRunResult;
import com.ktx.repository.*;
import com.ktx.service.impl.AllocationEngineImpl;

@ExtendWith(MockitoExtension.class)
class AllocationEngineTest {

    private AllocationEngine engine;

    @Mock private RoomApplicationRepository roomApplicationRepository;
    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private BedRepository bedRepository;
    @Mock private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        engine = new AllocationEngineImpl(
                roomApplicationRepository,
                systemConfigRepository,
                bedRepository,
                contractRepository
        );
    }

    private void mockSystemConfigs(String mode) {
        SystemConfig policy = new SystemConfig();
        policy.setConfigKey("alloc.weight.policy");
        policy.setConfigValue("1000");

        SystemConfig remote = new SystemConfig();
        remote.setConfigKey("alloc.weight.remote");
        remote.setConfigValue("500");

        SystemConfig prevGood = new SystemConfig();
        prevGood.setConfigKey("alloc.weight.prev_good");
        prevGood.setConfigValue("200");

        SystemConfig prefMode = new SystemConfig();
        prefMode.setConfigKey("alloc.preference.mode");
        prefMode.setConfigValue(mode);

        when(systemConfigRepository.findById("alloc.weight.policy")).thenReturn(Optional.of(policy));
        when(systemConfigRepository.findById("alloc.weight.remote")).thenReturn(Optional.of(remote));
        when(systemConfigRepository.findById("alloc.weight.prev_good")).thenReturn(Optional.of(prevGood));
        when(systemConfigRepository.findById("alloc.preference.mode")).thenReturn(Optional.of(prefMode));
    }

    // ==========================================
    // 1. RANK & TIE-BREAK (3 CẤP ĐỘ)
    // ==========================================
    @Nested
    @DisplayName("1. Rank and Tie-break tests")
    class RankAndTieBreakTests {

        @Test
        @DisplayName("Rank theo điểm số DESC và tie-break cấp 2 theo submitted_at ASC")
        void testRankingAndTieBreak_ScoreAndSubmittedAt() {
            mockSystemConfigs("SOFT");

            Student sA = createStudent(1L, Gender.MALE, null, null);
            Student sB = createStudent(2L, Gender.MALE, null, null);
            Student sC = createStudent(3L, Gender.MALE, null, null);
            Student sD = createStudent(4L, Gender.MALE, null, null);

            LocalDateTime now = LocalDateTime.now();

            // A: POLICY + prev good -> score 1200
            RoomApplication aA = createApp(sA, null, null, PriorityCategory.POLICY, true, now.plusMinutes(10));
            // B: REMOTE + prev good -> score 700
            RoomApplication aB = createApp(sB, null, null, PriorityCategory.REMOTE_AREA, true, now.plusMinutes(5));
            // C: NONE + prev good -> score 200, nộp sau D
            RoomApplication aC = createApp(sC, null, null, PriorityCategory.NONE, true, now.plusMinutes(20));
            // D: NONE + prev good -> score 200, nộp trước C
            RoomApplication aD = createApp(sD, null, null, PriorityCategory.NONE, true, now.plusMinutes(10));

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(aA, aB, aC, aD));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(Collections.emptyList());
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            assertEquals(4, items.size());
            // Rank 1: A (Score 1200)
            assertEquals(sA.getId(), items.get(0).getStudent().getId());
            assertEquals(1200, items.get(0).getScore());
            assertEquals(1, items.get(0).getRankNo());

            // Rank 2: B (Score 700)
            assertEquals(sB.getId(), items.get(1).getStudent().getId());
            assertEquals(700, items.get(1).getScore());
            assertEquals(2, items.get(1).getRankNo());

            // Rank 3: D (Score 200, nộp trước C)
            assertEquals(sD.getId(), items.get(2).getStudent().getId());
            assertEquals(200, items.get(2).getScore());
            assertEquals(3, items.get(2).getRankNo());

            // Rank 4: C (Score 200, nộp sau D)
            assertEquals(sC.getId(), items.get(3).getStudent().getId());
            assertEquals(200, items.get(3).getScore());
            assertEquals(4, items.get(3).getRankNo());
        }

        @Test
        @DisplayName("Tie-break cấp 3 theo student.id ASC khi cùng điểm và cùng submitted_at")
        void testRankingTieBreak_StudentId_WhenScoreAndSubmittedAtEqual() {
            mockSystemConfigs("SOFT");

            LocalDateTime fixedTime = LocalDateTime.of(2026, 9, 1, 8, 0, 0);

            // Cả 3 sinh viên đều có cùng điểm (score = 500) và cùng submittedAt
            Student sSmallId = createStudent(5L, Gender.MALE, null, null);
            Student sMediumId = createStudent(20L, Gender.MALE, null, null);
            Student sLargeId = createStudent(100L, Gender.MALE, null, null);

            RoomApplication appLarge = createApp(sLargeId, null, null, PriorityCategory.REMOTE_AREA, false, fixedTime);
            RoomApplication appSmall = createApp(sSmallId, null, null, PriorityCategory.REMOTE_AREA, false, fixedTime);
            RoomApplication appMedium = createApp(sMediumId, null, null, PriorityCategory.REMOTE_AREA, false, fixedTime);

            // Đưa vào repository theo thứ tự lộn xộn: Large, Small, Medium
            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(appLarge, appSmall, appMedium));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(Collections.emptyList());
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            assertEquals(3, items.size());
            // Rank 1: student.id = 5 (nhỏ nhất)
            assertEquals(5L, items.get(0).getStudent().getId());
            assertEquals(1, items.get(0).getRankNo());

            // Rank 2: student.id = 20
            assertEquals(20L, items.get(1).getStudent().getId());
            assertEquals(2, items.get(1).getRankNo());

            // Rank 3: student.id = 100 (lớn nhất)
            assertEquals(100L, items.get(2).getStudent().getId());
            assertEquals(3, items.get(2).getRankNo());
        }
    }

    // ==========================================
    // 2. SOFT KHÔNG NỚI GENDER
    // ==========================================
    @Nested
    @DisplayName("2. SOFT mode never relaxes gender_policy tests")
    class SoftPreferenceGenderConstraintTests {

        @Test
        @DisplayName("SOFT không nới gender: SV Nữ nộp đơn nhưng hệ thống chỉ còn giường tòa Nam -> WAITLISTED")
        void testSoftPreference_NeverRelaxesGender_FemaleWhenOnlyMaleBeds() {
            mockSystemConfigs("SOFT");

            Building maleBuilding = createBuilding(1L, BuildingGenderPolicy.MALE);
            Room room = createRoom(1L, maleBuilding, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Bed maleBed = createBed(1L, room, "G1", BedStatus.VACANT);

            Student femaleStudent = createStudent(10L, Gender.FEMALE, null, null);

            RoomApplication app = createApp(femaleStudent, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(maleBed));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Tuyệt đối không xếp sinh viên nữ vào giường tòa nam
            assertEquals(AllocationResult.WAITLISTED, item.getResult());
            assertEquals("NO_VACANT_BED", item.getReason());
            assertNull(item.getBed());
        }

        @Test
        @DisplayName("SOFT không nới gender: SV Nam nộp đơn nhưng hệ thống chỉ còn giường tòa Nữ -> WAITLISTED")
        void testSoftPreference_NeverRelaxesGender_MaleWhenOnlyFemaleBeds() {
            mockSystemConfigs("SOFT");

            Building femaleBuilding = createBuilding(2L, BuildingGenderPolicy.FEMALE);
            Room room = createRoom(2L, femaleBuilding, "201", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Bed femaleBed = createBed(2L, room, "G1", BedStatus.VACANT);

            Student maleStudent = createStudent(11L, Gender.MALE, null, null);

            RoomApplication app = createApp(maleStudent, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(femaleBed));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Tuyệt đối không xếp sinh viên nam vào giường tòa nữ
            assertEquals(AllocationResult.WAITLISTED, item.getResult());
            assertEquals("NO_VACANT_BED", item.getReason());
            assertNull(item.getBed());
        }

        @Test
        @DisplayName("SOFT nới lỏng tòa nhưng chỉ nới trong cùng giới tính: bỏ qua tòa khác giới tính")
        void testSoftPreference_RelaxesToSameGenderOnly() {
            mockSystemConfigs("SOFT");

            Building preferredMaleBuilding = createBuilding(1L, BuildingGenderPolicy.MALE);
            Building femaleBuilding = createBuilding(2L, BuildingGenderPolicy.FEMALE);
            Building alternativeMaleBuilding = createBuilding(3L, BuildingGenderPolicy.MALE);

            Room r1 = createRoom(1L, preferredMaleBuilding, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rFemale = createRoom(2L, femaleBuilding, "201", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room r3 = createRoom(3L, alternativeMaleBuilding, "301", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bed1 = createBed(1L, r1, "G1", BedStatus.VACANT);
            Bed bedFemale = createBed(2L, rFemale, "G1", BedStatus.VACANT);
            Bed bed3 = createBed(3L, r3, "G1", BedStatus.VACANT);

            // Giường ở tòa nam mong muốn đã bị chiếm bởi SV khác
            Student other = createStudent(99L, Gender.MALE, null, null);
            Contract c1 = new Contract();
            c1.setBed(bed1);
            c1.setStudent(other);

            Student maleStudent = createStudent(1L, Gender.MALE, null, null);
            RoomApplication app = createApp(maleStudent, preferredMaleBuilding, RoomType.STANDARD_4, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed1, bedFemale, bed3));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c1));

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Sinh viên được xếp vào tòa nam thay thế r3, KHÔNG BAO GIỜ chạm vào bedFemale
            assertEquals(AllocationResult.ASSIGNED, item.getResult());
            assertEquals(bed3.getId(), item.getBed().getId());
            assertEquals("NO_BUILDING_MATCH", item.getReason());
        }

        @Test
        @DisplayName("SOFT nới lỏng loại phòng trong cùng tòa: hết STANDARD_2 nới sang STANDARD_4")
        void testSoftPreference_DropRoomType_KeepBuilding() {
            mockSystemConfigs("SOFT");

            Building building = createBuilding(1L, BuildingGenderPolicy.MALE);
            Room rStd6 = createRoom(1L, building, "101", RoomType.STANDARD_6, RoomStatus.ACTIVE);
            Room rStd4 = createRoom(2L, building, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedStd6 = createBed(1L, rStd6, "G1", BedStatus.VACANT);
            Bed bedStd4 = createBed(2L, rStd4, "G1", BedStatus.VACANT);

            Student other = createStudent(99L, Gender.MALE, null, null);
            Contract c = new Contract();
            c.setBed(bedStd6);
            c.setStudent(other);

            Student maleStudent = createStudent(1L, Gender.MALE, null, null);
            RoomApplication app = createApp(maleStudent, building, RoomType.STANDARD_6, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedStd6, bedStd4));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c));

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            assertEquals(AllocationResult.ASSIGNED, item.getResult());
            assertEquals(bedStd4.getId(), item.getBed().getId());
            assertEquals("NO_TYPE_MATCH", item.getReason());
        }
    }

    // ==========================================
    // 3. HẾT GIƯỜNG -> WAITLISTED
    // ==========================================
    @Nested
    @DisplayName("3. Bed exhaustion and Waitlist tests")
    class BedExhaustionAndWaitlistTests {

        @Test
        @DisplayName("Hết giường: 3 SV nộp đơn nhưng chỉ có 2 giường trống -> SV hạng 3 bị WAITLISTED")
        void testExhaustedBeds_HigherRankAssigned_LowerRankWaitlisted() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);
            Room r = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Bed bed1 = createBed(1L, r, "G1", BedStatus.VACANT);
            Bed bed2 = createBed(2L, r, "G2", BedStatus.VACANT);

            LocalDateTime now = LocalDateTime.now();

            Student s1 = createStudent(1L, Gender.MALE, null, null);
            Student s2 = createStudent(2L, Gender.MALE, null, null);
            Student s3 = createStudent(3L, Gender.MALE, null, null);

            // s1: POLICY (1000)
            RoomApplication app1 = createApp(s1, null, null, PriorityCategory.POLICY, false, now);
            // s2: REMOTE (500)
            RoomApplication app2 = createApp(s2, null, null, PriorityCategory.REMOTE_AREA, false, now);
            // s3: NONE (0)
            RoomApplication app3 = createApp(s3, null, null, PriorityCategory.NONE, false, now);

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app1, app2, app3));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed1, bed2));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            assertEquals(3, items.size());

            // Rank 1: s1 được gán giường
            assertEquals(AllocationResult.ASSIGNED, items.get(0).getResult());
            assertEquals(s1.getId(), items.get(0).getStudent().getId());
            assertNotNull(items.get(0).getBed());

            // Rank 2: s2 được gán giường còn lại
            assertEquals(AllocationResult.ASSIGNED, items.get(1).getResult());
            assertEquals(s2.getId(), items.get(1).getStudent().getId());
            assertNotNull(items.get(1).getBed());

            // Rank 3: s3 hết giường -> WAITLISTED với lý do NO_VACANT_BED
            assertEquals(AllocationResult.WAITLISTED, items.get(2).getResult());
            assertEquals(s3.getId(), items.get(2).getStudent().getId());
            assertEquals("NO_VACANT_BED", items.get(2).getReason());
            assertNull(items.get(2).getBed());
        }

        @Test
        @DisplayName("Hệ thống hoàn toàn không có giường trống -> Tất cả chuyển WAITLISTED")
        void testZeroBedsAvailable_AllWaitlisted() {
            mockSystemConfigs("SOFT");

            Student s1 = createStudent(1L, Gender.MALE, null, null);
            Student s2 = createStudent(2L, Gender.FEMALE, null, null);

            RoomApplication app1 = createApp(s1, null, null, PriorityCategory.POLICY, false, LocalDateTime.now());
            RoomApplication app2 = createApp(s2, null, null, PriorityCategory.REMOTE_AREA, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app1, app2));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(Collections.emptyList());
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            assertEquals(2, items.size());
            for (AllocationItem item : items) {
                assertEquals(AllocationResult.WAITLISTED, item.getResult());
                assertEquals("NO_VACANT_BED", item.getReason());
                assertNull(item.getBed());
            }
        }

        @Test
        @DisplayName("Giường MAINTENANCE hoặc phòng INACTIVE không được tính là trống")
        void testBedUnderMaintenance_OrRoomInactive_NotConsideredVacant() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            // Phòng INACTIVE
            Room inactiveRoom = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.INACTIVE);
            Bed bedInInactiveRoom = createBed(1L, inactiveRoom, "G1", BedStatus.VACANT);

            // Giường MAINTENANCE trong phòng ACTIVE
            Room activeRoom = createRoom(2L, b, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Bed bedMaintenance = createBed(2L, activeRoom, "G1", BedStatus.MAINTENANCE);

            // Giường hợp lệ
            Bed validBed = createBed(3L, activeRoom, "G2", BedStatus.VACANT);

            Student s1 = createStudent(1L, Gender.MALE, null, null);
            Student s2 = createStudent(2L, Gender.MALE, null, null);

            RoomApplication app1 = createApp(s1, null, null, PriorityCategory.POLICY, false, LocalDateTime.now());
            RoomApplication app2 = createApp(s2, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED))
                    .thenReturn(List.of(app1, app2));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedInInactiveRoom, bedMaintenance, validBed));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            // s1 được gán vào validBed duy nhất
            assertEquals(AllocationResult.ASSIGNED, items.get(0).getResult());
            assertEquals(validBed.getId(), items.get(0).getBed().getId());

            // s2 không thể vào bedInInactiveRoom hay bedMaintenance -> WAITLISTED
            assertEquals(AllocationResult.WAITLISTED, items.get(1).getResult());
            assertEquals("NO_VACANT_BED", items.get(1).getReason());
            assertNull(items.get(1).getBed());
        }

        @Test
        @DisplayName("Chế độ STRICT: Hết giường đúng tòa/loại nguyện vọng -> WAITLISTED ngay (không nới)")
        void testStrictPreferenceWaitlist() {
            mockSystemConfigs("STRICT");

            Building bA = createBuilding(1L, BuildingGenderPolicy.MALE);
            Building bB = createBuilding(2L, BuildingGenderPolicy.MALE);

            Room rA = createRoom(1L, bA, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rB = createRoom(2L, bB, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedA = createBed(1L, rA, "G1", BedStatus.VACANT);
            Bed bedB = createBed(2L, rB, "G1", BedStatus.VACANT);

            Student s = createStudent(1L, Gender.MALE, null, null);
            Student other = createStudent(99L, Gender.MALE, null, null);

            // bedA bị chiếm bởi other
            Contract activeContractOnA = new Contract();
            activeContractOnA.setBed(bedA);
            activeContractOnA.setStudent(other);

            RoomApplication app = createApp(s, bA, RoomType.STANDARD_4, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedA, bedB));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(activeContractOnA));

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Chế độ STRICT: không được nới sang bB -> WAITLISTED
            assertEquals(AllocationResult.WAITLISTED, item.getResult());
            assertEquals("NO_BUILDING_MATCH", item.getReason());
        }
    }

    // ==========================================
    // 4. COMPARATOR ĐỦ & NULL-SAFE LỚP/KHOA
    // ==========================================
    @Nested
    @DisplayName("4. Comparator hierarchy and null-safety tests")
    class ComparatorAndNullSafetyTests {

        @Test
        @DisplayName("Comparator roomBonus: Cùng lớp (100+pack) > Cùng khoa (40+pack) > Gom phòng (pack) > Phòng trống")
        void testComparator_RoomBonusHierarchy() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            Room rClass = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rFac = createRoom(2L, b, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rPack = createRoom(3L, b, "103", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rEmpty = createRoom(4L, b, "104", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedClassOcc = createBed(1L, rClass, "G1", BedStatus.VACANT);
            Bed bedClassFree = createBed(2L, rClass, "G2", BedStatus.VACANT);

            Bed bedFacOcc = createBed(3L, rFac, "G1", BedStatus.VACANT);
            Bed bedFacFree = createBed(4L, rFac, "G2", BedStatus.VACANT);

            Bed bedPackOcc1 = createBed(5L, rPack, "G1", BedStatus.VACANT);
            Bed bedPackOcc2 = createBed(6L, rPack, "G2", BedStatus.VACANT);
            Bed bedPackFree = createBed(7L, rPack, "G3", BedStatus.VACANT);

            Bed bedEmptyFree = createBed(8L, rEmpty, "G1", BedStatus.VACANT);

            // Occupants
            Student svSameClass = createStudent(101L, Gender.MALE, "CNTT-01", "CNTT");
            Student svSameFac = createStudent(102L, Gender.MALE, "CNTT-02", "CNTT");
            Student svOther1 = createStudent(103L, Gender.MALE, "KT-01", "KETOAN");
            Student svOther2 = createStudent(104L, Gender.MALE, "KT-02", "KETOAN");

            Contract c1 = new Contract(); c1.setBed(bedClassOcc); c1.setStudent(svSameClass);
            Contract c2 = new Contract(); c2.setBed(bedFacOcc); c2.setStudent(svSameFac);
            Contract c3 = new Contract(); c3.setBed(bedPackOcc1); c3.setStudent(svOther1);
            Contract c4 = new Contract(); c4.setBed(bedPackOcc2); c4.setStudent(svOther2);

            // Sinh viên nộp đơn thuộc lớp CNTT-01, khoa CNTT
            Student applicant = createStudent(1L, Gender.MALE, "CNTT-01", "CNTT");
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.POLICY, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(
                    List.of(bedClassOcc, bedClassFree, bedFacOcc, bedFacFree, bedPackOcc1, bedPackOcc2, bedPackFree, bedEmptyFree));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c1, c2, c3, c4));

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Ưu tiên cao nhất là cùng lớp: phải vào bedClassFree trong rClass
            assertEquals(bedClassFree.getId(), item.getBed().getId());
        }

        @Test
        @DisplayName("Comparator roomBonus: Cùng khoa (40+pack) thắng gom phòng (pack) dù phòng pack có nhiều người hơn")
        void testComparator_SameFacultyBeatsPacking() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            Room rFac = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rPack = createRoom(2L, b, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedFacOcc = createBed(1L, rFac, "G1", BedStatus.VACANT);
            Bed bedFacFree = createBed(2L, rFac, "G2", BedStatus.VACANT);

            Bed bedPackOcc1 = createBed(3L, rPack, "G1", BedStatus.VACANT);
            Bed bedPackOcc2 = createBed(4L, rPack, "G2", BedStatus.VACANT);
            Bed bedPackFree = createBed(5L, rPack, "G3", BedStatus.VACANT);

            // rFac có 1 người cùng khoa (bonus = 40 + 5 = 45)
            // rPack có 2 người khác khoa (bonus = 10)
            Student svSameFac = createStudent(101L, Gender.MALE, "CNTT-02", "CNTT");
            Student svOther1 = createStudent(102L, Gender.MALE, "KT-01", "KETOAN");
            Student svOther2 = createStudent(103L, Gender.MALE, "KT-02", "KETOAN");

            Contract c1 = new Contract(); c1.setBed(bedFacOcc); c1.setStudent(svSameFac);
            Contract c2 = new Contract(); c2.setBed(bedPackOcc1); c2.setStudent(svOther1);
            Contract c3 = new Contract(); c3.setBed(bedPackOcc2); c3.setStudent(svOther2);

            Student applicant = createStudent(1L, Gender.MALE, "CNTT-01", "CNTT");
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.POLICY, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedFacOcc, bedFacFree, bedPackOcc1, bedPackOcc2, bedPackFree));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c1, c2, c3));

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Cùng khoa (bonus 45) thắng pack 2 người (bonus 10)
            assertEquals(bedFacFree.getId(), item.getBed().getId());
        }

        @Test
        @DisplayName("Comparator roomBonus: Khi không cùng lớp/khoa, ưu tiên gom phòng đông người hơn (pack = size * 5)")
        void testComparator_PackingBonus_MoreOccupantsPreferred() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            Room r2People = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room r1Person = createRoom(2L, b, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedA1 = createBed(1L, r2People, "G1", BedStatus.VACANT);
            Bed bedA2 = createBed(2L, r2People, "G2", BedStatus.VACANT);
            Bed bedAFree = createBed(3L, r2People, "G3", BedStatus.VACANT);

            Bed bedB1 = createBed(4L, r1Person, "G1", BedStatus.VACANT);
            Bed bedBFree = createBed(5L, r1Person, "G2", BedStatus.VACANT);

            Student sv1 = createStudent(101L, Gender.MALE, "KT-01", "KETOAN");
            Student sv2 = createStudent(102L, Gender.MALE, "KT-02", "KETOAN");
            Student sv3 = createStudent(103L, Gender.MALE, "LUAT-01", "LUAT");

            Contract c1 = new Contract(); c1.setBed(bedA1); c1.setStudent(sv1);
            Contract c2 = new Contract(); c2.setBed(bedA2); c2.setStudent(sv2);
            Contract c3 = new Contract(); c3.setBed(bedB1); c3.setStudent(sv3);

            // Applicant ngành CNTT (không trùng lớp/khoa của ai)
            Student applicant = createStudent(1L, Gender.MALE, "CNTT-01", "CNTT");
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedA1, bedA2, bedAFree, bedB1, bedBFree));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c1, c2, c3));

            AllocationRunResult result = engine.plan(1L);
            AllocationItem item = result.getItems().get(0);

            // Được xếp vào r2People vì có 2 người (pack 10 > pack 5)
            assertEquals(bedAFree.getId(), item.getBed().getId());
        }

        @Test
        @DisplayName("Comparator roomNumber: Số tự nhiên tăng dần (101 < 102), số đứng trước chữ (102 < 101A), chữ theo lexicographic (101A < 101B)")
        void testComparator_RoomNumberOrdering() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            Room r201 = createRoom(1L, b, "201", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room r102 = createRoom(2L, b, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room r101 = createRoom(3L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room r101A = createRoom(4L, b, "101A", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room r101B = createRoom(5L, b, "101B", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bed201 = createBed(1L, r201, "G1", BedStatus.VACANT);
            Bed bed102 = createBed(2L, r102, "G1", BedStatus.VACANT);
            Bed bed101 = createBed(3L, r101, "G1", BedStatus.VACANT);
            Bed bed101A = createBed(4L, r101A, "G1", BedStatus.VACANT);
            Bed bed101B = createBed(5L, r101B, "G1", BedStatus.VACANT);

            Student applicant = createStudent(1L, Gender.MALE, null, null);
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            // Case 1: Giữa các phòng số ("201", "102", "101") -> chọn "101"
            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed201, bed102, bed101));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result1 = engine.plan(1L);
            assertEquals(bed101.getId(), result1.getItems().get(0).getBed().getId());

            // Case 2: Giữa số và không phải số thuần túy ("102" vs "101A") -> số thuần túy xếp trước ("102")
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed101A, bed102));
            AllocationRunResult result2 = engine.plan(1L);
            assertEquals(bed102.getId(), result2.getItems().get(0).getBed().getId());

            // Case 3: Giữa hai phòng không parse được số ("101B" vs "101A") -> so sánh chuỗi ("101A" xếp trước)
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed101B, bed101A));
            AllocationRunResult result3 = engine.plan(1L);
            assertEquals(bed101A.getId(), result3.getItems().get(0).getBed().getId());
        }

        @Test
        @DisplayName("Comparator bedCode & bed.id: Cùng phòng chọn bedCode nhỏ hơn (G1 < G2); cùng bedCode chọn id nhỏ hơn")
        void testComparator_BedCodeAndIdTieBreak() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);
            Room r = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            // Cùng phòng, G1 (id 2) vs G2 (id 1)
            Bed bedG2 = createBed(1L, r, "G2", BedStatus.VACANT);
            Bed bedG1 = createBed(2L, r, "G1", BedStatus.VACANT);

            Student applicant = createStudent(1L, Gender.MALE, null, null);
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedG2, bedG1));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result1 = engine.plan(1L);
            // G1 được chọn trước G2 bất kể id
            assertEquals(bedG1.getId(), result1.getItems().get(0).getBed().getId());

            // Cùng phòng, cùng mã giường G1, nhưng id khác nhau: id 10 vs id 20
            Bed bedG1_10 = createBed(10L, r, "G1", BedStatus.VACANT);
            Bed bedG1_20 = createBed(20L, r, "G1", BedStatus.VACANT);

            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedG1_20, bedG1_10));
            AllocationRunResult result2 = engine.plan(1L);
            // bed có id nhỏ hơn (10L) được chọn
            assertEquals(bedG1_10.getId(), result2.getItems().get(0).getBed().getId());
        }

        @Test
        @DisplayName("Null-safe: classCode/facultyCode là null ở cả ứng viên và người đang ở không gây lỗi")
        void testComparator_NullSafeClassAndFaculty() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            Room rOccupied = createRoom(1L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rEmpty = createRoom(2L, b, "102", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedOcc = createBed(1L, rOccupied, "G1", BedStatus.VACANT);
            Bed bedFreeInOcc = createBed(2L, rOccupied, "G2", BedStatus.VACANT);
            Bed bedFreeInEmpty = createBed(3L, rEmpty, "G1", BedStatus.VACANT);

            // Người đang ở có classCode = null và facultyCode = null
            Student occupant = createStudent(99L, Gender.MALE, null, null);
            Contract c = new Contract();
            c.setBed(bedOcc);
            c.setStudent(occupant);

            // Ứng viên cũng có classCode = null và facultyCode = null
            Student applicant = createStudent(1L, Gender.MALE, null, null);
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedOcc, bedFreeInOcc, bedFreeInEmpty));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c));

            // Chạy không văng NullPointerException!
            assertDoesNotThrow(() -> {
                AllocationRunResult result = engine.plan(1L);
                AllocationItem item = result.getItems().get(0);
                assertNotNull(item.getBed());
                // Vì null không bằng null nên không được bonus cùng lớp (100) hay cùng khoa (40),
                // nhưng rOccupied có 1 người ở nên được pack bonus (5) -> chọn rOccupied!
                assertEquals(bedFreeInOcc.getId(), item.getBed().getId());
            });
        }

        @Test
        @DisplayName("Null-safe: Room không có số phòng (null hoặc rỗng) được xếp an toàn vào NON_NUMERIC_LAST")
        void testComparator_NullSafeRoomProperties() {
            mockSystemConfigs("SOFT");

            Building b = createBuilding(1L, BuildingGenderPolicy.MALE);

            Room rNullNumber = createRoom(1L, b, null, RoomType.STANDARD_4, RoomStatus.ACTIVE);
            Room rNumeric = createRoom(2L, b, "101", RoomType.STANDARD_4, RoomStatus.ACTIVE);

            Bed bedNull = createBed(1L, rNullNumber, "G1", BedStatus.VACANT);
            Bed bedNum = createBed(2L, rNumeric, "G1", BedStatus.VACANT);

            Student applicant = createStudent(1L, Gender.MALE, null, null);
            RoomApplication app = createApp(applicant, null, null, PriorityCategory.NONE, false, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedNull, bedNum));
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> {
                AllocationRunResult result = engine.plan(1L);
                // "101" có số nên được xếp trước rNullNumber
                assertEquals(bedNum.getId(), result.getItems().get(0).getBed().getId());
            });
        }
    }

    // ==========================================
    // 5. CÁC TÌNH HUỐNG KHÁC (SKIPPED, FALLBACK)
    // ==========================================
    @Nested
    @DisplayName("5. Special and Skipped cases")
    class SpecialCasesTests {

        @Test
        @DisplayName("Sinh viên bị cấm ở hoặc đã có hợp đồng chiếm giữ thì SKIPPED")
        void testSkippedBlockedAndHoused() {
            mockSystemConfigs("SOFT");

            Student sBlocked = createStudent(1L, Gender.MALE, null, null);
            sBlocked.setBlockedFromHousing(true);

            Student sHoused = createStudent(2L, Gender.MALE, null, null);

            RoomApplication aBlocked = createApp(sBlocked, null, null, PriorityCategory.NONE, false, LocalDateTime.now());
            RoomApplication aHoused = createApp(sHoused, null, null, PriorityCategory.NONE, false, LocalDateTime.now().plusSeconds(1));

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(aBlocked, aHoused));
            when(contractRepository.existsByStudentIdAndStatusIn(eq(2L), any())).thenReturn(true);
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(Collections.emptyList());
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            assertEquals(AllocationResult.SKIPPED, items.get(0).getResult());
            assertEquals("SKIPPED_BLOCKED", items.get(0).getReason());

            assertEquals(AllocationResult.SKIPPED, items.get(1).getResult());
            assertEquals("SKIPPED_ALREADY_HOUSED", items.get(1).getReason());
        }

        @Test
        @DisplayName("Fallback config: Khi system_configs rỗng thì dùng giá trị mặc định (1000, 500, 200, SOFT)")
        void testFallbackConfigs() {
            when(systemConfigRepository.findById("alloc.weight.policy")).thenReturn(Optional.empty());
            when(systemConfigRepository.findById("alloc.weight.remote")).thenReturn(Optional.empty());
            when(systemConfigRepository.findById("alloc.weight.prev_good")).thenReturn(Optional.empty());
            when(systemConfigRepository.findById("alloc.preference.mode")).thenReturn(Optional.empty());

            Student sA = createStudent(1L, Gender.MALE, null, null);
            RoomApplication aA = createApp(sA, null, null, PriorityCategory.POLICY, true, LocalDateTime.now());

            when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(aA));
            when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(Collections.emptyList());
            when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

            AllocationRunResult result = engine.plan(1L);
            List<AllocationItem> items = result.getItems();

            assertEquals(1, items.size());
            // 1000 + 200 = 1200
            assertEquals(1200, items.get(0).getScore());
        }
    }

    // ==========================================
    // HELPER CREATION METHODS
    // ==========================================
    private Building createBuilding(Long id, BuildingGenderPolicy genderPolicy) {
        Building b = new Building();
        b.setId(id);
        b.setCode("B" + id);
        b.setName("Building " + id);
        b.setGenderPolicy(genderPolicy);
        b.setActive(true);
        return b;
    }

    private Room createRoom(Long id, Building building, String roomNumber, RoomType roomType, RoomStatus status) {
        Room r = new Room();
        r.setId(id);
        r.setBuilding(building);
        r.setRoomNumber(roomNumber);
        r.setRoomType(roomType);
        r.setStatus(status);
        return r;
    }

    private Bed createBed(Long id, Room room, String bedCode, BedStatus status) {
        Bed b = new Bed();
        b.setId(id);
        b.setRoom(room);
        b.setBedCode(bedCode);
        b.setStatus(status);
        return b;
    }

    private Student createStudent(Long id, Gender gender, String classCode, String facultyCode) {
        Student s = new Student();
        s.setId(id);
        s.setFullName("Student " + id);
        s.setGender(gender);
        s.setClassCode(classCode);
        s.setFacultyCode(facultyCode);
        s.setBlockedFromHousing(false);
        return s;
    }

    private RoomApplication createApp(Student student, Building preferredBuilding, RoomType preferredRoomType,
                                      PriorityCategory priority, boolean prevStayGood, LocalDateTime submittedAt) {
        RoomApplication app = new RoomApplication();
        app.setStudent(student);
        app.setPreferredBuilding(preferredBuilding);
        app.setPreferredRoomType(preferredRoomType);
        app.setPrioritySnapshot(priority);
        app.setPreviousStayGoodSnapshot(prevStayGood);
        app.setSubmittedAt(submittedAt);
        return app;
    }
}
