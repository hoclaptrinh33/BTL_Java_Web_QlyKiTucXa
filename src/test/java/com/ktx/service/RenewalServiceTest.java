package com.ktx.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.RenewalRequest;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.RenewalStatus;
import com.ktx.domain.enums.Role;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RenewalRequestRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.impl.RenewalServiceImpl;

@ExtendWith(MockitoExtension.class)
class RenewalServiceTest {

    @Mock
    private RenewalRequestRepository renewalRequestRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private RenewalService renewalService;

    private Student student;
    private Contract contract;
    private Bed bed;

    @BeforeEach
    void setUp() {
        renewalService = new RenewalServiceImpl(
                renewalRequestRepository,
                contractRepository,
                studentRepository,
                notificationRepository
        );

        student = new Student();
        student.setId(10L);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");
        student.setGender(Gender.MALE);

        User studentUser = new User();
        studentUser.setId(100L);
        studentUser.setUsername("sv001");
        student.setUser(studentUser);

        Building building = new Building();
        building.setId(1L);
        building.setName("Tòa A");

        Room room = new Room();
        room.setId(2L);
        room.setRoomNumber("201");
        room.setBuilding(building);

        bed = new Bed();
        bed.setId(5L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.OCCUPIED);
        bed.setRoom(room);

        contract = new Contract();
        contract.setId(20L);
        contract.setContractNo("HD-2026-001");
        contract.setStudent(student);
        contract.setBed(bed);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 6, 30));
    }

    @Test
    @DisplayName("Nộp gia hạn thành công với số tháng termMonths")
    void submitRenewal_withTermMonths_success() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), any()))
                .thenReturn(List.of(contract));
        when(renewalRequestRepository.existsByStudentIdAndStatus(10L, RenewalStatus.SUBMITTED))
                .thenReturn(false);
        when(renewalRequestRepository.save(any(RenewalRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RenewalRequest req = renewalService.submitRenewal(10L, 5, null, "Xin ở tiếp kỳ 1");

        assertNotNull(req);
        assertEquals(RenewalStatus.SUBMITTED, req.getStatus());
        assertEquals(LocalDate.of(2026, 11, 30), req.getRequestedEnd());
        assertEquals("Xin ở tiếp kỳ 1", req.getAdminNote());
        assertEquals(ContractStatus.PENDING_RENEWAL, contract.getStatus());

        verify(contractRepository).save(contract);
        verify(renewalRequestRepository).save(req);
    }

    @Test
    @DisplayName("Nộp gia hạn thành công với requestedEnd cụ thể")
    void submitRenewal_withExplicitRequestedEnd_success() {
        LocalDate requestedEnd = LocalDate.of(2026, 12, 31);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), any()))
                .thenReturn(List.of(contract));
        when(renewalRequestRepository.existsByStudentIdAndStatus(10L, RenewalStatus.SUBMITTED))
                .thenReturn(false);
        when(renewalRequestRepository.save(any(RenewalRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RenewalRequest req = renewalService.submitRenewal(10L, null, requestedEnd, "Xin ở tới cuối năm");

        assertNotNull(req);
        assertEquals(RenewalStatus.SUBMITTED, req.getStatus());
        assertEquals(requestedEnd, req.getRequestedEnd());
        assertEquals(ContractStatus.PENDING_RENEWAL, contract.getStatus());
    }

    @Test
    @DisplayName("Nộp gia hạn thất bại khi sinh viên không có hợp đồng ACTIVE")
    void submitRenewal_noActiveContract_throwsBusinessException() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), any()))
                .thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> renewalService.submitRenewal(10L, 5, null, "Xin ở"));
        assertEquals("Bạn chưa có hợp đồng phòng ở để xin gia hạn", ex.getMessage());
    }

    @Test
    @DisplayName("Nộp gia hạn thất bại khi đang có đơn gia hạn chờ duyệt")
    void submitRenewal_alreadyHasPending_throwsBusinessException() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), any()))
                .thenReturn(List.of(contract));
        when(renewalRequestRepository.existsByStudentIdAndStatus(10L, RenewalStatus.SUBMITTED))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> renewalService.submitRenewal(10L, 5, null, "Xin ở"));
        assertEquals("Bạn đang có một yêu cầu gia hạn đang chờ xét duyệt", ex.getMessage());
    }

    @Test
    @DisplayName("Nộp gia hạn thất bại khi requestedEnd <= endDate hiện tại")
    void submitRenewal_invalidRequestedEnd_throwsBusinessException() {
        LocalDate invalidDate = LocalDate.of(2026, 6, 30);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), any()))
                .thenReturn(List.of(contract));
        when(renewalRequestRepository.existsByStudentIdAndStatus(10L, RenewalStatus.SUBMITTED))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> renewalService.submitRenewal(10L, null, invalidDate, "Lỗi ngày"));
        assertEquals("Thời hạn gia hạn mới phải sau ngày hết hạn hiện tại của hợp đồng (" + contract.getEndDate() + ")", ex.getMessage());
    }

    @Test
    @DisplayName("Duyệt gia hạn thành công: cập nhật endDate và chuyển về ACTIVE")
    void approveRenewal_success() {
        contract.setStatus(ContractStatus.PENDING_RENEWAL);

        RenewalRequest request = new RenewalRequest();
        request.setId(100L);
        request.setContract(contract);
        request.setStudent(student);
        request.setStatus(RenewalStatus.SUBMITTED);
        request.setRequestedEnd(LocalDate.of(2026, 12, 31));

        when(renewalRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(request));
        when(renewalRequestRepository.save(any(RenewalRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        renewalService.approveRenewal(100L, 99L, "Đồng ý gia hạn");

        assertEquals(RenewalStatus.APPROVED, request.getStatus());
        assertEquals("Đồng ý gia hạn", request.getAdminNote());
        assertNotNull(request.getDecidedAt());

        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertEquals(LocalDate.of(2026, 12, 31), contract.getEndDate());

        verify(contractRepository).save(contract);
        verify(renewalRequestRepository).save(request);
    }

    @Test
    @DisplayName("Duyệt gia hạn thất bại khi đơn không ở trạng thái SUBMITTED")
    void approveRenewal_notSubmitted_throwsBusinessException() {
        RenewalRequest request = new RenewalRequest();
        request.setId(100L);
        request.setStatus(RenewalStatus.APPROVED);

        when(renewalRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(request));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> renewalService.approveRenewal(100L, 99L, "Note"));
        assertEquals("Chỉ có thể duyệt đơn đang ở trạng thái SUBMITTED", ex.getMessage());
    }

    @Test
    @DisplayName("Từ chối gia hạn: hợp đồng về ACTIVE giữ nguyên endDate cũ")
    void rejectRenewal_success() {
        contract.setStatus(ContractStatus.PENDING_RENEWAL);
        LocalDate originalEndDate = contract.getEndDate();

        RenewalRequest request = new RenewalRequest();
        request.setId(100L);
        request.setContract(contract);
        request.setStudent(student);
        request.setStatus(RenewalStatus.SUBMITTED);
        request.setRequestedEnd(LocalDate.of(2026, 12, 31));

        when(renewalRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(request));
        when(renewalRequestRepository.save(any(RenewalRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        renewalService.rejectRenewal(100L, 99L, "Hết chỗ kỳ sau");

        assertEquals(RenewalStatus.REJECTED, request.getStatus());
        assertEquals("Hết chỗ kỳ sau", request.getAdminNote());
        assertNotNull(request.getDecidedAt());

        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertEquals(originalEndDate, contract.getEndDate());

        verify(contractRepository).save(contract);
        verify(renewalRequestRepository).save(request);
    }

    @Test
    @DisplayName("Sinh viên hủy đơn gia hạn: hợp đồng về ACTIVE")
    void cancelRenewal_success() {
        contract.setStatus(ContractStatus.PENDING_RENEWAL);

        RenewalRequest request = new RenewalRequest();
        request.setId(100L);
        request.setContract(contract);
        request.setStudent(student);
        request.setStatus(RenewalStatus.SUBMITTED);

        when(renewalRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(request));
        when(renewalRequestRepository.save(any(RenewalRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        renewalService.cancelRenewal(100L, 10L);

        assertEquals(RenewalStatus.CANCELLED, request.getStatus());
        assertEquals(ContractStatus.ACTIVE, contract.getStatus());

        verify(contractRepository).save(contract);
        verify(renewalRequestRepository).save(request);
    }

    @Test
    @DisplayName("Sinh viên khác không được hủy đơn gia hạn")
    void cancelRenewal_otherStudent_throwsBusinessException() {
        RenewalRequest request = new RenewalRequest();
        request.setId(100L);
        request.setContract(contract);
        request.setStudent(student); // id = 10L
        request.setStatus(RenewalStatus.SUBMITTED);

        when(renewalRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(request));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> renewalService.cancelRenewal(100L, 999L));
        assertEquals("Bạn không có quyền thao tác trên đơn gia hạn này", ex.getMessage());
    }

    @Test
    @DisplayName("Job timeout: Hợp đồng PENDING_RENEWAL quá hạn -> EXPIRED, đơn SUBMITTED -> CANCELLED, giường vẫn OCCUPIED")
    void processExpiredContractsAndRenewals_pendingRenewalTimeout() {
        contract.setStatus(ContractStatus.PENDING_RENEWAL);
        contract.setEndDate(LocalDate.now().minusDays(1));

        RenewalRequest request = new RenewalRequest();
        request.setId(100L);
        request.setContract(contract);
        request.setStudent(student);
        request.setStatus(RenewalStatus.SUBMITTED);

        LocalDate today = LocalDate.now();

        when(contractRepository.findByStatusAndEndDateBefore(ContractStatus.PENDING_RENEWAL, today))
                .thenReturn(List.of(contract));
        when(renewalRequestRepository.findByContractIdAndStatus(20L, RenewalStatus.SUBMITTED))
                .thenReturn(List.of(request));
        when(contractRepository.findByStatusAndEndDateBefore(ContractStatus.ACTIVE, today))
                .thenReturn(List.of());
        when(contractRepository.findByStatusInAndEndDateLessThanEqual(any(), any()))
                .thenReturn(List.of());

        int count = renewalService.processExpiredContractsAndRenewals(today);

        assertEquals(1, count);
        assertEquals(ContractStatus.EXPIRED, contract.getStatus());
        assertEquals(BedStatus.OCCUPIED, bed.getStatus()); // Invariant: bed remains OCCUPIED until explicit checkout!
        assertEquals(RenewalStatus.CANCELLED, request.getStatus());
        assertEquals("EXPIRED", request.getAdminNote());

        verify(contractRepository).save(contract);
        verify(renewalRequestRepository).save(request);
    }
}
