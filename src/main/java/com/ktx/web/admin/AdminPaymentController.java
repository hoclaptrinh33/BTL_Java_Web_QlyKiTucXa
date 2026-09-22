package com.ktx.web.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Payment;
import com.ktx.domain.User;
import com.ktx.domain.enums.PaymentMethod;
import com.ktx.repository.UserRepository;
import com.ktx.service.PaymentService;

@Controller
@RequestMapping({"/manage/payments", "/admin/payments"})
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('payment.record')")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public AdminPaymentController(PaymentService paymentService,
                                  UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       Model model) {
        List<Payment> payments = paymentService.searchPayments(keyword);

        BigDecimal totalCollected = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("payments", payments);
        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("totalCount", payments.size());
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Lịch sử Thanh toán & Thu tiền");
        model.addAttribute("pageSubtitle", "Nhật ký thu tiền mặt và chuyển khoản tại quầy BQL KTX");
        model.addAttribute("activeMenu", "payments");

        return "admin/payments/list";
    }

    @PostMapping
    public String recordPayment(@RequestParam("invoiceId") Long invoiceId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("method") PaymentMethod method,
                                @RequestParam(value = "referenceNo", required = false) String referenceNo,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        User actor = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin quản trị viên"));

        try {
            Payment p = paymentService.recordPayment(invoiceId, amount, method, referenceNo, actor.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ghi nhận thanh toán thành công " + String.format("%,d", p.getAmount().longValue()) + " đ cho hóa đơn #" + p.getInvoice().getInvoiceNo());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:" + invoiceBase() + "/" + invoiceId;
    }

    private String invoiceBase() {
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
}
