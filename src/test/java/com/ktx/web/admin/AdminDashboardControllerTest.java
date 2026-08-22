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
                .andExpect(content().string(containsString("Cài đặt hệ thống")));
    }
}
