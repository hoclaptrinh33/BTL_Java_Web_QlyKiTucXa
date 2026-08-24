package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.dto.RegistrationPeriodForm;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationPeriodServiceTest {

    @Mock
    private RegistrationPeriodRepository periodRepository;
    @Mock
    private RoomApplicationRepository roomApplicationRepository;
    @Mock
    private AllocationRunRepository allocationRunRepository;

    private RegistrationPeriodService periodService;
    private User testUser;

    @BeforeEach
    void setUp() {
        periodService = new RegistrationPeriodService(periodRepository, roomApplicationRepository, allocationRunRepository);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
    }

    @Test
    void create_success() {
        RegistrationPeriodForm form = createValidForm();
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.create(form, testUser);

        assertNotNull(result);
        assertEquals("Đợt tân SV 2026", result.getName());
        assertEquals(PeriodType.FRESHMAN, result.getPeriodType());
        assertEquals("2026-2027", result.getAcademicYear());
        assertEquals(PeriodStatus.DRAFT, result.getStatus());
        assertEquals(testUser, result.getCreatedBy());
        verify(periodRepository, times(1)).save(any(RegistrationPeriod.class));
    }

    @Test
    void create_throwsWhenOpenAtAfterCloseAt() {
        RegistrationPeriodForm form = createValidForm();
        form.setOpenAt(LocalDateTime.now().plusDays(2));
        form.setCloseAt(LocalDateTime.now().plusDays(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.create(form, testUser));
        assertEquals(RegistrationPeriodService.INVALID_DATES, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void create_throwsWhenTermStartAfterTermEnd() {
        RegistrationPeriodForm form = createValidForm();
        form.setTermStart(LocalDate.now().plusMonths(6));
        form.setTermEnd(LocalDate.now().plusMonths(5));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.create(form, testUser));
        assertEquals(RegistrationPeriodService.INVALID_TERM_DATES, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void update_success() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);

        RegistrationPeriodForm form = createValidForm();
        form.setStatus(PeriodStatus.DRAFT);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.update(id, form);

        assertNotNull(result);
        assertEquals("Đợt tân SV 2026", result.getName());
        verify(periodRepository, times(1)).save(existing);
    }

    @Test
    void update_throwsWhenStatusOpenAndDuplicateTypeExists() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);

        RegistrationPeriodForm form = createValidForm();
        form.setStatus(PeriodStatus.OPEN);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(periodRepository.existsByStatusAndPeriodTypeAndIdNot(PeriodStatus.OPEN, PeriodType.FRESHMAN, id)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.update(id, form));
        assertEquals(RegistrationPeriodService.DUPLICATE_OPEN_TYPE, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void delete_success() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roomApplicationRepository.existsByPeriodId(id)).thenReturn(false);
        when(allocationRunRepository.existsByPeriodId(id)).thenReturn(false);

        periodService.delete(id);

        verify(periodRepository, times(1)).delete(existing);
    }

    @Test
    void delete_throwsWhenNotDraft() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.OPEN);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.delete(id));
        assertEquals(RegistrationPeriodService.CANNOT_DELETE_NON_DRAFT, exception.getMessage());
        verify(periodRepository, never()).delete(any(RegistrationPeriod.class));
    }

    @Test
    void delete_throwsWhenHasApplications() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roomApplicationRepository.existsByPeriodId(id)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.delete(id));
        assertEquals(RegistrationPeriodService.CANNOT_DELETE_HAS_APPS, exception.getMessage());
        verify(periodRepository, never()).delete(any(RegistrationPeriod.class));
    }

    @Test
    void transitionToOpen_success() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setPeriodType(PeriodType.FRESHMAN);
        existing.setStatus(PeriodStatus.DRAFT);
        existing.setOpenAt(LocalDateTime.now().minusDays(1));
        existing.setCloseAt(LocalDateTime.now().plusDays(2));
        existing.setTermStart(LocalDate.now());
        existing.setTermEnd(LocalDate.now().plusMonths(5));

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(periodRepository.existsByStatusAndPeriodType(PeriodStatus.OPEN, PeriodType.FRESHMAN)).thenReturn(false);
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.transitionToOpen(id);

        assertNotNull(result);
        assertEquals(PeriodStatus.OPEN, result.getStatus());
        verify(periodRepository, times(1)).save(existing);
    }

    @Test
    void transitionToOpen_throwsWhenDuplicateOpenType() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setPeriodType(PeriodType.FRESHMAN);
        existing.setStatus(PeriodStatus.DRAFT);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(periodRepository.existsByStatusAndPeriodType(PeriodStatus.OPEN, PeriodType.FRESHMAN)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.transitionToOpen(id));
        assertEquals(RegistrationPeriodService.DUPLICATE_OPEN_TYPE, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void transitionToClose_success() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.OPEN);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.transitionToClose(id);

        assertNotNull(result);
        assertEquals(PeriodStatus.CLOSED, result.getStatus());
        verify(periodRepository, times(1)).save(existing);
    }

    @Test
    void transitionToClose_throwsWhenNotOpen() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.transitionToClose(id));
        assertEquals(RegistrationPeriodService.INVALID_TRANSITION, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    private RegistrationPeriodForm createValidForm() {
        RegistrationPeriodForm form = new RegistrationPeriodForm();
        form.setName("Đợt tân SV 2026");
        form.setPeriodType(PeriodType.FRESHMAN);
        form.setAcademicYear("2026-2027");
        form.setOpenAt(LocalDateTime.now().minusDays(1));
        form.setCloseAt(LocalDateTime.now().plusDays(2));
        form.setTermStart(LocalDate.now());
        form.setTermEnd(LocalDate.now().plusMonths(5));
        return form;
    }
}
