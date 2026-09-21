package com.ktx.web.staff;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.domain.Building;
import com.ktx.domain.Invoice;
import com.ktx.domain.Room;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.UtilityReadingForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.BillingEngine;
import com.ktx.service.UtilityReadingService;

@WebMvcTest(controllers = StaffReadingController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StaffReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilityReadingService utilityReadingService;

    @MockitoBean
    private BillingEngine billingEngine;

    @MockitoBean
    private RoomRepository roomRepository;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private ContractRepository contractRepository;

    @MockitoBean
    private StaffScope staffScope;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void studentCannotAccessStaffReadings() throws Exception {
        mockMvc.perform(get("/staff/readings").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void staffViewsReadingsListSuccessfully() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A1");
        b.setName("Tòa A1");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setFloor(1);
        r.setRoomType(RoomType.STANDARD_4);
        r.setBuilding(b);

        when(staffScope.buildingId(any())).thenReturn(Optional.of(1L));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(buildingRepository.findAll()).thenReturn(List.of(b));
        when(roomRepository.findByBuildingIdWithBuilding(1L)).thenReturn(List.of(r));
        when(contractRepository.findOccupyingByRoomId(eq(10L), any())).thenReturn(List.of());
        when(utilityReadingService.getReading(eq(10L), any(YearMonth.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/staff/readings").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chỉ số điện nước")))
                .andExpect(content().string(containsString("101")));
    }

    @Test
    void staffAccessDeniedWhenRecordingForOtherBuilding() throws Exception {
        Building b = new Building();
        b.setId(2L);
        b.setCode("B1");

        Room r = new Room();
        r.setId(20L);
        r.setRoomNumber("201");
        r.setBuilding(b);

        when(roomRepository.findByIdWithBuilding(20L)).thenReturn(Optional.of(r));
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(staffScope).assertRoom(any(Authentication.class), eq(r));

        mockMvc.perform(get("/staff/readings/record")
                        .param("roomId", "20")
                        .with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void staffCanViewRecordFormForOwnRoom() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A1");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setBuilding(b);

        when(roomRepository.findByIdWithBuilding(10L)).thenReturn(Optional.of(r));
        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(10L);
        form.setBillingMonth("2026-09");
        form.setElecPrev(100);
        form.setWaterPrev(10);
        when(utilityReadingService.prepareForm(eq(10L), any(YearMonth.class))).thenReturn(form);

        mockMvc.perform(get("/staff/readings/record")
                        .param("roomId", "10")
                        .param("month", "2026-09")
                        .with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ghi chỉ số điện nước")))
                .andExpect(content().string(containsString("101")));
    }

    @Test
    void staffRecordsReadingSuccessfully() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A1");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setBuilding(b);

        when(roomRepository.findByIdWithBuilding(10L)).thenReturn(Optional.of(r));

        mockMvc.perform(post("/staff/readings/record")
                        .param("roomId", "10")
                        .param("billingMonth", "2026-09")
                        .param("elecPrev", "100")
                        .param("elecCurr", "250")
                        .param("waterPrev", "10")
                        .param("waterCurr", "25")
                        .with(csrf())
                        .with(user("staffA").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/readings?buildingId=1&month=2026-09"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(utilityReadingService).recordReading(any(UtilityReadingForm.class), any(Authentication.class));
    }

    @Test
    void staffIssuesInvoiceSuccessfully() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A1");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setBuilding(b);

        when(roomRepository.findByIdWithBuilding(10L)).thenReturn(Optional.of(r));
        Invoice invoice = new Invoice();
        invoice.setId(100L);
        invoice.setInvoiceType(InvoiceType.UTILITY);
        when(billingEngine.issueUtilityInvoices(eq(10L), eq(YearMonth.of(2026, 9))))
                .thenReturn(List.of(invoice));

        mockMvc.perform(post("/staff/readings/10/issue")
                        .param("month", "2026-09")
                        .with(csrf())
                        .with(user("staffA").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/readings?buildingId=1&month=2026-09"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void adminRedirectsFromAdminReadings() throws Exception {
        mockMvc.perform(get("/admin/readings")
                        .param("buildingId", "1")
                        .param("month", "2026-09")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/readings?buildingId=1&month=2026-09"));
    }

    @Test
    void adminBatchGeneratesInvoices() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A1");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setBuilding(b);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(roomRepository.findByBuildingIdWithBuilding(1L)).thenReturn(List.of(r));
        when(utilityReadingService.getReading(eq(10L), any(YearMonth.class)))
                .thenReturn(Optional.of(new UtilityReading()));
        Invoice invoice = new Invoice();
        when(billingEngine.issueUtilityInvoices(eq(10L), any(YearMonth.class)))
                .thenReturn(List.of(invoice));

        mockMvc.perform(post("/admin/invoices/generate")
                        .param("buildingId", "1")
                        .param("month", "2026-09")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/readings?buildingId=1&month=2026-09"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
