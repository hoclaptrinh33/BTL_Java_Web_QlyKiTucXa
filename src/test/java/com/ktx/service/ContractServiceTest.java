package com.ktx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.CompletionReason;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.repository.BedRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.service.impl.ContractServiceImpl;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private DocumentNumberService documentNumberService;

    @Mock
    private SystemConfigService systemConfigService;

    private ContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new ContractServiceImpl(contractRepository, bedRepository, documentNumberService, systemConfigService);
    }

    @Test
    @DisplayName("createDraftFromAllocation tạo HĐ DRAFT, cọc 50% HALF_UP và chiếm dụng giường OCCUPIED")
    void createDraftFromAllocation_success() {
        Student student = new Student();
        student.setId(10L);

        Room room = new Room();
        room.setId(5L);
        room.setPricePerTerm(new BigDecimal("1500000"));

        Bed bed = new Bed();
        bed.setId(50L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.VACANT);
        bed.setRoom(room);

        RoomApplication app = new RoomApplication();
        app.setId(100L);
        app.setStudent(student);

        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2027, 1, 31);

        when(documentNumberService.nextContractNo(2026)).thenReturn("HD-2026-000001");
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(bedRepository.occupyBed(50L, 1L)).thenReturn(1);

        Contract contract = contractService.createDraftFromAllocation(app, bed, start, end);

        assertNotNull(contract);
        assertEquals("HD-2026-000001", contract.getContractNo());
        assertEquals(student, contract.getStudent());
        assertEquals(bed, contract.getBed());
        assertEquals(app, contract.getApplication());
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertEquals(DepositStatus.HELD, contract.getDepositStatus());
        assertEquals(new BigDecimal("1500000"), contract.getRoomFee());
        // 50% của 1.500.000 = 750.000
        assertEquals(new BigDecimal("750000"), contract.getDepositAmount());

        verify(bedRepository).occupyBed(50L, 1L);
    }

    @Test
    @DisplayName("createDraftFromAllocation ném ngoại lệ nếu không khóa được giường (occupyBed != 1)")
    void createDraftFromAllocation_bedNotVacant() {
        Student student = new Student();
        student.setId(10L);

        Room room = new Room();
        room.setPricePerTerm(new BigDecimal("1200000"));

        Bed bed = new Bed();
        bed.setId(50L);
        bed.setBedCode("G1");
        bed.setRoom(room);

        RoomApplication app = new RoomApplication();
        app.setId(100L);
        app.setStudent(student);

        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2027, 1, 31);

        when(documentNumberService.nextContractNo(2026)).thenReturn("HD-2026-000001");
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });
        when(bedRepository.occupyBed(50L, 2L)).thenReturn(0); // Không update được dòng nào

        assertThrows(BusinessException.class,
                () -> contractService.createDraftFromAllocation(app, bed, start, end));
    }

    @Test
    @DisplayName("cancelDraft chuyển trạng thái sang COMPLETED, lý do CANCELLED_BEFORE_CHECKIN và nhả giường")
    void cancelDraft_success() {
        Bed bed = new Bed();
        bed.setId(50L);
        bed.setStatus(BedStatus.OCCUPIED);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setBed(bed);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(bedRepository.vacateBed(50L)).thenReturn(1);

        contractService.cancelDraft(10L);

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertEquals(CompletionReason.CANCELLED_BEFORE_CHECKIN, contract.getCompletionReason());
        verify(contractRepository).save(contract);
        verify(bedRepository).vacateBed(50L);
    }

    @Test
    @DisplayName("cancelDraft ném ngoại lệ nếu vacateBed != 1")
    void cancelDraft_vacateFailed() {
        Bed bed = new Bed();
        bed.setId(50L);
        bed.setStatus(BedStatus.OCCUPIED);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setBed(bed);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(bedRepository.vacateBed(50L)).thenReturn(0);

        assertThrows(BusinessException.class, () -> contractService.cancelDraft(10L));
    }

    @Test
    @DisplayName("cancelDraft ném ngoại lệ nếu hợp đồng không phải DRAFT")
    void cancelDraft_notDraft() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.ACTIVE);

        assertThrows(BusinessException.class, () -> contractService.cancelDraft(10L));
        verify(bedRepository, never()).vacateBed(any(Long.class));
    }

    @Test
    @DisplayName("terminate chuyển ACTIVE sang TERMINATED, cọc FORFEITED và giường VẪN OCCUPIED")
    void terminate_success() {
        Bed bed = new Bed();
        bed.setId(50L);
        bed.setStatus(BedStatus.OCCUPIED);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setDepositStatus(DepositStatus.HELD);
        contract.setBed(bed);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        contractService.terminate(10L, true);

        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertEquals(DepositStatus.FORFEITED, contract.getDepositStatus());
        verify(contractRepository).save(contract);
        // Giường VẪN OCCUPIED, không được nhả giường khi terminate (§04-04)
        verify(bedRepository, never()).vacateBed(any(Long.class));
    }

    @Test
    @DisplayName("terminate ném ngoại lệ nếu hợp đồng không phải ACTIVE")
    void terminate_notActive() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.DRAFT);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        assertThrows(BusinessException.class, () -> contractService.terminate(10L, false));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("createDraft tạo HĐ DRAFT cho sinh viên khi application = null (gán tay)")
    void createDraft_withoutApplication_success() {
        Student student = new Student();
        student.setId(15L);

        Room room = new Room();
        room.setPricePerTerm(new BigDecimal("2000000"));

        Bed bed = new Bed();
        bed.setId(60L);
        bed.setBedCode("B03");
        bed.setRoom(room);

        LocalDate termStart = LocalDate.of(2026, 9, 1);
        LocalDate termEnd = LocalDate.of(2027, 1, 31);

        when(documentNumberService.nextContractNo(2026)).thenReturn("HD-2026-9999");
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> {
            Contract c = i.getArgument(0);
            c.setId(99L);
            return c;
        });
        when(bedRepository.occupyBed(60L, 99L)).thenReturn(1);

        Contract contract = contractService.createDraft(student, null, bed, termStart, termEnd);

        assertNotNull(contract);
        assertEquals("HD-2026-9999", contract.getContractNo());
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertEquals(new BigDecimal("1000000"), contract.getDepositAmount());
        assertEquals(student, contract.getStudent());
        assertEquals(bed, contract.getBed());
        verify(bedRepository).occupyBed(60L, 99L);
    }
}
