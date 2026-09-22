package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.dto.DashboardSnapshot;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;
import com.ktx.service.DashboardService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class,
        AdminMenuAdvice.class})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private DashboardService dashboardService;
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
    void adminSeesHomeShellAndSidebar() throws Exception {
        when(dashboardService.load()).thenReturn(new DashboardSnapshot());

        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng sinh viên")))
                .andExpect(content().string(containsString("Sinh viên")))
                .andExpect(content().string(containsString("Đợt đăng ký")))
                .andExpect(content().string(containsString("Phân bổ chỗ ở")))
                .andExpect(content().string(containsString("Hóa đơn")))
                .andExpect(content().string(containsString("Yêu cầu sửa chữa")))
                .andExpect(content().string(containsString("Thống kê")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Cài đặt hệ thống"))));
    }

    @Test
    void adminGetsOccupancyChartApi() throws Exception {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("system", java.util.Map.of("occupied", 10, "vacant", 5, "occupancyPercent", 66.7));
        map.put("buildings", java.util.List.of());
        when(dashboardService.getOccupancyChartData()).thenReturn(map);

        mockMvc.perform(get("/admin/dashboard/api/occupancy").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.system.occupied").value(10))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.system.occupancyPercent").value(66.7));
    }

    @Test
    void adminGetsDebtByMonthChartApi() throws Exception {
        com.ktx.dto.DebtByMonthDto dto = new com.ktx.dto.DebtByMonthDto();
        dto.setLabels(java.util.List.of("09/2026"));
        dto.setData(java.util.List.of(new java.math.BigDecimal("15000000")));
        dto.setTotalDebt(new java.math.BigDecimal("15000000"));
        dto.setTotalInvoices(2);
        when(dashboardService.calculateDebtByMonth()).thenReturn(dto);

        mockMvc.perform(get("/admin/dashboard/api/debt-by-month").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.labels[0]").value("09/2026"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0]").value(15000000))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalInvoices").value(2));
    }
}
