package com.ktx.web.admin;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.service.ExportService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('report.read')")
public class AdminReportController {

    private final ExportService exportService;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;

    public AdminReportController(ExportService exportService,
                                 ContractRepository contractRepository,
                                 InvoiceRepository invoiceRepository) {
        this.exportService = exportService;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping({"/manage/reports", "/admin/reports"})
    public String reports(Model model) {
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("pageTitle", "Báo cáo");
        model.addAttribute("pageSubtitle", "Xuất Excel / PDF danh sách nội trú và công nợ");
        model.addAttribute("occupyingCount", contractRepository.countByStatusIn(OccupyingStatuses.OCCUPYING));
        model.addAttribute("overdueCount", invoiceRepository.countByStatus(InvoiceStatus.OVERDUE));
        model.addAttribute("unpaidCount", invoiceRepository.countByStatus(InvoiceStatus.UNPAID));
        return "admin/reports/index";
    }

    @GetMapping({"/manage/reports/residents", "/manage/reports/residents.xlsx", "/admin/reports/residents", "/admin/reports/residents.xlsx"})
    public ResponseEntity<byte[]> downloadResidentsXlsx() {
        byte[] data = exportService.exportResidentsXlsx();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("danh-sach-noi-tru.xlsx", StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @GetMapping({"/manage/reports/debts", "/manage/reports/debts.xlsx", "/manage/reports/debts.pdf", "/manage/reports/debts/pdf", "/admin/reports/debts", "/admin/reports/debts.xlsx", "/admin/reports/debts.pdf", "/admin/reports/debts/pdf"})
    public ResponseEntity<byte[]> downloadDebts(@RequestParam(value = "format", required = false) String format,
                                                HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean isPdf = "pdf".equalsIgnoreCase(format) || uri.endsWith(".pdf") || uri.endsWith("/pdf");

        HttpHeaders headers = new HttpHeaders();
        if (isPdf) {
            byte[] data = exportService.exportDebtsPdf();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("danh-sach-no-qua-han.pdf", StandardCharsets.UTF_8)
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } else {
            byte[] data = exportService.exportDebtsXlsx();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("danh-sach-no-qua-han.xlsx", StandardCharsets.UTF_8)
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        }
    }

    @GetMapping({"/manage/reports/invoices/{id}/pdf", "/manage/invoices/{id}/pdf", "/admin/reports/invoices/{id}/pdf", "/admin/invoices/{id}/pdf"})
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable("id") Long id) {
        byte[] data = exportService.exportInvoicePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("bien-lai-hoa-don-" + id + ".pdf", StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
