package com.ktx.web.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Payment;
import com.ktx.domain.User;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.BillingEngine;
import com.ktx.service.InvoiceService;

@Controller
@RequestMapping({"/manage/invoices", "/admin/invoices"})
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('invoice.read')")
public class AdminInvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final BillingEngine billingEngine;

    public AdminInvoiceController(InvoiceService invoiceService,
                                  InvoiceRepository invoiceRepository,
                                  UserRepository userRepository,
                                  BillingEngine billingEngine) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.billingEngine = billingEngine;
    }

    private static volatile LocalDate lastLateFeeRunDate = null;

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) InvoiceStatus status,
                       @RequestParam(value = "type", required = false) InvoiceType type,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        try {
            LocalDate today = LocalDate.now();
            if (!today.equals(lastLateFeeRunDate)) {
                billingEngine.applyLateFees(today);
                lastLateFeeRunDate = today;
            }
        } catch (Exception ignored) {
        }

        Page<Invoice> invoicePage = invoiceService.searchInvoices(status, type, keyword, PageRequest.of(Math.max(page, 0), 30));
        if (invoicePage == null) {
            invoicePage = Page.empty();
        }
        List<Invoice> invoices = invoicePage.getContent();

        long totalCount = 0;
        long paidCount = 0;
        long overdueCount = 0;
        long unpaidCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        List<Object[]> summary = invoiceService.summarizeByStatus();
        if (summary != null) {
            for (Object[] row : summary) {
                InvoiceStatus rowStatus = (InvoiceStatus) row[0];
                long count = ((Number) row[1]).longValue();
                BigDecimal sum = (BigDecimal) row[2];
                totalCount += count;
                if (rowStatus == InvoiceStatus.PAID) {
                    paidCount = count;
                    totalRevenue = sum;
                } else if (rowStatus == InvoiceStatus.OVERDUE) {
                    overdueCount = count;
                    totalDebt = totalDebt.add(sum);
                } else if (rowStatus == InvoiceStatus.UNPAID) {
                    unpaidCount = count;
                    totalDebt = totalDebt.add(sum);
                }
            }
        }

        List<Long> ids = new ArrayList<>();
        for (Invoice inv : invoices) {
            ids.add(inv.getId());
        }
        Map<Long, BigDecimal> fetchedPaid = ids.isEmpty() ? Map.of() : invoiceService.paidAmounts(ids);
        Map<Long, BigDecimal> paidMap = new HashMap<>(fetchedPaid != null ? fetchedPaid : Map.of());
        Map<Long, BigDecimal> remainingMap = new HashMap<>();
        for (Invoice inv : invoices) {
            BigDecimal paid = paidMap.getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal total = inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO;
            BigDecimal remaining = total.subtract(paid);
            remainingMap.put(inv.getId(), remaining.signum() > 0 ? remaining : BigDecimal.ZERO);
        }

        model.addAttribute("invoices", invoices);
        model.addAttribute("remainingMap", remainingMap);
        model.addAttribute("paidMap", paidMap);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalDebt", totalDebt);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("keyword", keyword);
        model.addAttribute("invoicePage", invoicePage);
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("types", InvoiceType.values());
        model.addAttribute("invoiceBase", base());
        model.addAttribute("paymentBase", paymentBase());
        model.addAttribute("pageTitle", "Quản lý Hóa đơn & Thu phí");
        model.addAttribute("pageSubtitle", "Tiền phòng theo kỳ, tiền đặt cọc và hóa đơn điện nước");
        model.addAttribute("activeMenu", "invoices");

        return "admin/invoices/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        try {
            billingEngine.applyLateFees(LocalDate.now());
        } catch (Exception ignored) {
        }

        Invoice invoice = invoiceService.getInvoiceById(id);
        List<InvoiceItem> items = invoiceService.getInvoiceItems(id);
        List<Payment> payments = invoiceService.getInvoicePayments(id);
        BigDecimal totalPaid = invoiceService.getTotalPaid(id);
        BigDecimal remaining = invoiceService.getRemainingAmount(id);

        model.addAttribute("invoice", invoice);
        model.addAttribute("items", items);
        model.addAttribute("payments", payments);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("remaining", remaining);
        model.addAttribute("invoiceBase", base());
        model.addAttribute("paymentBase", paymentBase());
        model.addAttribute("pageTitle", "Hóa đơn #" + invoice.getInvoiceNo());
        model.addAttribute("pageSubtitle", "Chi tiết các khoản thu và lịch sử thanh toán");
        model.addAttribute("activeMenu", "invoices");

        return "admin/invoices/detail";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('invoice.issue')")
    public String cancel(@PathVariable("id") Long id,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        User actor = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin quản trị viên"));

        try {
            invoiceService.cancel(id, actor.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hóa đơn #" + id + " thành công!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:" + base() + "/" + id;
    }

    private String base() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String uri = sra.getRequest().getRequestURI();
                if (uri != null && uri.startsWith("/manage")) {
                    return "/manage/invoices";
                }
            }
        } catch (Exception ignored) {}
        return "/admin/invoices";
    }

    private String paymentBase() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String uri = sra.getRequest().getRequestURI();
                if (uri != null && uri.startsWith("/manage")) {
                    return "/manage/payments";
                }
            }
        } catch (Exception ignored) {}
        return "/admin/payments";
    }
}
