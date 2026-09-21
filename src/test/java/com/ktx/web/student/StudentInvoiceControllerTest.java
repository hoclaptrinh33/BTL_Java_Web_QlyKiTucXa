package com.ktx.web.student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ktx.domain.Invoice;
import com.ktx.domain.Student;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.BillingEngine;
import com.ktx.service.InvoiceService;

@WebMvcTest(controllers = StudentInvoiceController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StudentInvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private BillingEngine billingEngine;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private StaffScope staffScope;

    @Test
    @DisplayName("GET /student/invoices: Sinh viên xem danh sách hóa đơn cá nhân")
    void studentCanAccessTheirInvoices() throws Exception {
        Student student = new Student();
        student.setId(10L);
        student.setStudentCode("D22CQCN001");
        student.setFullName("Nguyen Van A");

        Invoice inv = new Invoice();
        inv.setId(50L);
        inv.setInvoiceNo("INV-2026-000050");
        inv.setStudent(student);
        inv.setInvoiceType(InvoiceType.UTILITY);
        inv.setTotal(new BigDecimal("150000"));
        inv.setStatus(InvoiceStatus.UNPAID);
        inv.setDueDate(LocalDate.now().plusDays(5));

        when(studentRepository.findByUserUsername("D22CQCN001")).thenReturn(Optional.of(student));
        when(invoiceRepository.findByStudentIdOrderByDueDateDesc(10L)).thenReturn(List.of(inv));
        when(invoiceService.getRemainingAmount(50L)).thenReturn(new BigDecimal("150000"));

        mockMvc.perform(get("/student/invoices")
                        .with(user("D22CQCN001").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("invoices", "totalUnpaidAmount"))
                .andExpect(view().name("student/invoices/list"));
    }

    @Test
    @DisplayName("GET /student/invoices/{id}: Sinh viên xem chi tiết hóa đơn của mình")
    void studentCanViewTheirInvoiceDetail() throws Exception {
        Student student = new Student();
        student.setId(10L);
        student.setStudentCode("D22CQCN001");
        student.setFullName("Nguyen Van A");

        Invoice inv = new Invoice();
        inv.setId(50L);
        inv.setInvoiceNo("INV-2026-000050");
        inv.setStudent(student);
        inv.setInvoiceType(InvoiceType.UTILITY);
        inv.setTotal(new BigDecimal("150000"));
        inv.setStatus(InvoiceStatus.UNPAID);
        inv.setDueDate(LocalDate.now().plusDays(5));

        when(studentRepository.findByUserUsername("D22CQCN001")).thenReturn(Optional.of(student));
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(inv));
        when(invoiceService.getInvoiceItems(50L)).thenReturn(List.of());
        when(invoiceService.getInvoicePayments(50L)).thenReturn(List.of());
        when(invoiceService.getTotalPaid(50L)).thenReturn(BigDecimal.ZERO);
        when(invoiceService.getRemainingAmount(50L)).thenReturn(new BigDecimal("150000"));

        mockMvc.perform(get("/student/invoices/50")
                        .with(user("D22CQCN001").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("invoice", "items", "payments", "totalPaid", "remaining"))
                .andExpect(view().name("student/invoices/detail"));
    }
}
