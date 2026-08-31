package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.SystemConfigRepository;

@ExtendWith(MockitoExtension.class)
class RoomApplicationServiceTest {

    @Mock
    private RoomApplicationRepository roomApplicationRepository;
    @Mock
    private RegistrationPeriodRepository periodRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private SystemConfigRepository systemConfigRepository;

    private RoomApplicationService applicationService;
    
    private Student testStudent;
    private RegistrationPeriod testPeriod;
    private Building testBuilding;

    @BeforeEach
    void setUp() {
        applicationService = new RoomApplicationService(
                roomApplicationRepository, periodRepository, studentRepository, buildingRepository, contractRepository, systemConfigRepository);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFullName("Nguyen Van A");
        testStudent.setGender(Gender.MALE);
        testStudent.setConductScore(80);
        testStudent.setBlockedFromHousing(false);
        testStudent.setPriorityCategory(PriorityCategory.REMOTE_AREA);
        testStudent.setPreviousStayGood(true);

        testPeriod = new RegistrationPeriod();
        testPeriod.setId(2L);
        testPeriod.setName("Dot 1");
        testPeriod.setStatus(PeriodStatus.OPEN);
        testPeriod.setOpenAt(LocalDateTime.now().minusDays(1));
        testPeriod.setCloseAt(LocalDateTime.now().plusDays(5));

        testBuilding = new Building();
        testBuilding.setId(3L);
        testBuilding.setName("Building Male");
        testBuilding.setGenderPolicy(BuildingGenderPolicy.MALE);
        testBuilding.setActive(true);
    }

    @Test
    void submitApplication_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));
        when(buildingRepository.findById(3L)).thenReturn(Optional.of(testBuilding));
        when(roomApplicationRepository.existsByPeriodIdAndStudentId(2L, 1L)).thenReturn(false);
        when(contractRepository.existsByStudentIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        
        SystemConfig configPolicy = new SystemConfig();
        configPolicy.setConfigValue("1000");
        SystemConfig configRemote = new SystemConfig();
        configRemote.setConfigValue("500");
        SystemConfig configPrev = new SystemConfig();
        configPrev.setConfigValue("200");
        when(systemConfigRepository.findById("alloc.weight.policy")).thenReturn(Optional.of(configPolicy));
        when(systemConfigRepository.findById("alloc.weight.remote")).thenReturn(Optional.of(configRemote));
        when(systemConfigRepository.findById("alloc.weight.prev_good")).thenReturn(Optional.of(configPrev));

        when(roomApplicationRepository.save(any(RoomApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomApplication app = applicationService.submitApplication(1L, 2L, 3L, RoomType.STANDARD_4, "Ghi chu test");

        assertNotNull(app);
        assertEquals(testPeriod, app.getPeriod());
        assertEquals(testStudent, app.getStudent());
        assertEquals(testBuilding, app.getPreferredBuilding());
        assertEquals(RoomType.STANDARD_4, app.getPreferredRoomType());
        assertEquals("Ghi chu test", app.getNote());
        assertEquals(PriorityCategory.REMOTE_AREA, app.getPrioritySnapshot());
        assertEquals(true, app.getPreviousStayGoodSnapshot());
        // score: remote(500) + prevGood(200) = 700
        assertEquals(700, app.getComputedScore());
        assertEquals(ApplicationStatus.SUBMITTED, app.getStatus());

        verify(roomApplicationRepository, times(1)).save(any(RoomApplication.class));
    }

    @Test
    void submitApplication_throwsWhenPeriodNotOpen() {
        testPeriod.setStatus(PeriodStatus.DRAFT);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.submitApplication(1L, 2L, null, null, null)
        );
        assertEquals(RoomApplicationService.PERIOD_NOT_OPEN, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void submitApplication_throwsWhenStudentBlocked() {
        testStudent.setBlockedFromHousing(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.submitApplication(1L, 2L, null, null, null)
        );
        assertEquals(RoomApplicationService.STUDENT_BLOCKED, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void submitApplication_throwsWhenConductScoreIsZero() {
        testStudent.setConductScore(0);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.submitApplication(1L, 2L, null, null, null)
        );
        assertEquals(RoomApplicationService.CONDUCT_SCORE_ZERO, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void submitApplication_throwsWhenAlreadySubmitted() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));
        when(roomApplicationRepository.existsByPeriodIdAndStudentId(2L, 1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.submitApplication(1L, 2L, null, null, null)
        );
        assertEquals(RoomApplicationService.ALREADY_SUBMITTED, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void submitApplication_throwsWhenActiveContractExists() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));
        when(roomApplicationRepository.existsByPeriodIdAndStudentId(2L, 1L)).thenReturn(false);
        when(contractRepository.existsByStudentIdAndStatusIn(eq(1L), anyCollection())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.submitApplication(1L, 2L, null, null, null)
        );
        assertEquals(RoomApplicationService.ACTIVE_CONTRACT_EXISTS, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void submitApplication_throwsWhenGenderMismatch() {
        testStudent.setGender(Gender.FEMALE); // Female student trying to apply for Male building
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(testPeriod));
        when(buildingRepository.findById(3L)).thenReturn(Optional.of(testBuilding));
        when(roomApplicationRepository.existsByPeriodIdAndStudentId(2L, 1L)).thenReturn(false);
        when(contractRepository.existsByStudentIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.submitApplication(1L, 2L, 3L, null, null)
        );
        assertEquals(RoomApplicationService.GENDER_MISMATCH, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void withdrawApplication_success() {
        RoomApplication app = new RoomApplication();
        app.setId(10L);
        app.setStudent(testStudent);
        app.setPeriod(testPeriod);
        app.setStatus(ApplicationStatus.SUBMITTED);

        when(roomApplicationRepository.findById(10L)).thenReturn(Optional.of(app));
        when(roomApplicationRepository.save(any(RoomApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomApplication result = applicationService.withdrawApplication(10L, 1L);

        assertEquals(ApplicationStatus.WITHDRAWN, result.getStatus());
        verify(roomApplicationRepository, times(1)).save(app);
    }

    @Test
    void withdrawApplication_throwsWhenAccessDenied() {
        RoomApplication app = new RoomApplication();
        app.setId(10L);
        Student anotherStudent = new Student();
        anotherStudent.setId(99L);
        app.setStudent(anotherStudent);

        when(roomApplicationRepository.findById(10L)).thenReturn(Optional.of(app));

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.withdrawApplication(10L, 1L) // student 1 trying to withdraw student 99's application
        );
        assertEquals(RoomApplicationService.ACCESS_DENIED, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }

    @Test
    void withdrawApplication_throwsWhenPeriodClosed() {
        RoomApplication app = new RoomApplication();
        app.setId(10L);
        app.setStudent(testStudent);
        testPeriod.setStatus(PeriodStatus.CLOSED);
        app.setPeriod(testPeriod);

        when(roomApplicationRepository.findById(10L)).thenReturn(Optional.of(app));

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            applicationService.withdrawApplication(10L, 1L)
        );
        assertEquals(RoomApplicationService.CANNOT_WITHDRAW_CLOSED, ex.getMessage());
        verify(roomApplicationRepository, never()).save(any(RoomApplication.class));
    }
}
