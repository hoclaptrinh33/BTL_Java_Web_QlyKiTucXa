package com.ktx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Bed;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UtilityReadingRepository;
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

    @Mock
    private UtilityReadingRepository utilityReadingRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private BillingEngine billingEngine;

    @BeforeEach
    void setUp() {
        billingEngine = new BillingEngineImpl(
                contractRepository,
                invoiceRepository,
                invoiceItemRepository,
                documentNumberService,
                utilityReadingRepository,
                systemConfigRepository
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

    private Contract createContractForStudent(Long contractId, Long studentId, String studentCode, Room room) {
        Student student = new Student();
        student.setId(studentId);
        student.setStudentCode(studentCode);
        student.setFullName("Sinh Vien " + studentId);

        Bed bed = new Bed();
        bed.setId(100L + studentId);
        bed.setBedCode("G" + studentId);
        bed.setRoom(room);

        Contract c = new Contract();
        c.setId(contractId);
        c.setContractNo("HD-2026-" + String.format("%06d", contractId));
        c.setStudent(student);
        c.setBed(bed);
        c.setStartDate(LocalDate.of(2026, 9, 1));
        c.setEndDate(LocalDate.of(2027, 1, 31));
        return c;
    }

    // ==========================================
    // TIERED ELECTRICITY TESTS
    // ==========================================

    @Test
    @DisplayName("tieredElectricity case A oracle: 280 kWh -> 679.540 VND")
    void testTieredElectricity_oracle280kWh() {
        long cost = billingEngine.tieredElectricity(280);
        assertEquals(679540L, cost);
    }

    @Test
    @DisplayName("tieredElectricity case B oracle: 51 kWh -> 101.250 VND")
    void testTieredElectricity_oracle51kWh() {
        long cost = billingEngine.tieredElectricity(51);
        assertEquals(101250L, cost);
    }

    @Test
    @DisplayName("tieredElectricity: 0 kWh hoặc số âm trả về 0 VND")
    void testTieredElectricity_zeroOrNegative() {
        assertEquals(0L, billingEngine.tieredElectricity(0));
        assertEquals(0L, billingEngine.tieredElectricity(-15));
    }

    @Test
    @DisplayName("tieredElectricity: các mốc biên 50 kWh và 100 kWh")
    void testTieredElectricity_boundary() {
        // 50 kWh: 50 * 1984 = 99.200
        assertEquals(99200L, billingEngine.tieredElectricity(50));
        // 100 kWh: 99.200 + 50 * 2050 = 201.700
        assertEquals(201700L, billingEngine.tieredElectricity(100));
    }

    // ==========================================
    // ISSUE UTILITY INVOICES: CASE A & CASE B
    // ==========================================

    @Test
    @DisplayName("Case A — chia hết (§6.5.2): 280 kWh, 18 m3, N=5 -> Mỗi SV subtotal 249.908 đ")
    void testIssueUtilityInvoices_caseA_evenSplit280kWh() {
        Long roomId = 101L;
        YearMonth month = YearMonth.of(2026, 9);
        LocalDate billingMonth = month.atDay(1);

        Room room = new Room();
        room.setId(roomId);
        room.setRoomNumber("101");

        // Utility reading: 280 kWh, 18 m3 (không thay công tơ)
        UtilityReading reading = new UtilityReading();
        reading.setId(1L);
        reading.setRoom(room);
        reading.setBillingMonth(billingMonth);
        reading.setElecPrev(1000);
        reading.setElecCurr(1280); // 280 kWh
        reading.setElecReplaced(false);
        reading.setWaterPrev(100);
        reading.setWaterCurr(118); // 18 m3
        reading.setWaterReplaced(false);
        reading.setNewBuildingMeter(false);

        when(utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, billingMonth))
                .thenReturn(Optional.of(reading));

        // 5 sinh viên (N=5)
        List<Contract> contracts = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            contracts.add(createContractForStudent(i, i * 10, "SV" + i, room));
        }
        when(contractRepository.findOccupyingByRoomId(roomId, OccupyingStatuses.OCCUPYING))
                .thenReturn(contracts);

        when(invoiceRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(documentNumberService.nextInvoiceNo(2026)).thenReturn("INV-2026-000001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Invoice> invoices = billingEngine.issueUtilityInvoices(roomId, month);

        assertNotNull(invoices);
        assertEquals(5, invoices.size());

        // Kiểm tra từng sinh viên: subtotal = 249.908 đ
        for (Invoice inv : invoices) {
            assertEquals(new BigDecimal("249908"), inv.getSubtotal());
            assertEquals(new BigDecimal("249908"), inv.getTotal());
            assertEquals(InvoiceType.UTILITY, inv.getInvoiceType());
            assertEquals(InvoiceStatus.UNPAID, inv.getStatus());
        }

        // Tổng tiền 5 SV = 1.249.540 đ
        long totalSum = invoices.stream()
                .mapToLong(inv -> inv.getTotal().longValue())
                .sum();
        assertEquals(1249540L, totalSum);

        // Kiểm tra lưu đầy đủ 5 InvoiceItem cho mỗi hóa đơn (ELEC, WATER, INTERNET, SANITATION, PARKING)
        // 5 hóa đơn * 5 items = 25 items
        verify(invoiceItemRepository, times(25)).save(any(InvoiceItem.class));
    }

    @Test
    @DisplayName("Case B — dư đồng (§6.5.2): 51 kWh, 1 m3, N=3, SV 10 nhận residual 2đ -> khớp từng dòng")
    void testIssueUtilityInvoices_caseB_residualSplit() {
        Long roomId = 201L;
        YearMonth month = YearMonth.of(2026, 9);
        LocalDate billingMonth = month.atDay(1);

        Room room = new Room();
        room.setId(roomId);
        room.setRoomNumber("201");

        // Utility reading: 51 kWh, 1 m3
        UtilityReading reading = new UtilityReading();
        reading.setId(2L);
        reading.setRoom(room);
        reading.setBillingMonth(billingMonth);
        reading.setElecPrev(500);
        reading.setElecCurr(551); // 51 kWh
        reading.setElecReplaced(false);
        reading.setWaterPrev(50);
        reading.setWaterCurr(51); // 1 m3
        reading.setWaterReplaced(false);
        reading.setNewBuildingMeter(false);

        when(utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, billingMonth))
                .thenReturn(Optional.of(reading));

        // 3 sinh viên: student_id = 10, 20, 30
        List<Contract> contracts = List.of(
                createContractForStudent(1L, 10L, "SV010", room),
                createContractForStudent(2L, 20L, "SV020", room),
                createContractForStudent(3L, 30L, "SV030", room)
        );
        when(contractRepository.findOccupyingByRoomId(roomId, OccupyingStatuses.OCCUPYING))
                .thenReturn(contracts);

        when(invoiceRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(documentNumberService.nextInvoiceNo(2026)).thenReturn("INV-2026-000002");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Invoice> invoices = billingEngine.issueUtilityInvoices(roomId, month);

        assertEquals(3, invoices.size());

        // SV 10 (minId) nhận residual 2đ -> subtotal = 105.418 đ
        Invoice inv10 = invoices.stream()
                .filter(i -> i.getStudent().getId().equals(10L))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("105418"), inv10.getSubtotal());
        assertEquals(new BigDecimal("105418"), inv10.getTotal());

        // SV 20 và 30: subtotal = 105.416 đ
        Invoice inv20 = invoices.stream()
                .filter(i -> i.getStudent().getId().equals(20L))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("105416"), inv20.getSubtotal());

        Invoice inv30 = invoices.stream()
                .filter(i -> i.getStudent().getId().equals(30L))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("105416"), inv30.getSubtotal());

        // Tổng subtotal cả 3 SV = 316.250 đ = 166.250 (divisible) + 3*50.000
        long totalSum = invoices.stream()
                .mapToLong(i -> i.getTotal().longValue())
                .sum();
        assertEquals(316250L, totalSum);

        // Kiểm tra chi tiết dòng InvoiceItem
        ArgumentCaptor<InvoiceItem> itemCaptor = ArgumentCaptor.forClass(InvoiceItem.class);
        verify(invoiceItemRepository, times(15)).save(itemCaptor.capture());
        List<InvoiceItem> savedItems = itemCaptor.getAllValues();

        // Kiểm tra các dòng của SV 10
        List<InvoiceItem> items10 = savedItems.stream()
                .filter(it -> it.getInvoice().getStudent().getId().equals(10L))
                .toList();
        assertEquals(5, items10.size());

        InvoiceItem elec10 = items10.stream().filter(it -> "ELEC".equals(it.getItemCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("33750"), elec10.getAmount());

        InvoiceItem water10 = items10.stream().filter(it -> "WATER".equals(it.getItemCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("5000"), water10.getAmount());

        InvoiceItem inet10 = items10.stream().filter(it -> "INTERNET".equals(it.getItemCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("16668"), inet10.getAmount());

        InvoiceItem san10 = items10.stream().filter(it -> "SANITATION".equals(it.getItemCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("20000"), san10.getAmount());

        InvoiceItem park10 = items10.stream().filter(it -> "PARKING".equals(it.getItemCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("30000"), park10.getAmount());

        // sum items SV 10 = 105.418 đ
        long sum10 = items10.stream().mapToLong(it -> it.getAmount().longValue()).sum();
        assertEquals(105418L, sum10);
    }

    @Test
    @DisplayName("issueUtilityInvoices: N=0 (phòng không có người ở) trả về danh sách rỗng")
    void testIssueUtilityInvoices_emptyRoom() {
        Long roomId = 301L;
        YearMonth month = YearMonth.of(2026, 9);
        LocalDate billingMonth = month.atDay(1);

        Room room = new Room();
        room.setId(roomId);

        UtilityReading reading = new UtilityReading();
        reading.setRoom(room);
        reading.setBillingMonth(billingMonth);
        reading.setElecPrev(100);
        reading.setElecCurr(200);
        reading.setElecReplaced(false);
        reading.setWaterPrev(10);
        reading.setWaterCurr(15);
        reading.setWaterReplaced(false);
        reading.setNewBuildingMeter(false);

        when(utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, billingMonth))
                .thenReturn(Optional.of(reading));
        when(contractRepository.findOccupyingByRoomId(roomId, OccupyingStatuses.OCCUPYING))
                .thenReturn(Collections.emptyList());

        List<Invoice> invoices = billingEngine.issueUtilityInvoices(roomId, month);
        assertTrue(invoices.isEmpty());
    }

    @Test
    @DisplayName("issueUtilityInvoices: Idempotent khi đã có hóa đơn cùng idempotencyKey chưa hủy")
    void testIssueUtilityInvoices_idempotent() {
        Long roomId = 101L;
        YearMonth month = YearMonth.of(2026, 9);
        LocalDate billingMonth = month.atDay(1);

        Room room = new Room();
        room.setId(roomId);

        UtilityReading reading = new UtilityReading();
        reading.setRoom(room);
        reading.setBillingMonth(billingMonth);
        reading.setElecPrev(100);
        reading.setElecCurr(200);
        reading.setElecReplaced(false);
        reading.setWaterPrev(10);
        reading.setWaterCurr(15);
        reading.setWaterReplaced(false);
        reading.setNewBuildingMeter(false);

        when(utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, billingMonth))
                .thenReturn(Optional.of(reading));

        Contract contract = createContractForStudent(1L, 10L, "SV010", room);
        when(contractRepository.findOccupyingByRoomId(roomId, OccupyingStatuses.OCCUPYING))
                .thenReturn(List.of(contract));

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(999L);
        existingInvoice.setStatus(InvoiceStatus.UNPAID);
        existingInvoice.setIdempotencyKey("UTILITY:10:101:2026-09");

        when(invoiceRepository.findByIdempotencyKey("UTILITY:10:101:2026-09"))
                .thenReturn(Optional.of(existingInvoice));

        List<Invoice> invoices = billingEngine.issueUtilityInvoices(roomId, month);

        assertEquals(1, invoices.size());
        assertEquals(999L, invoices.get(0).getId());
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(invoiceItemRepository, never()).save(any(InvoiceItem.class));
    }

    @Test
    @DisplayName("issueUtilityInvoices ném lỗi khi chưa có chỉ số điện nước tháng đó")
    void testIssueUtilityInvoices_missingReading() {
        Long roomId = 999L;
        YearMonth month = YearMonth.of(2026, 9);

        when(utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, month.atDay(1)))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> billingEngine.issueUtilityInvoices(roomId, month));
    }

    // ==========================================
    // EXISTING ROOM FEE & DEPOSIT TESTS
    // ==========================================

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
