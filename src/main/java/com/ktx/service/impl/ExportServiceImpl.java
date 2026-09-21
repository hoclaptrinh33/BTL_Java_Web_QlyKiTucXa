package com.ktx.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.service.ExportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    public ExportServiceImpl(ContractRepository contractRepository,
                             InvoiceRepository invoiceRepository,
                             InvoiceItemRepository invoiceItemRepository) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    @Override
    public byte[] exportResidentsXlsx() {
        List<Contract> contracts = new java.util.ArrayList<>(contractRepository.findOccupyingWithDetails(OccupyingStatuses.OCCUPYING));
        contracts.sort(Comparator
                .comparing((Contract c) -> c.getBed().getRoom().getBuilding().getCode(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(c -> c.getBed().getRoom().getRoomNumber(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(c -> c.getBed().getBedCode(), String.CASE_INSENSITIVE_ORDER));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Danh sách nội trú");
            sheet.setDisplayGridlines(true);

            // Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH SINH VIÊN ĐANG NỘI TRÚ KÝ TÚC XÁ");
            CellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(new byte[]{(byte) 109, (byte) 94, (byte) 245}, null));
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(1);
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_TIME_FORMAT) + " | Tổng số: " + contracts.size() + " sinh viên");
            CellStyle metaStyle = workbook.createCellStyle();
            XSSFFont metaFont = workbook.createFont();
            metaFont.setItalic(true);
            metaFont.setFontHeightInPoints((short) 10);
            metaStyle.setFont(metaFont);
            metaCell.setCellStyle(metaStyle);

            // Header Row
            String[] headers = {
                    "STT", "MSSV", "Họ và tên", "Giới tính", "Tòa",
                    "Phòng", "Giường", "Số hợp đồng", "Ngày bắt đầu", "Hạn hợp đồng"
            };

            Row headerRow = sheet.createRow(3);
            CellStyle headerStyle = createHeaderStyle(workbook, new byte[]{(byte) 109, (byte) 94, (byte) 245});

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle borderStyle = createBorderedStyle(workbook);
            CellStyle centerStyle = createBorderedStyle(workbook);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            int rowIdx = 4;
            for (int i = 0; i < contracts.size(); i++) {
                Contract c = contracts.get(i);
                Student s = c.getStudent();
                Room r = c.getBed().getRoom();
                Building b = r.getBuilding();

                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(i + 1);
                c0.setCellStyle(centerStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(s != null ? s.getStudentCode() : "");
                c1.setCellStyle(centerStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(s != null ? s.getFullName() : "");
                c2.setCellStyle(borderStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(s != null && s.getGender() != null ? (s.getGender() == Gender.MALE ? "Nam" : "Nữ") : "");
                c3.setCellStyle(centerStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(b != null ? b.getName() : "");
                c4.setCellStyle(centerStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(r != null ? r.getRoomNumber() : "");
                c5.setCellStyle(centerStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(c.getBed() != null ? c.getBed().getBedCode() : "");
                c6.setCellStyle(centerStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(c.getContractNo() != null ? c.getContractNo() : "");
                c7.setCellStyle(centerStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(c.getStartDate() != null ? c.getStartDate().format(DATE_FORMAT) : "");
                c8.setCellStyle(centerStyle);

                Cell c9 = row.createCell(9);
                c9.setCellValue(c.getEndDate() != null ? c.getEndDate().format(DATE_FORMAT) : "");
                c9.setCellStyle(centerStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3000));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel danh sách nội trú: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportDebtsXlsx() {
        List<Invoice> invoices = invoiceRepository.findByStatusInOrderByDueDateAsc(
                List.of(InvoiceStatus.OVERDUE, InvoiceStatus.UNPAID));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Công nợ hóa đơn");
            sheet.setDisplayGridlines(true);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO CÔNG NỢ HÓA ĐƠN KÝ TÚC XÁ");
            CellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(new byte[]{(byte) 225, (byte) 29, (byte) 72}, null)); // rose
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(1);
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_TIME_FORMAT) + " | Hóa đơn chưa thanh toán & quá hạn");
            CellStyle metaStyle = workbook.createCellStyle();
            XSSFFont metaFont = workbook.createFont();
            metaFont.setItalic(true);
            metaFont.setFontHeightInPoints((short) 10);
            metaStyle.setFont(metaFont);
            metaCell.setCellStyle(metaStyle);

            String[] headers = {
                    "STT", "Mã hóa đơn", "MSSV", "Họ và tên", "Phòng", "Tòa",
                    "Kỳ / Tháng", "Tiền gốc (VNĐ)", "Phạt quá hạn (VNĐ)", "Tổng nợ (VNĐ)", "Hạn nộp", "Trạng thái"
            };

            Row headerRow = sheet.createRow(3);
            CellStyle headerStyle = createHeaderStyle(workbook, new byte[]{(byte) 194, (byte) 65, (byte) 12}); // amber/dark-peach

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle borderStyle = createBorderedStyle(workbook);
            CellStyle centerStyle = createBorderedStyle(workbook);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = createBorderedStyle(workbook);
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            int rowIdx = 4;
            BigDecimal sumSubtotal = BigDecimal.ZERO;
            BigDecimal sumLateFee = BigDecimal.ZERO;
            BigDecimal sumTotal = BigDecimal.ZERO;

            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                Student s = inv.getStudent();
                Room r = inv.getRoom();
                Building b = (r != null) ? r.getBuilding() : null;

                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(i + 1);
                c0.setCellStyle(centerStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(inv.getInvoiceNo());
                c1.setCellStyle(centerStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(s != null ? s.getStudentCode() : "");
                c2.setCellStyle(centerStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(s != null ? s.getFullName() : "");
                c3.setCellStyle(borderStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(r != null ? r.getRoomNumber() : "—");
                c4.setCellStyle(centerStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(b != null ? b.getName() : "—");
                c5.setCellStyle(centerStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(inv.getBillingMonth() != null ? inv.getBillingMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")) : "Học kỳ");
                c6.setCellStyle(centerStyle);

                Cell c7 = row.createCell(7);
                BigDecimal sub = inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO;
                c7.setCellValue(sub.doubleValue());
                c7.setCellStyle(currencyStyle);
                sumSubtotal = sumSubtotal.add(sub);

                Cell c8 = row.createCell(8);
                BigDecimal late = inv.getLateFee() != null ? inv.getLateFee() : BigDecimal.ZERO;
                c8.setCellValue(late.doubleValue());
                c8.setCellStyle(currencyStyle);
                sumLateFee = sumLateFee.add(late);

                Cell c9 = row.createCell(9);
                BigDecimal tot = inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO;
                c9.setCellValue(tot.doubleValue());
                c9.setCellStyle(currencyStyle);
                sumTotal = sumTotal.add(tot);

                Cell c10 = row.createCell(10);
                c10.setCellValue(inv.getDueDate() != null ? inv.getDueDate().format(DATE_FORMAT) : "");
                c10.setCellStyle(centerStyle);

                Cell c11 = row.createCell(11);
                c11.setCellValue(inv.getStatus() == InvoiceStatus.OVERDUE ? "Quá hạn" : "Chưa thanh toán");
                c11.setCellStyle(centerStyle);
            }

            // Summary row
            Row sumRow = sheet.createRow(rowIdx);
            Cell sc0 = sumRow.createCell(0);
            sc0.setCellValue("Tổng cộng (" + invoices.size() + " HĐ)");
            CellStyle sumLabelStyle = createBorderedStyle(workbook);
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            sumLabelStyle.setFont(boldFont);
            sc0.setCellStyle(sumLabelStyle);

            for (int k = 1; k < 7; k++) {
                Cell ec = sumRow.createCell(k);
                ec.setCellStyle(sumLabelStyle);
            }

            CellStyle sumCurrencyStyle = createBorderedStyle(workbook);
            sumCurrencyStyle.setFont(boldFont);
            sumCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
            sumCurrencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            Cell sc7 = sumRow.createCell(7);
            sc7.setCellValue(sumSubtotal.doubleValue());
            sc7.setCellStyle(sumCurrencyStyle);

            Cell sc8 = sumRow.createCell(8);
            sc8.setCellValue(sumLateFee.doubleValue());
            sc8.setCellStyle(sumCurrencyStyle);

            Cell sc9 = sumRow.createCell(9);
            sc9.setCellValue(sumTotal.doubleValue());
            sc9.setCellStyle(sumCurrencyStyle);

            Cell sc10 = sumRow.createCell(10);
            sc10.setCellStyle(sumLabelStyle);
            Cell sc11 = sumRow.createCell(11);
            sc11.setCellStyle(sumLabelStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3000));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel công nợ: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportDebtsPdf() {
        List<Invoice> invoices = invoiceRepository.findByStatusInOrderByDueDateAsc(
                List.of(InvoiceStatus.OVERDUE, InvoiceStatus.UNPAID));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 25, 25);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = getPdfFont(14, Font.BOLD, new Color(194, 65, 12));
            Font subFont = getPdfFont(9, Font.ITALIC, Color.DARK_GRAY);
            Font headerFont = getPdfFont(9, Font.BOLD, Color.WHITE);
            Font cellFont = getPdfFont(8, Font.NORMAL, Color.BLACK);
            Font cellBold = getPdfFont(8, Font.BOLD, Color.BLACK);

            Paragraph pTitle = new Paragraph("DANH SÁCH HÓA ĐƠN CÔNG NỢ KÝ TÚC XÁ", titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitle);

            Paragraph pSub = new Paragraph("Ngày xuất: " + LocalDateTime.now().format(DATE_TIME_FORMAT)
                    + " | Tổng số hóa đơn nợ: " + invoices.size(), subFont);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingAfter(15);
            document.add(pSub);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 12f, 11f, 18f, 10f, 10f, 14f, 11f, 10f});

            String[] headers = {"STT", "Mã HĐ", "MSSV", "Họ và tên", "Phòng", "Kỳ/Tháng", "Tổng nợ (VNĐ)", "Hạn nộp", "Trạng thái"};
            Color headerBg = new Color(194, 65, 12);
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setBackgroundColor(headerBg);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                c.setPadding(5);
                table.addCell(c);
            }

            BigDecimal totalSum = BigDecimal.ZERO;
            Color altColor = new Color(248, 250, 252);

            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                Student s = inv.getStudent();
                Room r = inv.getRoom();
                Building b = (r != null) ? r.getBuilding() : null;
                BigDecimal tot = inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO;
                totalSum = totalSum.add(tot);

                Color bg = (i % 2 == 1) ? altColor : Color.WHITE;

                addCell(table, String.valueOf(i + 1), cellFont, Element.ALIGN_CENTER, bg);
                addCell(table, inv.getInvoiceNo(), cellFont, Element.ALIGN_CENTER, bg);
                addCell(table, s != null ? s.getStudentCode() : "", cellFont, Element.ALIGN_CENTER, bg);
                addCell(table, s != null ? s.getFullName() : "", cellFont, Element.ALIGN_LEFT, bg);
                addCell(table, (r != null ? r.getRoomNumber() : "") + (b != null ? " (" + b.getCode() + ")" : ""), cellFont, Element.ALIGN_CENTER, bg);
                addCell(table, inv.getBillingMonth() != null ? inv.getBillingMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")) : "Học kỳ", cellFont, Element.ALIGN_CENTER, bg);
                addCell(table, CURRENCY_FORMAT.format(tot.longValue()), cellBold, Element.ALIGN_RIGHT, bg);
                addCell(table, inv.getDueDate() != null ? inv.getDueDate().format(DATE_FORMAT) : "", cellFont, Element.ALIGN_CENTER, bg);
                addCell(table, inv.getStatus() == InvoiceStatus.OVERDUE ? "Quá hạn" : "Chưa thanh toán", cellFont, Element.ALIGN_CENTER, bg);
            }

            // Total row
            PdfPCell sumLabel = new PdfPCell(new Phrase("Tổng cộng", cellBold));
            sumLabel.setColspan(6);
            sumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sumLabel.setPadding(5);
            sumLabel.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(sumLabel);

            PdfPCell sumVal = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(totalSum.longValue()) + " đ", cellBold));
            sumVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sumVal.setPadding(5);
            sumVal.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(sumVal);

            PdfPCell empty = new PdfPCell(new Phrase("", cellFont));
            empty.setColspan(2);
            empty.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(empty);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF công nợ: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn ID: " + invoiceId));
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderByIdAsc(invoiceId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font headerTitleFont = getPdfFont(16, Font.BOLD, new Color(109, 94, 245));
            Font subTitleFont = getPdfFont(11, Font.NORMAL, Color.DARK_GRAY);
            Font labelFont = getPdfFont(9, Font.BOLD, Color.DARK_GRAY);
            Font valueFont = getPdfFont(9, Font.NORMAL, Color.BLACK);
            Font tableHeadFont = getPdfFont(9, Font.BOLD, Color.WHITE);
            Font cellFont = getPdfFont(9, Font.NORMAL, Color.BLACK);
            Font boldCellFont = getPdfFont(9, Font.BOLD, Color.BLACK);

            // Header Title
            Paragraph orgName = new Paragraph("KÝ TÚC XÁ SINH VIÊN - BAN QUẢN LÝ KTX", subTitleFont);
            orgName.setAlignment(Element.ALIGN_CENTER);
            document.add(orgName);

            Paragraph title = new Paragraph("HÓA ĐƠN DỊCH VỤ / PHIẾU THU", headerTitleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph noPara = new Paragraph("Số hóa đơn: " + invoice.getInvoiceNo()
                    + " | Ngày lập: " + LocalDateTime.now().format(DATE_FORMAT), subTitleFont);
            noPara.setAlignment(Element.ALIGN_CENTER);
            noPara.setSpacingAfter(18);
            document.add(noPara);

            // Info Table (2 columns)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50f, 50f});
            infoTable.setSpacingAfter(15);

            Student s = invoice.getStudent();
            Room r = invoice.getRoom();
            Building b = (r != null) ? r.getBuilding() : null;

            PdfPCell leftInfo = new PdfPCell();
            leftInfo.setBorder(PdfPCell.NO_BORDER);
            leftInfo.addElement(new Paragraph("Họ và tên: " + (s != null ? s.getFullName() : ""), valueFont));
            leftInfo.addElement(new Paragraph("Mã sinh viên: " + (s != null ? s.getStudentCode() : ""), valueFont));
            leftInfo.addElement(new Paragraph("Phòng: " + (r != null ? r.getRoomNumber() : "—")
                    + "  |  Tòa: " + (b != null ? b.getName() : "—"), valueFont));
            infoTable.addCell(leftInfo);

            PdfPCell rightInfo = new PdfPCell();
            rightInfo.setBorder(PdfPCell.NO_BORDER);
            rightInfo.addElement(new Paragraph("Kỳ / Tháng: " + (invoice.getBillingMonth() != null ? invoice.getBillingMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")) : "Học kỳ"), valueFont));
            rightInfo.addElement(new Paragraph("Hạn nộp: " + (invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FORMAT) : "—"), valueFont));
            rightInfo.addElement(new Paragraph("Trạng thái: " + (invoice.getStatus() == InvoiceStatus.PAID ? "ĐÃ THANH TOÁN" : (invoice.getStatus() == InvoiceStatus.OVERDUE ? "QUÁ HẠN" : "CHƯA THANH TOÁN")), labelFont));
            if (invoice.getPaidAt() != null) {
                rightInfo.addElement(new Paragraph("Ngày thanh toán: " + invoice.getPaidAt().format(DATE_TIME_FORMAT), valueFont));
            }
            infoTable.addCell(rightInfo);

            document.add(infoTable);

            // Items Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{6f, 44f, 14f, 18f, 18f});
            table.setSpacingAfter(12);

            Color headerColor = new Color(109, 94, 245);
            String[] colNames = {"STT", "Khoản mục", "Số lượng", "Đơn giá (VNĐ)", "Thành tiền (VNĐ)"};
            for (String col : colNames) {
                PdfPCell c = new PdfPCell(new Phrase(col, tableHeadFont));
                c.setBackgroundColor(headerColor);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6);
                table.addCell(c);
            }

            for (int i = 0; i < items.size(); i++) {
                InvoiceItem item = items.get(i);
                addCell(table, String.valueOf(i + 1), cellFont, Element.ALIGN_CENTER, Color.WHITE);
                addCell(table, item.getDescription(), cellFont, Element.ALIGN_LEFT, Color.WHITE);
                addCell(table, item.getQty() != null ? String.valueOf(item.getQty().stripTrailingZeros().toPlainString()) : "1", cellFont, Element.ALIGN_CENTER, Color.WHITE);
                addCell(table, item.getUnitPrice() != null ? CURRENCY_FORMAT.format(item.getUnitPrice().longValue()) : "0", cellFont, Element.ALIGN_RIGHT, Color.WHITE);
                addCell(table, item.getAmount() != null ? CURRENCY_FORMAT.format(item.getAmount().longValue()) : "0", boldCellFont, Element.ALIGN_RIGHT, Color.WHITE);
            }

            // Summary Rows
            addSummaryRow(table, "Tiền gốc:", invoice.getSubtotal() != null ? CURRENCY_FORMAT.format(invoice.getSubtotal().longValue()) + " đ" : "0 đ", cellFont);
            if (invoice.getLateFee() != null && invoice.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(table, "Phạt nộp muộn (5%):", CURRENCY_FORMAT.format(invoice.getLateFee().longValue()) + " đ", cellFont);
            }
            addSummaryRow(table, "TỔNG CỘNG:", invoice.getTotal() != null ? CURRENCY_FORMAT.format(invoice.getTotal().longValue()) + " đ" : "0 đ", boldCellFont);

            document.add(table);

            // Signatures Table
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50f, 50f});
            signTable.setSpacingBefore(30);

            PdfPCell signLeft = new PdfPCell();
            signLeft.setBorder(PdfPCell.NO_BORDER);
            signLeft.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph pSignL = new Paragraph("Người nộp tiền\n(Ký và ghi rõ họ tên)\n\n\n\n", valueFont);
            pSignL.setAlignment(Element.ALIGN_CENTER);
            signLeft.addElement(pSignL);
            signTable.addCell(signLeft);

            PdfPCell signRight = new PdfPCell();
            signRight.setBorder(PdfPCell.NO_BORDER);
            signRight.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph pSignR = new Paragraph("Người lập phiếu / Kế toán KTX\n(Ký và ghi rõ họ tên)\n\n\n\n", valueFont);
            pSignR.setAlignment(Element.ALIGN_CENTER);
            signRight.addElement(pSignR);
            signTable.addCell(signRight);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF biên lai: " + e.getMessage(), e);
        }
    }

    private static void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, font));
        cLabel.setColspan(4);
        cLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cLabel.setPadding(5);
        cLabel.setBackgroundColor(new Color(248, 250, 252));
        table.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(value, font));
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cVal.setPadding(5);
        cVal.setBackgroundColor(new Color(248, 250, 252));
        table.addCell(cVal);
    }

    private static void addCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBackgroundColor(bg);
        table.addCell(cell);
    }

    private static Font getPdfFont(float size, int style, Color color) {
        BaseFont baseFont = findSystemUnicodeFont();
        if (baseFont != null) {
            return new Font(baseFont, size, style, color);
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private static BaseFont findSystemUnicodeFont() {
        String[] candidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/tahoma.ttf",
                "C:/Windows/Fonts/times.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
        };
        for (String path : candidates) {
            if (new File(path).exists()) {
                try {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook, byte[] rgb) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        style.setFont(font);

        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setBorders(style);
        return style;
    }

    private CellStyle createBorderedStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
