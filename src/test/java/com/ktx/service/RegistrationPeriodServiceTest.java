package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.PeriodGenderScope;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.dto.RegistrationPeriodForm;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.BuildingRepository;
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
    @Mock
    private BuildingRepository buildingRepository;

    private RegistrationPeriodService periodService;
    private User testUser;

    @BeforeEach
    void setUp() {
        periodService = new RegistrationPeriodService(periodRepository, roomApplicationRepository, allocationRunRepository, buildingRepository);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
    }

    @Test
    void create_success() {
        RegistrationPeriodForm form = createValidForm();
        form.setGenderScope(PeriodGenderScope.MALE_ONLY);
        form.setMinConductScore(65);
        form.setTargetCohort("Tân sinh viên K68");
        form.setTargetQuota(200);
        form.setDepositRatio(BigDecimal.valueOf(0.50));
        form.setContactPhone("02438691234");
        form.setContactEmail("ktx@university.edu.vn");

        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.create(form, testUser);

        assertNotNull(result);
        assertEquals("Đợt tân SV 2026", result.getName());
        assertEquals(PeriodType.FRESHMAN, result.getPeriodType());
        assertEquals("2026-2027", result.getAcademicYear());
        assertEquals(PeriodStatus.DRAFT, result.getStatus());
        assertEquals(testUser, result.getCreatedBy());
        assertEquals(PeriodGenderScope.MALE_ONLY, result.getGenderScope());
        assertEquals(65, result.getMinConductScore());
        assertEquals("Tân sinh viên K68", result.getTargetCohort());
        assertEquals(200, result.getTargetQuota());
        assertEquals("02438691234", result.getContactPhone());
        assertEquals("ktx@university.edu.vn", result.getContactEmail());
        verify(periodRepository, times(1)).save(any(RegistrationPeriod.class));
    }

    @Test
    void create_withBuildings_success() {
        RegistrationPeriodForm form = createValidForm();
        form.setBuildingIds(List.of(10L, 20L));

        Building b1 = new Building();
        b1.setId(10L);
        Building b2 = new Building();
        b2.setId(20L);

        when(buildingRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(b1, b2));
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.create(form, testUser);

        assertNotNull(result);
        assertEquals(2, result.getBuildings().size());
        verify(buildingRepository, times(1)).findAllById(List.of(10L, 20L));
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
        form.setTermStart(LocalDate.now().plusMonths(5));
        form.setTermEnd(LocalDate.now());

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.create(form, testUser));
        assertEquals(RegistrationPeriodService.INVALID_TERM_DATES, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void create_throwsWhenCheckinStartAfterCheckinEnd() {
        RegistrationPeriodForm form = createValidForm();
        form.setCheckinStart(LocalDate.now().plusDays(10));
        form.setCheckinEnd(LocalDate.now().plusDays(5));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.create(form, testUser));
        assertEquals(RegistrationPeriodService.INVALID_CHECKIN_DATES, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void create_throwsWhenPaymentDeadlineBeforeCloseAt() {
        RegistrationPeriodForm form = createValidForm();
        form.setCloseAt(LocalDateTime.now().plusDays(5));
        form.setPaymentDeadline(LocalDateTime.now().plusDays(2));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.create(form, testUser));
        assertEquals(RegistrationPeriodService.INVALID_PAYMENT_DEADLINE, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void update_success() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);
        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));

        RegistrationPeriodForm form = createValidForm();
        form.setName("Đợt tân SV 2026 - Đã sửa");
        form.setStatus(PeriodStatus.DRAFT);
        form.setBuildingIds(List.of(10L));

        Building b1 = new Building();
        b1.setId(10L);
        when(buildingRepository.findAllById(List.of(10L))).thenReturn(List.of(b1));
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.update(id, form);

        assertNotNull(result);
        assertEquals("Đợt tân SV 2026 - Đã sửa", result.getName());
        assertEquals(1, result.getBuildings().size());
        verify(periodRepository, times(1)).save(existing);
    }

    @Test
    void update_throwsWhenOpenTypeDuplicate() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.DRAFT);
        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));

        RegistrationPeriodForm form = createValidForm();
        form.setStatus(PeriodStatus.OPEN);

        when(periodRepository.existsByStatusAndPeriodTypeAndIdNot(PeriodStatus.OPEN, form.getPeriodType(), id))
                .thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.update(id, form));
        assertEquals(RegistrationPeriodService.DUPLICATE_OPEN_TYPE, exception.getMessage());
        verify(periodRepository, never()).save(any(RegistrationPeriod.class));
    }

    @Test
    void update_throwsWhenOpenToDraft() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.OPEN);
        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));

        RegistrationPeriodForm form = createValidForm();
        form.setStatus(PeriodStatus.DRAFT);

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.update(id, form));
        assertEquals(RegistrationPeriodService.INVALID_TRANSITION, exception.getMessage());
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
        existing.setStatus(PeriodStatus.DRAFT);
        existing.setPeriodType(PeriodType.FRESHMAN);
        existing.setOpenAt(LocalDateTime.now().minusDays(1));
        existing.setCloseAt(LocalDateTime.now().plusDays(2));
        existing.setTermStart(LocalDate.now());
        existing.setTermEnd(LocalDate.now().plusMonths(5));

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));
        when(periodRepository.existsByStatusAndPeriodType(PeriodStatus.OPEN, PeriodType.FRESHMAN)).thenReturn(false);
        when(periodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationPeriod result = periodService.transitionToOpen(id);

        assertEquals(PeriodStatus.OPEN, result.getStatus());
        verify(periodRepository, times(1)).save(existing);
    }

    @Test
    void transitionToOpen_throwsWhenNotDraft() {
        Long id = 1L;
        RegistrationPeriod existing = new RegistrationPeriod();
        existing.setId(id);
        existing.setStatus(PeriodStatus.CLOSED);

        when(periodRepository.findById(id)).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(BusinessException.class, () -> periodService.transitionToOpen(id));
        assertEquals(RegistrationPeriodService.INVALID_TRANSITION, exception.getMessage());
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

    @Test
    void getByIdWithDetails_success() {
        Long id = 1L;
        RegistrationPeriod period = new RegistrationPeriod();
        period.setId(id);
        when(periodRepository.findByIdWithBuildingsAndCreator(id)).thenReturn(Optional.of(period));

        RegistrationPeriod result = periodService.getByIdWithDetails(id);
        assertNotNull(result);
        assertEquals(id, result.getId());
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
