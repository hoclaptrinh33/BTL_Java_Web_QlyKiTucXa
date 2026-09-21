package com.ktx.web.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ktx.domain.Building;
import com.ktx.domain.Invoice;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.BillingEngine;
import com.ktx.service.InvoiceService;

@WebMvcTest(controllers = AdminInvoiceController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminInvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BillingEngine billingEngine;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private StaffScope staffScope;

    private Invoice createSampleInvoice() {
        Student s = new Student();
        s.setId(1L);
        s.setFullName("Nguyen Van A");
        s.setStudentCode("SV001");

        Building b = new Building();
        b.setId(1L);
        b.setCode("A");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setBuilding(b);

        Invoice inv = new Invoice();
        inv.setId(100L);
        inv.setInvoiceNo("INV-2026-000100");
        inv.setStudent(s);
        inv.setRoom(r);
        inv.setInvoiceType(InvoiceType.UTILITY);
        inv.setBillingMonth(LocalDate.of(2026, 9, 1));
        inv.setSubtotal(new BigDecimal("250000"));
        inv.setLateFee(BigDecimal.ZERO);
        inv.setTotal(new BigDecimal("250000"));
        inv.setDueDate(LocalDate.of(2026, 9, 30));
        inv.setStatus(InvoiceStatus.UNPAID);
        inv.setIdempotencyKey("UTILITY:1:10:2026-09");
        return inv;
    }

    @Test
    @DisplayName("GET /admin/invoices: Admin truy cập thành công")
    void adminCanAccessInvoiceList() throws Exception {
        Invoice inv = createSampleInvoice();
        when(invoiceService.searchInvoices(any(), any(), any())).thenReturn(List.of(inv));
        when(invoiceRepository.findAllByOrderByDueDateDesc()).thenReturn(List.of(inv));
        when(invoiceService.getRemainingAmount(100L)).thenReturn(new BigDecimal("250000"));
        when(invoiceService.getTotalPaid(100L)).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/admin/invoices")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("invoices", "totalCount", "unpaidCount"))
                .andExpect(view().name("admin/invoices/list"));
    }

    @Test
    @DisplayName("GET /admin/invoices: Staff bị chặn 403 Forbidden")
    void staffCannotAccessAdminInvoices() throws Exception {
        mockMvc.perform(get("/admin/invoices")
                        .with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/invoices/{id}: Admin xem chi tiết hóa đơn thành công")
    void adminCanViewInvoiceDetail() throws Exception {
        Invoice inv = createSampleInvoice();
        when(invoiceService.getInvoiceById(100L)).thenReturn(inv);
        when(invoiceService.getInvoiceItems(100L)).thenReturn(List.of());
        when(invoiceService.getInvoicePayments(100L)).thenReturn(List.of());
        when(invoiceService.getTotalPaid(100L)).thenReturn(BigDecimal.ZERO);
        when(invoiceService.getRemainingAmount(100L)).thenReturn(new BigDecimal("250000"));

        mockMvc.perform(get("/admin/invoices/100")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("invoice", "items", "payments"))
                .andExpect(view().name("admin/invoices/detail"));
    }

    @Test
    @DisplayName("POST /admin/invoices/{id}/cancel: Admin hủy hóa đơn thành công")
    void adminCanCancelInvoice() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/admin/invoices/100/cancel")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices/100"));

        verify(invoiceService).cancel(100L, 1L);
    }
}
