package com.ktx.web.student;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.Student;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.RoomApplicationService;

@Controller
@RequestMapping("/student/invoices")
public class StudentInvoiceController {

    private final StudentRepository studentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;

    public StudentInvoiceController(StudentRepository studentRepository,
                                    InvoiceRepository invoiceRepository,
                                    ContractRepository contractRepository) {
        this.studentRepository = studentRepository;
        this.invoiceRepository = invoiceRepository;
        this.contractRepository = contractRepository;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Invoice> invoices = invoiceRepository.findByStudentIdOrderByDueDateDesc(student.getId());

        List<Invoice> unpaidInvoices = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.OVERDUE)
                .collect(Collectors.toList());

        BigDecimal totalUnpaidAmount = unpaidInvoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("student", student);
        model.addAttribute("invoices", invoices);
        model.addAttribute("unpaidCount", unpaidInvoices.size());
        model.addAttribute("totalUnpaidAmount", totalUnpaidAmount);
        model.addAttribute("pageTitle", "Hóa đơn & Tiền phòng");
        model.addAttribute("pageSubtitle", "Tra cứu nghĩa vụ tài chính và lịch sử thanh toán");
        model.addAttribute("activeMenu", "invoices");
        return "student/invoices/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Principal principal, Model model) {
        Student student = getStudent(principal);
        Invoice invoice = invoiceRepository.findById(id).orElse(null);

        // Bảo mật: chỉ xem hóa đơn của chính mình
        if (invoice != null && !invoice.getStudent().getId().equals(student.getId())) {
            throw new BusinessException("Bạn không có quyền truy cập hóa đơn này.");
        }

        model.addAttribute("student", student);
        model.addAttribute("invoice", invoice);
        model.addAttribute("pageTitle", "Chi tiết hóa đơn " + (invoice != null ? invoice.getInvoiceNo() : ""));
        model.addAttribute("pageSubtitle", "Thông tin đối soát và công thức tính tiền");
        model.addAttribute("activeMenu", "invoices");
        return "student/invoices/detail";
    }

    private Student getStudent(Principal principal) {
        return studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(RoomApplicationService.STUDENT_NOT_FOUND));
    }
}
