package com.ktx.web.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@RequestMapping("/admin/invoices")
@PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) InvoiceStatus status,
                       @RequestParam(value = "type", required = false) InvoiceType type,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       Model model) {
        // Quét cập nhật hóa đơn quá hạn
        try {
            billingEngine.applyLateFees(LocalDate.now());
        } catch (Exception ignored) {
        }

        List<Invoice> invoices = invoiceService.searchInvoices(status, type, keyword);

        // Tính toán các chỉ số thống kê tổng quan
        List<Invoice> allInvoices = invoiceRepository.findAllByOrderByDueDateDesc();
        long totalCount = allInvoices.size();
        long paidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        long overdueCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count();
        long unpaidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.UNPAID).count();

        BigDecimal totalRevenue = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebt = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.OVERDUE)
                .map(i -> invoiceService.getRemainingAmount(i.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> remainingMap = new HashMap<>();
        Map<Long, BigDecimal> paidMap = new HashMap<>();
        for (Invoice inv : invoices) {
            remainingMap.put(inv.getId(), invoiceService.getRemainingAmount(inv.getId()));
            paidMap.put(inv.getId(), invoiceService.getTotalPaid(inv.getId()));
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
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("types", InvoiceType.values());
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
        model.addAttribute("pageTitle", "Hóa đơn #" + invoice.getInvoiceNo());
        model.addAttribute("pageSubtitle", "Chi tiết các khoản thu và lịch sử thanh toán");
        model.addAttribute("activeMenu", "invoices");

        return "admin/invoices/detail";
    }

    @PostMapping("/{id}/cancel")
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

        return "redirect:/admin/invoices/" + id;
    }
}
