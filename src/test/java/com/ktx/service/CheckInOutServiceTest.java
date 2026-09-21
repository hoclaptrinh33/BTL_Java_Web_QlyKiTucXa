package com.ktx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.CheckInOut;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.CheckInOutType;
import com.ktx.domain.enums.CompletionReason;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.BedRepository;
import com.ktx.repository.CheckInOutRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.impl.CheckInOutServiceImpl;

@ExtendWith(MockitoExtension.class)
class CheckInOutServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CheckInOutRepository checkInOutRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomAssetRepository roomAssetRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BillingEngine billingEngine;

    private CheckInOutService checkInOutService;

    @BeforeEach
    void setUp() {
        checkInOutService = new CheckInOutServiceImpl(
                contractRepository,
                checkInOutRepository,
                bedRepository,
                userRepository,
                roomAssetRepository,
                invoiceRepository,
                billingEngine
        );
    }

    private Contract createSampleContract(Long contractId, ContractStatus status) {
        Building building = new Building();
        building.setId(1L);
        building.setCode("A");
        building.setGenderPolicy(BuildingGenderPolicy.MALE);

        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setBuilding(building);
        room.setPricePerTerm(new BigDecimal("1200000"));

        Bed bed = new Bed();
        bed.setId(100L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.OCCUPIED);
        bed.setRoom(room);

        Student student = new Student();
        student.setId(5L);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");
        student.setGender(Gender.MALE);

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setContractNo("HD-2026-000001");
        contract.setStudent(student);
        contract.setBed(bed);
        contract.setStatus(status);
        contract.setDepositAmount(new BigDecimal("600000"));
        contract.setDepositStatus(DepositStatus.HELD);
        contract.setStartDate(LocalDate.of(2026, 9, 1));
        contract.setEndDate(LocalDate.of(2027, 1, 31));
        return contract;
    }

    private User createSampleUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("staff01");
        return user;
    }

    @Test
    @DisplayName("checkIn: DRAFT sang ACTIVE, tạo CheckInOut, gọi issueDeposit và issueRoomFee")
    void checkIn_success() {
        Contract contract = createSampleContract(1L, ContractStatus.DRAFT);
        User staff = createSampleUser(2L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(checkInOutRepository.existsByContractIdAndEventType(1L, CheckInOutType.CHECK_IN)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(checkInOutRepository.save(any(CheckInOut.class))).thenAnswer(inv -> {
            CheckInOut cio = inv.getArgument(0);
            cio.setId(50L);
            return cio;
        });

        CheckInOut result = checkInOutService.checkIn(1L, 2L, "Bàn giao chìa khóa", true, null);

        assertNotNull(result);
        assertEquals(CheckInOutType.CHECK_IN, result.getEventType());
        assertEquals("Bàn giao chìa khóa", result.getAssetNote());
        assertTrue(result.getOk());
        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertNotNull(contract.getSignedAt());

        verify(contractRepository).save(contract);
        verify(billingEngine).issueDeposit(1L);
        verify(billingEngine).issueRoomFee(1L);
        // Bed không bị nhả khi check-in
        verify(bedRepository, never()).vacateBed(any(Long.class));
    }

    @Test
    @DisplayName("checkIn: Ném ngoại lệ nếu hợp đồng không ở trạng thái DRAFT")
    void checkIn_notDraft_throws() {
        Contract contract = createSampleContract(1L, ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkInOutService.checkIn(1L, 2L, "Note", true, null));
        assertTrue(ex.getMessage().contains("DRAFT"));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("checkIn: Ném ngoại lệ nếu đã check-in trước đó")
    void checkIn_alreadyCheckedIn_throws() {
        Contract contract = createSampleContract(1L, ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(checkInOutRepository.existsByContractIdAndEventType(1L, CheckInOutType.CHECK_IN)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkInOutService.checkIn(1L, 2L, "Note", true, null));
        assertTrue(ex.getMessage().contains("đã được check-in"));
    }

    @Test
    @DisplayName("checkIn: Ném ngoại lệ nếu giới tính sinh viên không khớp chính sách tòa")
    void checkIn_genderMismatch_throws() {
        Contract contract = createSampleContract(1L, ContractStatus.DRAFT);
        contract.getStudent().setGender(Gender.FEMALE); // Sinh viên Nữ nhưng tòa là MALE

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(checkInOutRepository.existsByContractIdAndEventType(1L, CheckInOutType.CHECK_IN)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkInOutService.checkIn(1L, 2L, "Note", true, null));
        assertTrue(ex.getMessage().contains("Giới tính"));
    }

    @Test
    @DisplayName("checkOut từ ACTIVE sang COMPLETED, nhả giường VACANT và hoàn cọc REFUNDED")
    void checkOut_active_success() {
        Contract contract = createSampleContract(1L, ContractStatus.ACTIVE);
        User staff = createSampleUser(2L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.existsByStudentIdAndStatus(5L, InvoiceStatus.OVERDUE)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(checkInOutRepository.save(any(CheckInOut.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bedRepository.vacateBed(100L)).thenReturn(1);

        CheckInOut result = checkInOutService.checkOut(1L, 2L, "Bàn giao phòng sạch sẽ", true, DepositStatus.REFUNDED, false, null);

        assertNotNull(result);
        assertEquals(CheckInOutType.CHECK_OUT, result.getEventType());
        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertEquals(CompletionReason.NORMAL_CHECKOUT, contract.getCompletionReason());
        assertEquals(DepositStatus.REFUNDED, contract.getDepositStatus());

        verify(contractRepository).save(contract);
        verify(bedRepository).vacateBed(100L);
    }

    @Test
    @DisplayName("checkOut từ EXPIRED sang COMPLETED, nhả giường VACANT")
    void checkOut_expired_success() {
        Contract contract = createSampleContract(1L, ContractStatus.EXPIRED);
        User staff = createSampleUser(2L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.existsByStudentIdAndStatus(5L, InvoiceStatus.OVERDUE)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(checkInOutRepository.save(any(CheckInOut.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bedRepository.vacateBed(100L)).thenReturn(1);

        CheckInOut result = checkInOutService.checkOut(1L, 2L, "Hết hạn và rời phòng", true, DepositStatus.REFUNDED, false, null);

        assertNotNull(result);
        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertEquals(CompletionReason.NORMAL_CHECKOUT, contract.getCompletionReason());
        verify(bedRepository).vacateBed(100L);
    }

    @Test
    @DisplayName("checkOut từ TERMINATED sang COMPLETED, lý do FORCED_AFTER_CHECKOUT, cọc FORFEITED, nhả giường")
    void checkOut_terminated_success() {
        Contract contract = createSampleContract(1L, ContractStatus.TERMINATED);
        User staff = createSampleUser(2L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.existsByStudentIdAndStatus(5L, InvoiceStatus.OVERDUE)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(checkInOutRepository.save(any(CheckInOut.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bedRepository.vacateBed(100L)).thenReturn(1);

        CheckInOut result = checkInOutService.checkOut(1L, 2L, "Rời phòng do vi phạm", false, DepositStatus.FORFEITED, false, null);

        assertNotNull(result);
        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertEquals(CompletionReason.FORCED_AFTER_CHECKOUT, contract.getCompletionReason());
        assertEquals(DepositStatus.FORFEITED, contract.getDepositStatus());
        verify(bedRepository).vacateBed(100L);
    }

    @Test
    @DisplayName("checkOut: Ném ngoại lệ nếu hợp đồng ở trạng thái DRAFT")
    void checkOut_draft_throws() {
        Contract contract = createSampleContract(1L, ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkInOutService.checkOut(1L, 2L, "Note", true, DepositStatus.REFUNDED, false, null));
        assertTrue(ex.getMessage().contains("ACTIVE, EXPIRED hoặc TERMINATED"));
        verify(bedRepository, never()).vacateBed(any(Long.class));
    }

    @Test
    @DisplayName("checkOut: Ném ngoại lệ nếu còn hóa đơn OVERDUE và force = false")
    void checkOut_overdueNotForced_throws() {
        Contract contract = createSampleContract(1L, ContractStatus.ACTIVE);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.existsByStudentIdAndStatus(5L, InvoiceStatus.OVERDUE)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkInOutService.checkOut(1L, 2L, "Note", true, DepositStatus.REFUNDED, false, null));
        assertTrue(ex.getMessage().contains("QUÁ HẠN"));
        verify(bedRepository, never()).vacateBed(any(Long.class));
    }

    @Test
    @DisplayName("checkOut: Thành công khi còn hóa đơn OVERDUE nếu force = true (chỉ admin)")
    void checkOut_overdueForced_success() {
        Contract contract = createSampleContract(1L, ContractStatus.ACTIVE);
        User staff = createSampleUser(2L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(checkInOutRepository.save(any(CheckInOut.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bedRepository.vacateBed(100L)).thenReturn(1);

        CheckInOut result = checkInOutService.checkOut(1L, 2L, "Buộc checkout", true, DepositStatus.REFUNDED, true, null);

        assertNotNull(result);
        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        verify(bedRepository).vacateBed(100L);
    }
}
