package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Notification;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.ViolationRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.impl.ConductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ConductServiceTest {

    @Mock
    private ViolationRepository violationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private StaffScope staffScope;

    private ConductService conductService;

    private Student student;
    private User staffUser;
    private Contract contract;

    @BeforeEach
    void setUp() {
        conductService = new ConductServiceImpl(
                violationRepository,
                studentRepository,
                userRepository,
                contractRepository,
                systemConfigRepository,
                notificationRepository,
                staffScope);

        User studentUser = new User();
        studentUser.setId(101L);
        studentUser.setUsername("sv01");

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);
        student.setFullName("Nguyen Van A");
        student.setConductScore(100);
        student.setBlockedFromHousing(false);

        staffUser = new User();
        staffUser.setId(201L);
        staffUser.setUsername("staffA");
        staffUser.setRole(Role.STAFF);

        Building building = new Building();
        building.setId(1L);

        Room room = new Room();
        room.setId(50L);
        room.setBuilding(building);

        Bed bed = new Bed();
        bed.setId(80L);
        bed.setRoom(room);

        contract = new Contract();
        contract.setId(1L);
        contract.setStudent(student);
        contract.setBed(bed);
    }

    @Test
    void recordViolation_prefillLateReturn_deducts5Points() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("staffA")).thenReturn(Optional.of(staffUser));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication adminAuth = auth("admin", "ROLE_ADMIN");

        Violation violation = conductService.recordViolation(
                10L, "staffA", ViolationType.LATE_RETURN, null, null, null,
                "Ve muon 23:30", LocalDateTime.now(), adminAuth);

        assertNotNull(violation);
        assertEquals(ViolationType.LATE_RETURN, violation.getViolationType());
        assertEquals(ViolationSeverity.MINOR, violation.getSeverity());
        assertEquals(5, violation.getPointsDeducted());
        assertEquals(ViolationAction.WARNING, violation.getAction());
        assertEquals(95, student.getConductScore());
        assertEquals(false, student.getBlockedFromHousing());

        verify(studentRepository, times(1)).save(student);
        verify(violationRepository, times(1)).save(any(Violation.class));
    }

    @Test
    void recordViolation_scoreNeverNegative() {
        student.setConductScore(20);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("staffA")).thenReturn(Optional.of(staffUser));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication adminAuth = auth("admin", "ROLE_ADMIN");

        // DAMAGE: 50 điểm trừ
        conductService.recordViolation(
                10L, "staffA", ViolationType.DAMAGE, ViolationSeverity.SEVERE, 50, null,
                "Dap vo cua kinh", LocalDateTime.now(), adminAuth);

        assertEquals(0, student.getConductScore());
        // Khi về 0 điểm -> blockedFromHousing = true
        assertTrue(student.getBlockedFromHousing());
    }

    @Test
    void recordViolation_severeTerminate_blocksFromHousing() {
        student.setConductScore(80);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("staffA")).thenReturn(Optional.of(staffUser));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication adminAuth = auth("admin", "ROLE_ADMIN");

        conductService.recordViolation(
                10L, "staffA", ViolationType.DAMAGE, ViolationSeverity.SEVERE, 50, ViolationAction.TERMINATE,
                "Hanh vi nghiem trong", LocalDateTime.now(), adminAuth);

        assertEquals(30, student.getConductScore());
        assertTrue(student.getBlockedFromHousing());
    }

    @Test
    void recordViolation_belowWarnThreshold_createsNotification() {
        student.setConductScore(60);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("staffA")).thenReturn(Optional.of(staffUser));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> inv.getArgument(0));

        SystemConfig warnConfig = new SystemConfig();
        warnConfig.setConfigValue("50");
        when(systemConfigRepository.findById("conduct.warn.threshold")).thenReturn(Optional.of(warnConfig));

        Authentication adminAuth = auth("admin", "ROLE_ADMIN");

        // Trừ 25 điểm -> điểm còn 35 (< 50)
        conductService.recordViolation(
                10L, "staffA", ViolationType.DISTURBANCE, ViolationSeverity.MAJOR, 25, ViolationAction.POINT_DEDUCT,
                "Gay on ao luc 23h", LocalDateTime.now(), adminAuth);

        assertEquals(35, student.getConductScore());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void recordViolation_staffScope_otherBuilding_throwsDenied() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("staffA")).thenReturn(Optional.of(staffUser));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));

        Authentication staffAuth = auth("staffA", "ROLE_STAFF");
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(staffScope).assertBuilding(staffAuth, 1L);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                conductService.recordViolation(
                        10L, "staffA", ViolationType.LATE_RETURN, null, null, null,
                        "Ve muon", LocalDateTime.now(), staffAuth));

        assertEquals(StaffScope.DENIED_BUILDING, ex.getMessage());
    }

    @Test
    void resetAllConductScores_resetsAllTo100() {
        student.setConductScore(30);
        Student s2 = new Student();
        s2.setId(11L);
        s2.setConductScore(60);

        when(studentRepository.findAll()).thenReturn(List.of(student, s2));

        SystemConfig initConfig = new SystemConfig();
        initConfig.setConfigValue("100");
        when(systemConfigRepository.findById("conduct.initial")).thenReturn(Optional.of(initConfig));

        conductService.resetAllConductScores();

        assertEquals(100, student.getConductScore());
        assertEquals(100, s2.getConductScore());
        verify(studentRepository, times(1)).saveAll(List.of(student, s2));
    }

    private static Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }
}
