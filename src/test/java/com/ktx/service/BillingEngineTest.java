package com.ktx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.ktx.domain.Bed;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.service.impl.BillingEngineImpl;

@ExtendWith(MockitoExtension.class)
class BillingEngineTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private DocumentNumberService documentNumberService;

    private BillingEngine billingEngine;

    @BeforeEach
    void setUp() {
        billingEngine = new BillingEngineImpl(
                contractRepository,
                invoiceRepository,
                invoiceItemRepository,
                documentNumberService
        );
    }

    private Contract createSampleContract() {
        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");

        Bed bed = new Bed();
        bed.setId(100L);
        bed.setBedCode("G1");
        bed.setRoom(room);

        Student student = new Student();
        student.setId(5L);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setContractNo("HD-2026-000001");
        contract.setStudent(student);
        contract.setBed(bed);
        contract.setRoomFee(new BigDecimal("1500000"));
        contract.setDepositAmount(new BigDecimal("750000")); // 50%
        contract.setStartDate(LocalDate.of(2026, 9, 1));
        contract.setEndDate(LocalDate.of(2027, 1, 31));
        return contract;
    }

    @Test
    @DisplayName("issueDeposit tạo hóa đơn DEPOSIT đúng 50% giá kỳ và lưu InvoiceItem")
    void issueDeposit_success() {
        Contract contract = createSampleContract();
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findByIdempotencyKey("INV:DEPOSIT:1")).thenReturn(Optional.empty());
        when(documentNumberService.nextInvoiceNo(2026)).thenReturn("INV-2026-000001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(200L);
            return i;
        });

        Invoice result = billingEngine.issueDeposit(1L);

        assertNotNull(result);
        assertEquals("INV-2026-000001", result.getInvoiceNo());
        assertEquals(InvoiceType.DEPOSIT, result.getInvoiceType());
        assertEquals(InvoiceStatus.UNPAID, result.getStatus());
        assertEquals(new BigDecimal("750000"), result.getTotal());
        assertEquals("INV:DEPOSIT:1", result.getIdempotencyKey());

        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceItemRepository).save(any(InvoiceItem.class));
    }

    @Test
    @DisplayName("issueDeposit trả về hóa đơn đã tồn tại theo idempotencyKey nếu gọi lần 2")
    void issueDeposit_idempotent() {
        Contract contract = createSampleContract();
        Invoice existing = new Invoice();
        existing.setId(200L);
        existing.setInvoiceNo("INV-2026-000001");
        existing.setTotal(new BigDecimal("750000"));

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findByIdempotencyKey("INV:DEPOSIT:1")).thenReturn(Optional.of(existing));

        Invoice result = billingEngine.issueDeposit(1L);

        assertNotNull(result);
        assertEquals(200L, result.getId());
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(invoiceItemRepository, never()).save(any(InvoiceItem.class));
    }

    @Test
    @DisplayName("issueRoomFee tạo hóa đơn ROOM_TERM đủ 100% tiền phòng kỳ")
    void issueRoomFee_success() {
        Contract contract = createSampleContract();
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findByIdempotencyKey("INV:ROOM_TERM:1")).thenReturn(Optional.empty());
        when(documentNumberService.nextInvoiceNo(2026)).thenReturn("INV-2026-000002");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = billingEngine.issueRoomFee(1L);

        assertNotNull(result);
        assertEquals("INV-2026-000002", result.getInvoiceNo());
        assertEquals(InvoiceType.ROOM_TERM, result.getInvoiceType());
        assertEquals(InvoiceStatus.UNPAID, result.getStatus());
        assertEquals(new BigDecimal("1500000"), result.getTotal());

        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceItemRepository).save(any(InvoiceItem.class));
    }
}
