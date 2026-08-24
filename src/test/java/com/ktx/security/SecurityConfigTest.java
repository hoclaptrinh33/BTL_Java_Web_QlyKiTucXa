package com.ktx.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;
import com.ktx.service.DashboardService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;
import com.ktx.web.ErrorPageController;
import com.ktx.web.HomeController;
import com.ktx.web.admin.AdminDashboardController;
import com.ktx.web.auth.LoginController;
import com.ktx.web.auth.RegisterController;
import com.ktx.web.staff.StaffDashboardController;
import com.ktx.web.student.StudentDashboardController;

@WebMvcTest(controllers = {
    HomeController.class,
    LoginController.class,
    RegisterController.class,
    ErrorPageController.class,
    AdminDashboardController.class,
    StaffDashboardController.class,
    StudentDashboardController.class
})
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.ktx.repository.StudentRepository studentRepository;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void staffCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void adminCanAccessStaff() throws Exception {
        mockMvc.perform(get("/staff/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void loginIsPermitted() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("Đăng nhập với Google")));
    }

    @Test
    void googleLoginIsPermitted() throws Exception {
        mockMvc.perform(get("/login/google"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?google"));
    }

    @Test
    void loginShowsErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không đúng tài khoản hoặc mật khẩu")));
    }

    @Test
    void forbiddenPageIsNotBlank() throws Exception {
        mockMvc.perform(get("/error/403"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có quyền truy cập")));
    }

    @Test
    void rootRedirectsAdminToDashboard() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void anonymousAdminRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }
}
