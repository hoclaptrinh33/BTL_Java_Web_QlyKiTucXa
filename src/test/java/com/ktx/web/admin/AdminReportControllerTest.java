package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;
import com.ktx.service.ExportService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;

@WebMvcTest(controllers = AdminReportController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class,
        AdminMenuAdvice.class})
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private ExportService exportService;
    @MockitoBean
    private ContractRepository contractRepository;
    @MockitoBean
    private InvoiceRepository invoiceRepository;
    @MockitoBean
    private StudentService studentService;
    @MockitoBean
    private BuildingService buildingService;
    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void adminCanAccessReportIndex() throws Exception {
        mockMvc.perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/reports/residents")))
                .andExpect(content().string(containsString("/admin/reports/debts")));
    }

    @Test
    void studentCannotAccessReports() throws Exception {
        mockMvc.perform(get("/admin/reports").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDownloadResidentsXlsx() throws Exception {
        byte[] fakeXlsx = new byte[]{0x50, 0x4B, 0x03, 0x04}; // zip header for xlsx
        when(exportService.exportResidentsXlsx()).thenReturn(fakeXlsx);

        mockMvc.perform(get("/admin/reports/residents").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("danh-sach-noi-tru.xlsx")))
                .andExpect(content().bytes(fakeXlsx));
    }

    @Test
    void adminCanDownloadDebtsXlsx() throws Exception {
        byte[] fakeXlsx = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(exportService.exportDebtsXlsx()).thenReturn(fakeXlsx);

        mockMvc.perform(get("/admin/reports/debts?format=xlsx").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("danh-sach-no-qua-han.xlsx")))
                .andExpect(content().bytes(fakeXlsx));
    }

    @Test
    void adminCanDownloadDebtsPdf() throws Exception {
        byte[] fakePdf = "%PDF-1.4 fake".getBytes();
        when(exportService.exportDebtsPdf()).thenReturn(fakePdf);

        mockMvc.perform(get("/admin/reports/debts?format=pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("danh-sach-no-qua-han.pdf")))
                .andExpect(content().bytes(fakePdf));
    }

    @Test
    void adminCanDownloadInvoicePdf() throws Exception {
        byte[] fakePdf = "%PDF-1.4 fake invoice".getBytes();
        when(exportService.exportInvoicePdf(1L)).thenReturn(fakePdf);

        mockMvc.perform(get("/admin/reports/invoices/1/pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("bien-lai-hoa-don-1.pdf")))
                .andExpect(content().bytes(fakePdf));
    }
}
