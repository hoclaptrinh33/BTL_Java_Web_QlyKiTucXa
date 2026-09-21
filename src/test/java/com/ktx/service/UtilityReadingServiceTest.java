package com.ktx.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.User;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.dto.UtilityReadingForm;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.UtilityReadingRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.impl.UtilityReadingServiceImpl;

@ExtendWith(MockitoExtension.class)
class UtilityReadingServiceTest {

    @Mock
    private UtilityReadingRepository utilityReadingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StaffScope staffScope;

    @Mock
    private Authentication auth;

    private UtilityReadingService utilityReadingService;

    private Room room;
    private User staffUser;

    @BeforeEach
    void setUp() {
        utilityReadingService = new UtilityReadingServiceImpl(
                utilityReadingRepository,
                roomRepository,
                userRepository,
                invoiceRepository,
                staffScope
        );

        Building building = new Building();
        building.setId(1L);
        building.setCode("A1");

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setBuilding(building);

        staffUser = new User();
        staffUser.setId(2L);
        staffUser.setUsername("staff_a1");
    }

    @Test
    @DisplayName("Ghi chỉ số thông thường (không thay công tơ) thành công")
    void testRecordReading_normal_success() {
        when(auth.getName()).thenReturn("staff_a1");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("staff_a1")).thenReturn(Optional.of(staffUser));
        when(invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                eq(10L), eq(LocalDate.of(2026, 9, 1)), eq(InvoiceType.UTILITY), eq(InvoiceStatus.CANCELLED)
        )).thenReturn(false);
        when(utilityReadingRepository.findByRoomIdAndBillingMonth(10L, LocalDate.of(2026, 9, 1)))
                .thenReturn(Optional.empty());
        when(utilityReadingRepository.save(any(UtilityReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        form.setElecPrev(100);
        form.setElecCurr(250);
        form.setElecReplaced(false);
        form.setWaterPrev(10);
        form.setWaterCurr(25);
        form.setWaterReplaced(false);

        UtilityReading saved = utilityReadingService.recordReading(form, auth);

        assertNotNull(saved);
        assertEquals(150, saved.calculateKwh());
        assertEquals(15, saved.calculateM3());
        assertFalse(saved.getElecReplaced());
        assertFalse(saved.getWaterReplaced());
        assertEquals(staffUser, saved.getRecordedBy());
        verify(staffScope).assertRoom(auth, room);
    }

    @Test
    @DisplayName("Ghi chỉ số khi thay công tơ điện (độc lập với nước không thay) thành công")
    void testRecordReading_elecReplaced_success() {
        when(auth.getName()).thenReturn("staff_a1");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("staff_a1")).thenReturn(Optional.of(staffUser));
        when(invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                eq(10L), eq(LocalDate.of(2026, 9, 1)), eq(InvoiceType.UTILITY), eq(InvoiceStatus.CANCELLED)
        )).thenReturn(false);
        when(utilityReadingRepository.findByRoomIdAndBillingMonth(10L, LocalDate.of(2026, 9, 1)))
                .thenReturn(Optional.empty());
        when(utilityReadingRepository.save(any(UtilityReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        // Thay công tơ điện: old_final=180, new_start=0, curr=40 => (180-100) + (40-0) = 120 kWh
        form.setElecReplaced(true);
        form.setElecPrev(100);
        form.setElecOldFinal(180);
        form.setElecNewStart(0);
        form.setElecCurr(40);

        // Nước không thay: 25 - 10 = 15 m3
        form.setWaterReplaced(false);
        form.setWaterPrev(10);
        form.setWaterCurr(25);

        UtilityReading saved = utilityReadingService.recordReading(form, auth);

        assertNotNull(saved);
        assertTrue(saved.getElecReplaced());
        assertFalse(saved.getWaterReplaced());
        assertEquals(120, saved.calculateKwh());
        assertEquals(15, saved.calculateM3());
    }

    @Test
    @DisplayName("Ghi chỉ số khi thay công tơ nước (độc lập với điện không thay) thành công")
    void testRecordReading_waterReplaced_success() {
        when(auth.getName()).thenReturn("staff_a1");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("staff_a1")).thenReturn(Optional.of(staffUser));
        when(invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                eq(10L), eq(LocalDate.of(2026, 9, 1)), eq(InvoiceType.UTILITY), eq(InvoiceStatus.CANCELLED)
        )).thenReturn(false);
        when(utilityReadingRepository.findByRoomIdAndBillingMonth(10L, LocalDate.of(2026, 9, 1)))
                .thenReturn(Optional.empty());
        when(utilityReadingRepository.save(any(UtilityReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        // Điện không thay
        form.setElecReplaced(false);
        form.setElecPrev(100);
        form.setElecCurr(200);

        // Nước thay: (80-50) + (25-5) = 30 + 20 = 50 m3
        form.setWaterReplaced(true);
        form.setWaterPrev(50);
        form.setWaterOldFinal(80);
        form.setWaterNewStart(5);
        form.setWaterCurr(25);

        UtilityReading saved = utilityReadingService.recordReading(form, auth);

        assertNotNull(saved);
        assertFalse(saved.getElecReplaced());
        assertTrue(saved.getWaterReplaced());
        assertEquals(100, saved.calculateKwh());
        assertEquals(50, saved.calculateM3());
    }

    @Test
    @DisplayName("Thất bại khi không thay công tơ mà curr < prev")
    void testRecordReading_failsWhenCurrLessThanPrev() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        form.setElecReplaced(false);
        form.setElecPrev(200);
        form.setElecCurr(150); // Nhỏ hơn prev
        form.setWaterPrev(10);
        form.setWaterCurr(20);

        assertThrows(BusinessException.class, () -> utilityReadingService.recordReading(form, auth));
    }

    @Test
    @DisplayName("Thất bại khi cờ thay công tơ điện bật nhưng thiếu old_final / new_start")
    void testRecordReading_failsWhenReplacedMissingFields() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        form.setElecReplaced(true);
        form.setElecPrev(100);
        form.setElecOldFinal(null); // Thiếu old_final
        form.setElecNewStart(0);
        form.setElecCurr(50);
        form.setWaterPrev(10);
        form.setWaterCurr(20);

        assertThrows(BusinessException.class, () -> utilityReadingService.recordReading(form, auth));
    }

    @Test
    @DisplayName("Khóa chỉnh sửa khi đã có hóa đơn hoạt động (chưa hủy)")
    void testRecordReading_lockedWhenInvoiceExists() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                eq(10L), eq(LocalDate.of(2026, 9, 1)), eq(InvoiceType.UTILITY), eq(InvoiceStatus.CANCELLED)
        )).thenReturn(true);

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        form.setElecPrev(100);
        form.setElecCurr(200);
        form.setWaterPrev(10);
        form.setWaterCurr(20);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> utilityReadingService.recordReading(form, auth));
        assertTrue(ex.getMessage().contains("Không thể chỉnh sửa chỉ số"));
        verify(utilityReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thất bại khi cán bộ không có quyền quản lý phòng (StaffScope violation)")
    void testRecordReading_deniedByStaffScope() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        doThrow(new AccessDeniedException("Không có quyền"))
                .when(staffScope).assertRoom(auth, room);

        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");

        assertThrows(AccessDeniedException.class,
                () -> utilityReadingService.recordReading(form, auth));
    }

    @Test
    @DisplayName("prepareForm kế thừa chỉ số cuối kỳ của tháng trước nếu chưa có reading tháng này")
    void testPrepareForm_prefillsFromPreviousMonth() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(utilityReadingRepository.findByRoomIdAndBillingMonth(10L, LocalDate.of(2026, 9, 1)))
                .thenReturn(Optional.empty());

        UtilityReading prevReading = new UtilityReading();
        prevReading.setElecCurr(320);
        prevReading.setWaterCurr(45);
        when(utilityReadingRepository.findByRoomIdAndBillingMonth(10L, LocalDate.of(2026, 8, 1)))
                .thenReturn(Optional.of(prevReading));

        UtilityReadingForm form = utilityReadingService.prepareForm(10L, YearMonth.of(2026, 9));

        assertNotNull(form);
        assertEquals(320, form.getElecPrev());
        assertEquals(45, form.getWaterPrev());
        assertEquals("2026-09", form.getBillingMonth());
        assertEquals(10L, form.getRoomId());
    }
}
