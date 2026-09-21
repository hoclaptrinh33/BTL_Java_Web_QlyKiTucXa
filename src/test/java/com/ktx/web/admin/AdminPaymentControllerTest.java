package com.ktx.web.admin;

import java.math.BigDecimal;
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

import com.ktx.domain.Invoice;
import com.ktx.domain.Payment;
import com.ktx.domain.User;
import com.ktx.domain.enums.PaymentMethod;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.PaymentService;

@WebMvcTest(controllers = AdminPaymentController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

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
    @DisplayName("GET /admin/payments: Admin xem danh sách thanh toán thành công")
    void adminCanAccessPaymentsList() throws Exception {
        when(paymentService.searchPayments(any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/payments")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("payments", "totalCollected"))
                .andExpect(view().name("admin/payments/list"));
    }

    @Test
    @DisplayName("GET /admin/payments: Staff bị chặn 403 Forbidden")
    void staffCannotAccessPayments() throws Exception {
        mockMvc.perform(get("/admin/payments")
                        .with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/payments: Admin ghi nhận thanh toán thành công")
    void adminCanRecordPayment() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        Invoice inv = new Invoice();
        inv.setId(10L);
        inv.setInvoiceNo("INV-2026-000010");

        Payment p = new Payment();
        p.setId(1L);
        p.setInvoice(inv);
        p.setAmount(new BigDecimal("500000"));
        when(paymentService.recordPayment(eq(10L), eq(new BigDecimal("500000")), eq(PaymentMethod.CASH), any(), eq(1L)))
                .thenReturn(p);

        mockMvc.perform(post("/admin/payments")
                        .param("invoiceId", "10")
                        .param("amount", "500000")
                        .param("method", "CASH")
                        .param("referenceNo", "REF-001")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices/10"));

        verify(paymentService).recordPayment(eq(10L), eq(new BigDecimal("500000")), eq(PaymentMethod.CASH), eq("REF-001"), eq(1L));
    }
}
