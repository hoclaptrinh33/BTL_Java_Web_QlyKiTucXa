package com.ktx.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void testRankingAndTieBreak() {
        mockSystemConfigs("SOFT");

        Student sA = new Student(); sA.setId(1L); sA.setBlockedFromHousing(false);
        Student sB = new Student(); sB.setId(2L); sB.setBlockedFromHousing(false);
        Student sC = new Student(); sC.setId(3L); sC.setBlockedFromHousing(false);
        Student sD = new Student(); sD.setId(4L); sD.setBlockedFromHousing(false);

        LocalDateTime now = LocalDateTime.now();

        // A: POLICY + prev good -> score 1200
        RoomApplication aA = new RoomApplication();
        aA.setStudent(sA);
        aA.setPrioritySnapshot(PriorityCategory.POLICY);
        aA.setPreviousStayGoodSnapshot(true);
        aA.setSubmittedAt(now.plusMinutes(10));

        // B: REMOTE + prev good -> score 700
        RoomApplication aB = new RoomApplication();
        aB.setStudent(sB);
        aB.setPrioritySnapshot(PriorityCategory.REMOTE_AREA);
        aB.setPreviousStayGoodSnapshot(true);
        aB.setSubmittedAt(now.plusMinutes(5));

        // C: NONE + prev good -> score 200, nộp sau D
        RoomApplication aC = new RoomApplication();
        aC.setStudent(sC);
        aC.setPrioritySnapshot(PriorityCategory.NONE);
        aC.setPreviousStayGoodSnapshot(true);
        aC.setSubmittedAt(now.plusMinutes(20));

        // D: NONE + prev good -> score 200, nộp trước C
        RoomApplication aD = new RoomApplication();
        aD.setStudent(sD);
        aD.setPrioritySnapshot(PriorityCategory.NONE);
        aD.setPreviousStayGoodSnapshot(true);
        aD.setSubmittedAt(now.plusMinutes(10));

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
        // Rank 2: B (Score 700)
        assertEquals(sB.getId(), items.get(1).getStudent().getId());
        assertEquals(700, items.get(1).getScore());
        // Rank 3: D (Score 200, nộp trước C)
        assertEquals(sD.getId(), items.get(2).getStudent().getId());
        assertEquals(200, items.get(2).getScore());
        // Rank 4: C (Score 200, nộp sau D)
        assertEquals(sC.getId(), items.get(3).getStudent().getId());
        assertEquals(200, items.get(3).getScore());
    }

    @Test
    void testSoftPreferenceRelaxation() {
        mockSystemConfigs("SOFT");

        Building bA = new Building(); bA.setId(1L); bA.setGenderPolicy(BuildingGenderPolicy.MALE);
        Building bB = new Building(); bB.setId(2L); bB.setGenderPolicy(BuildingGenderPolicy.MALE);

        Room rA = new Room(); rA.setId(1L); rA.setBuilding(bA); rA.setRoomType(RoomType.STANDARD_4); rA.setStatus(RoomStatus.ACTIVE); rA.setRoomNumber("101");
        Room rB = new Room(); rB.setId(2L); rB.setBuilding(bB); rB.setRoomType(RoomType.STANDARD_4); rB.setStatus(RoomStatus.ACTIVE); rB.setRoomNumber("102");

        Bed bedA = new Bed(); bedA.setId(1L); bedA.setRoom(rA); bedA.setStatus(BedStatus.VACANT); bedA.setBedCode("G1");
        Bed bedB = new Bed(); bedB.setId(2L); bedB.setRoom(rB); bedB.setStatus(BedStatus.VACANT); bedB.setBedCode("G1");

        // Student s prefers Building A, Room Type STANDARD_4. But we mock bedA as occupied in DB.
        Student s = new Student(); s.setId(1L); s.setGender(Gender.MALE); s.setBlockedFromHousing(false);
        Student other = new Student(); other.setId(99L);

        Contract activeContractOnA = new Contract();
        activeContractOnA.setBed(bedA);
        activeContractOnA.setStudent(other);

        RoomApplication app = new RoomApplication();
        app.setStudent(s);
        app.setPreferredBuilding(bA);
        app.setPreferredRoomType(RoomType.STANDARD_4);
        app.setPrioritySnapshot(PriorityCategory.NONE);
        app.setPreviousStayGoodSnapshot(false);
        app.setSubmittedAt(LocalDateTime.now());

        when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedA, bedB));
        when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(activeContractOnA));

        AllocationRunResult result = engine.plan(1L);
        AllocationItem item = result.getItems().get(0);

        assertEquals(AllocationResult.ASSIGNED, item.getResult());
        // Assigned to bedB (relaxed building)
        assertEquals(bedB.getId(), item.getBed().getId());
        assertEquals("NO_BUILDING_MATCH", item.getReason());
    }

    @Test
    void testStrictPreferenceWaitlist() {
        mockSystemConfigs("STRICT");

        Building bA = new Building(); bA.setId(1L); bA.setGenderPolicy(BuildingGenderPolicy.MALE);
        Room rA = new Room(); rA.setId(1L); rA.setBuilding(bA); rA.setRoomType(RoomType.STANDARD_4); rA.setStatus(RoomStatus.ACTIVE); rA.setRoomNumber("101");
        Bed bedA = new Bed(); bedA.setId(1L); bedA.setRoom(rA); bedA.setStatus(BedStatus.VACANT); bedA.setBedCode("G1");

        // Thêm một giường trống ở tòa khác để hasVacantBedsMatchingGender = true
        Building bB = new Building(); bB.setId(2L); bB.setGenderPolicy(BuildingGenderPolicy.MALE);
        Room rB = new Room(); rB.setId(2L); rB.setBuilding(bB); rB.setRoomType(RoomType.STANDARD_4); rB.setStatus(RoomStatus.ACTIVE); rB.setRoomNumber("102");
        Bed bedB = new Bed(); bedB.setId(2L); bedB.setRoom(rB); bedB.setStatus(BedStatus.VACANT); bedB.setBedCode("G1");

        Student s = new Student(); s.setId(1L); s.setGender(Gender.MALE); s.setBlockedFromHousing(false);
        Student other = new Student(); other.setId(99L);

        // bedA bị chiếm bởi other
        Contract activeContractOnA = new Contract();
        activeContractOnA.setBed(bedA);
        activeContractOnA.setStudent(other);

        RoomApplication app = new RoomApplication();
        app.setStudent(s);
        app.setPreferredBuilding(bA);
        app.setPreferredRoomType(RoomType.STANDARD_4);
        app.setPrioritySnapshot(PriorityCategory.NONE);
        app.setPreviousStayGoodSnapshot(false);
        app.setSubmittedAt(LocalDateTime.now());

        when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedA, bedB));
        when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(activeContractOnA));

        AllocationRunResult result = engine.plan(1L);
        AllocationItem item = result.getItems().get(0);

        assertEquals(AllocationResult.WAITLISTED, item.getResult());
        assertEquals("NO_BUILDING_MATCH", item.getReason());
    }

    @Test
    void testGenderHardConstraint() {
        mockSystemConfigs("SOFT");

        Building maleBuilding = new Building(); maleBuilding.setId(1L); maleBuilding.setGenderPolicy(BuildingGenderPolicy.MALE);
        Room room = new Room(); room.setId(1L); room.setBuilding(maleBuilding); room.setRoomType(RoomType.STANDARD_4); room.setStatus(RoomStatus.ACTIVE); room.setRoomNumber("101");
        Bed bed = new Bed(); bed.setId(1L); bed.setRoom(room); bed.setStatus(BedStatus.VACANT); bed.setBedCode("G1");

        // Female student applying
        Student s = new Student(); s.setId(1L); s.setGender(Gender.FEMALE); s.setBlockedFromHousing(false);

        RoomApplication app = new RoomApplication();
        app.setStudent(s);
        app.setPrioritySnapshot(PriorityCategory.NONE);
        app.setPreviousStayGoodSnapshot(false);
        app.setSubmittedAt(LocalDateTime.now());

        when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(app));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed));
        when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

        AllocationRunResult result = engine.plan(1L);
        AllocationItem item = result.getItems().get(0);

        // Waitlisted because female student cannot go to male building
        assertEquals(AllocationResult.WAITLISTED, item.getResult());
        assertEquals("NO_VACANT_BED", item.getReason());
    }

    @Test
    void testRoommatesClustering() {
        mockSystemConfigs("SOFT");

        Building b = new Building(); b.setId(1L); b.setGenderPolicy(BuildingGenderPolicy.MALE);
        
        // Two rooms in same building
        Room rA = new Room(); rA.setId(1L); rA.setBuilding(b); rA.setRoomType(RoomType.STANDARD_4); rA.setStatus(RoomStatus.ACTIVE); rA.setRoomNumber("101");
        Room rB = new Room(); rB.setId(2L); rB.setBuilding(b); rB.setRoomType(RoomType.STANDARD_4); rB.setStatus(RoomStatus.ACTIVE); rB.setRoomNumber("102");

        Bed bedA1 = new Bed(); bedA1.setId(1L); bedA1.setRoom(rA); bedA1.setStatus(BedStatus.VACANT); bedA1.setBedCode("G1");
        Bed bedA2 = new Bed(); bedA2.setId(2L); bedA2.setRoom(rA); bedA2.setStatus(BedStatus.VACANT); bedA2.setBedCode("G2");
        Bed bedB1 = new Bed(); bedB1.setId(3L); bedB1.setRoom(rB); bedB1.setStatus(BedStatus.VACANT); bedB1.setBedCode("G1");

        // Student 1 (IT class)
        Student s1 = new Student(); s1.setId(1L); s1.setGender(Gender.MALE); s1.setBlockedFromHousing(false); s1.setClassCode("IT-01");
        // Student 2 (IT class, same class)
        Student s2 = new Student(); s2.setId(2L); s2.setGender(Gender.MALE); s2.setBlockedFromHousing(false); s2.setClassCode("IT-01");

        RoomApplication a1 = new RoomApplication();
        a1.setStudent(s1);
        a1.setPrioritySnapshot(PriorityCategory.NONE);
        a1.setPreviousStayGoodSnapshot(false);
        a1.setSubmittedAt(LocalDateTime.now());

        RoomApplication a2 = new RoomApplication();
        a2.setStudent(s2);
        a2.setPrioritySnapshot(PriorityCategory.NONE);
        a2.setPreviousStayGoodSnapshot(false);
        a2.setSubmittedAt(LocalDateTime.now().plusSeconds(1)); // nộp sau a1

        when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(a1, a2));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bedA1, bedA2, bedB1));
        when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

        AllocationRunResult result = engine.plan(1L);
        List<AllocationItem> items = result.getItems();

        // Student 1 assigned first (lowest room number 101, bedA1)
        assertEquals(s1.getId(), items.get(0).getStudent().getId());
        assertEquals(bedA1.getId(), items.get(0).getBed().getId());

        // Student 2 has same class. Room A has s1 (same class). Room B is empty.
        // Therefore Room A gets roomBonus of 100 points, directing Student 2 to bedA2.
        assertEquals(s2.getId(), items.get(1).getStudent().getId());
        assertEquals(bedA2.getId(), items.get(1).getBed().getId());
    }

    @Test
    void testSkippedBlockedAndHoused() {
        mockSystemConfigs("SOFT");

        Student sBlocked = new Student(); sBlocked.setId(1L); sBlocked.setBlockedFromHousing(true);
        Student sHoused = new Student(); sHoused.setId(2L); sHoused.setBlockedFromHousing(false);

        RoomApplication aBlocked = new RoomApplication();
        aBlocked.setStudent(sBlocked);
        aBlocked.setPrioritySnapshot(PriorityCategory.NONE);
        aBlocked.setPreviousStayGoodSnapshot(false);
        aBlocked.setSubmittedAt(LocalDateTime.now());

        RoomApplication aHoused = new RoomApplication();
        aHoused.setStudent(sHoused);
        aHoused.setPrioritySnapshot(PriorityCategory.NONE);
        aHoused.setPreviousStayGoodSnapshot(false);
        aHoused.setSubmittedAt(LocalDateTime.now().plusSeconds(1));

        when(roomApplicationRepository.findByPeriodIdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(List.of(aBlocked, aHoused));
        when(contractRepository.existsByStudentIdAndStatusIn(eq(2L), any())).thenReturn(true); // Already has contract
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(Collections.emptyList());
        when(contractRepository.findOccupyingWithDetails(any())).thenReturn(Collections.emptyList());

        AllocationRunResult result = engine.plan(1L);
        List<AllocationItem> items = result.getItems();

        assertEquals(AllocationResult.SKIPPED, items.get(0).getResult());
        assertEquals("SKIPPED_BLOCKED", items.get(0).getReason());

        assertEquals(AllocationResult.SKIPPED, items.get(1).getResult());
        assertEquals("SKIPPED_ALREADY_HOUSED", items.get(1).getReason());
    }
}
