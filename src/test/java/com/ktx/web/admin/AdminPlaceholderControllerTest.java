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
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;
import com.ktx.service.DashboardService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;

@WebMvcTest(controllers = AdminPlaceholderController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class, AdminMenuAdvice.class})
class AdminPlaceholderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

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
    void adminCanAccessStats() throws Exception {
        DashboardSnapshot snapshot = new DashboardSnapshot();
        snapshot.setStudentCount(100);
        snapshot.setOccupyingStudentCount(80);
        snapshot.setOccupancyPercent(80.0);
        snapshot.setActiveRoomCount(20);
        when(dashboardService.load()).thenReturn(snapshot);

        mockMvc.perform(get("/manage/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thống kê &amp; Công suất Lưu trú Ký túc xá")));

        mockMvc.perform(get("/admin/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thống kê &amp; Công suất Lưu trú Ký túc xá")));
    }

    @Test
    void studentCannotAccessStats() throws Exception {
        mockMvc.perform(get("/admin/stats").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
