package com.ktx.web.staff;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Building;
import com.ktx.dto.DashboardSnapshot;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;
import com.ktx.service.DashboardService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;
import com.ktx.web.admin.AdminMenuAdvice;

@WebMvcTest(controllers = StaffDashboardController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class,
        AdminMenuAdvice.class})
class StaffDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private StaffScope staffScope;
    @MockitoBean
    private BuildingRepository buildingRepository;
    @MockitoBean
    private StaffRepository staffRepository;

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
    void staffCanAccessDashboardForAssignedBuilding() throws Exception {
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

        mockMvc.perform(get("/staff/dashboard").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bảng điều khiển Tòa A")))
                .andExpect(content().string(containsString("SV nội trú tòa")))
                .andExpect(content().string(containsString("Phòng hoạt động")));
    }

    @Test
    void staffDeniedWhenStaffScopeFails() throws Exception {
        when(staffScope.buildingId(any())).thenReturn(Optional.of(1L));
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(staffScope).assertBuilding(any(), eq(1L));

        mockMvc.perform(get("/staff/dashboard").with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessStaffDashboard() throws Exception {
        Building b = new Building();
        b.setId(2L);
        b.setCode("B");
        b.setName("Tòa B");

        DashboardSnapshot snap = new DashboardSnapshot();
        snap.setStaffView(true);
        snap.setBuildingName("Tòa B");

        when(staffScope.buildingId(any())).thenReturn(Optional.empty());
        when(buildingRepository.findAll()).thenReturn(List.of(b));
        when(buildingRepository.findById(2L)).thenReturn(Optional.of(b));
        when(dashboardService.loadForBuilding(2L)).thenReturn(snap);

        mockMvc.perform(get("/staff/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bảng điều khiển Tòa B")));
    }
}
