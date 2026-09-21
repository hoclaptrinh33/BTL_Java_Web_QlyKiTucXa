package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.service.impl.ExportServiceImpl;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportServiceImpl(contractRepository, invoiceRepository, invoiceItemRepository);
    }

    @Test
    void testExportResidentsXlsx() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);

        Room r = new Room();
        r.setId(1L);
        r.setBuilding(b);
        r.setRoomNumber("A-101");
        r.setRoomType(RoomType.STANDARD_4);
        r.setStatus(RoomStatus.ACTIVE);

        Bed bed = new Bed();
        bed.setId(1L);
        bed.setRoom(r);
        bed.setBedCode("G1");

        Student s = new Student();
        s.setId(1L);
        s.setStudentCode("D22CQCN001");
        s.setFullName("Nguyễn Văn A");
        s.setGender(Gender.MALE);

        Contract c = new Contract();
        c.setId(1L);
        c.setContractNo("HD-2026-000001");
        c.setStudent(s);
        c.setBed(bed);
        c.setStartDate(LocalDate.of(2026, 9, 1));
        c.setEndDate(LocalDate.of(2027, 1, 31));
        c.setStatus(ContractStatus.ACTIVE);

        when(contractRepository.findOccupyingWithDetails(any())).thenReturn(List.of(c));

        byte[] xlsxBytes = exportService.exportResidentsXlsx();
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            XSSFSheet sheet = wb.getSheet("Danh sách nội trú");
            assertNotNull(sheet);
            assertEquals("DANH SÁCH SINH VIÊN ĐANG NỘI TRÚ KÝ TÚC XÁ", sheet.getRow(0).getCell(0).getStringCellValue());
            // Header is row 3
            assertEquals("MSSV", sheet.getRow(3).getCell(1).getStringCellValue());
            // Data row 4
            assertEquals("D22CQCN001", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("Nguyễn Văn A", sheet.getRow(4).getCell(2).getStringCellValue());
            assertEquals("A-101", sheet.getRow(4).getCell(5).getStringCellValue());
            assertEquals("G1", sheet.getRow(4).getCell(6).getStringCellValue());
            assertEquals("HD-2026-000001", sheet.getRow(4).getCell(7).getStringCellValue());
        }
    }

    @Test
    void testExportDebtsXlsx() throws Exception {
        Student s = new Student();
        s.setId(2L);
        s.setStudentCode("D22CQCN002");
        s.setFullName("Trần Văn B");

        Invoice inv = new Invoice();
        inv.setId(10L);
        inv.setInvoiceNo("INV-2026-000010");
        inv.setStudent(s);
        inv.setSubtotal(new BigDecimal("1800000"));
        inv.setLateFee(new BigDecimal("90000"));
        inv.setTotal(new BigDecimal("1890000"));
        inv.setDueDate(LocalDate.of(2026, 9, 10));
        inv.setStatus(InvoiceStatus.OVERDUE);
        inv.setBillingMonth(LocalDate.of(2026, 9, 1));

        when(invoiceRepository.findByStatusInOrderByDueDateAsc(any())).thenReturn(List.of(inv));

        byte[] xlsxBytes = exportService.exportDebtsXlsx();
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            XSSFSheet sheet = wb.getSheet("Công nợ hóa đơn");
            assertNotNull(sheet);
            assertEquals("INV-2026-000010", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("D22CQCN002", sheet.getRow(4).getCell(2).getStringCellValue());
            assertEquals(1800000.0, sheet.getRow(4).getCell(7).getNumericCellValue());
            assertEquals(90000.0, sheet.getRow(4).getCell(8).getNumericCellValue());
            assertEquals(1890000.0, sheet.getRow(4).getCell(9).getNumericCellValue());
        }
    }

    @Test
    void testExportDebtsPdf() {
        Student s = new Student();
        s.setStudentCode("D22CQCN002");
        s.setFullName("Trần Văn B");

        Invoice inv = new Invoice();
        inv.setId(10L);
        inv.setInvoiceNo("INV-2026-000010");
        inv.setStudent(s);
        inv.setTotal(new BigDecimal("1890000"));
        inv.setDueDate(LocalDate.of(2026, 9, 10));
        inv.setStatus(InvoiceStatus.OVERDUE);

        when(invoiceRepository.findByStatusInOrderByDueDateAsc(any())).thenReturn(List.of(inv));

        byte[] pdfBytes = exportService.exportDebtsPdf();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    void testExportInvoicePdf() {
        Student s = new Student();
        s.setStudentCode("D22CQCN001");
        s.setFullName("Nguyễn Văn A");

        Invoice inv = new Invoice();
        inv.setId(5L);
        inv.setInvoiceNo("INV-2026-000005");
        inv.setStudent(s);
        inv.setInvoiceType(InvoiceType.UTILITY);
        inv.setBillingMonth(LocalDate.of(2026, 9, 1));
        inv.setDueDate(LocalDate.of(2026, 9, 15));
        inv.setStatus(InvoiceStatus.UNPAID);
        inv.setSubtotal(new BigDecimal("250000"));
        inv.setLateFee(BigDecimal.ZERO);
        inv.setTotal(new BigDecimal("250000"));

        InvoiceItem item = new InvoiceItem();
        item.setId(1L);
        item.setDescription("Tiền điện tháng 09/2026");
        item.setQty(new BigDecimal("50"));
        item.setUnitPrice(new BigDecimal("3000"));
        item.setAmount(new BigDecimal("150000"));

        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(inv));
        when(invoiceItemRepository.findByInvoiceIdOrderByIdAsc(5L)).thenReturn(List.of(item));

        byte[] pdfBytes = exportService.exportInvoicePdf(5L);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    void testExportInvoicePdf_NotFound() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> exportService.exportInvoicePdf(999L));
    }
}
