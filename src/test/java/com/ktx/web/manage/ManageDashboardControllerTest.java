package com.ktx.web.manage;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Building;
import com.ktx.dto.DashboardSnapshot;
import com.ktx.dto.DebtByMonthDto;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.BuildingService;
import com.ktx.service.DashboardService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;
import com.ktx.web.admin.AdminMenuAdvice;

@WebMvcTest(controllers = ManageDashboardController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class, AdminMenuAdvice.class})
class ManageDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private StaffScope staffScope;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private RoomService roomService;

    @Test
    void managerCanViewFullOperationalDashboard() throws Exception {
        DashboardSnapshot snap = new DashboardSnapshot();
        snap.setStudentCount(100);
        snap.setOccupyingStudentCount(80);
        snap.setOccupiedBeds(80);
        snap.setVacantBeds(20);
        snap.setOccupancyPercent(80.0);

        when(staffScope.buildingId(any())).thenReturn(Optional.empty());
        when(dashboardService.load()).thenReturn(snap);

        mockMvc.perform(get("/manage/dashboard").with(user("manager").roles("QUAN_LY")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng quan vận hành")))
                .andExpect(content().string(containsString("Tỷ lệ sử dụng giường")))
                .andExpect(content().string(containsString("Công nợ theo tháng")));
    }

    @Test
    void staffCanViewBuildingDashboardViaManage() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");

        DashboardSnapshot snap = new DashboardSnapshot();
        snap.setStaffView(true);
        snap.setBuildingName("Tòa A");
        snap.setOccupyingStudentCount(25);
        snap.setActiveRoomCount(10);
        snap.setOccupiedBeds(25);
        snap.setVacantBeds(15);
        snap.setOccupancyPercent(62.5);

        when(staffScope.buildingId(any())).thenReturn(Optional.of(1L));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(dashboardService.loadForBuilding(1L)).thenReturn(snap);

        mockMvc.perform(get("/manage/dashboard").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bảng điều khiển Tòa A")))
                .andExpect(content().string(containsString("SV nội trú tòa")));
    }

    @Test
    void testManageChartApis() throws Exception {
        Map<String, Object> chartData = Map.of("system", Map.of("occupied", 50, "vacant", 10));
        when(dashboardService.getOccupancyChartData()).thenReturn(chartData);

        DebtByMonthDto debtData = new DebtByMonthDto();
        debtData.setTotalDebt(BigDecimal.valueOf(5000000));
        when(dashboardService.calculateDebtByMonth()).thenReturn(debtData);

        mockMvc.perform(get("/manage/api/occupancy").with(user("manager").roles("QUAN_LY")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.system.occupied").value(50));

        mockMvc.perform(get("/manage/api/debt-by-month").with(user("manager").roles("QUAN_LY")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalDebt").value(5000000));
    }
}
