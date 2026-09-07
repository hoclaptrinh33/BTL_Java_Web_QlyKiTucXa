package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.dto.ViolationForm;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.ViolationRepository;

@ExtendWith(MockitoExtension.class)
class ViolationServiceTest {

    @Mock
    private ViolationRepository violationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemConfigRepository systemConfigRepository;

    private ViolationService violationService;

    @BeforeEach
    void setUp() {
        violationService = new ViolationService(violationRepository, studentRepository, userRepository, systemConfigRepository);
        when(violationRepository.save(any(Violation.class))).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void recordViolation_prefillDamageBlocksAndSuggestsTerminateAtZero() {
        Student student = student(35);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user()));
        when(systemConfigRepository.findById("conduct.warn.threshold")).thenReturn(Optional.empty());

        ViolationForm form = new ViolationForm();
        form.setStudentId(1L);
        form.setViolationType(ViolationType.DAMAGE);

        ViolationService.RecordViolationResult result = violationService.recordViolation(form, "admin");

        assertEquals(0, student.getConductScore());
        assertTrue(student.getBlockedFromHousing());
        assertTrue(result.isBlockedAndSuggestTerminate());
        assertTrue(result.isWarning());
        assertEquals(50, result.getViolation().getPointsDeducted());
        assertEquals(ViolationSeverity.SEVERE, result.getViolation().getSeverity());
    }

    @Test
    void recordViolation_otherTypeUsesSeverityFallbackAndWarnThresholdConfig() {
        Student student = student(60);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByUsername("staffA")).thenReturn(Optional.of(user()));
        when(systemConfigRepository.findById("conduct.warn.threshold")).thenReturn(Optional.of(config("55")));

        ViolationForm form = new ViolationForm();
        form.setStudentId(1L);
        form.setViolationType(ViolationType.OTHER);
        form.setSeverity(ViolationSeverity.MAJOR);

        ViolationService.RecordViolationResult result = violationService.recordViolation(form, "staffA");

        assertEquals(40, student.getConductScore());
        assertTrue(result.isWarning());
        assertFalse(result.isBlockedAndSuggestTerminate());
        assertEquals(20, result.getViolation().getPointsDeducted());
    }

    @Test
    void resetConductScore_setsConfiguredInitialAndUnblocks() {
        Student student = student(0);
        student.setBlockedFromHousing(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(systemConfigRepository.findById("conduct.initial")).thenReturn(Optional.of(config("100")));

        Student updated = violationService.resetConductScore(1L);

        assertNotNull(updated);
        assertEquals(100, updated.getConductScore());
        assertFalse(updated.getBlockedFromHousing());
        verify(studentRepository).save(student);
    }

    private static Student student(int score) {
        Student student = new Student();
        student.setId(1L);
        student.setConductScore(score);
        student.setBlockedFromHousing(false);
        return student;
    }

    private static User user() {
        User user = new User();
        user.setId(10L);
        user.setUsername("admin");
        return user;
    }

    private static SystemConfig config(String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigValue(value);
        return config;
    }
}
