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
import com.ktx.web.admin.AdminStudentController;
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
    AdminStudentController.class,
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
    private com.ktx.repository.ContractRepository contractRepository;

    @MockitoBean
    private com.ktx.repository.RoomApplicationRepository roomApplicationRepository;

    @MockitoBean
    private com.ktx.repository.InvoiceRepository invoiceRepository;

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

    @MockitoBean
    private com.ktx.security.StaffScope staffScope;

    @MockitoBean
    private com.ktx.repository.BuildingRepository buildingRepository;

    @Test
    void staffCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void adminCannotAccessStudents() throws Exception {
        mockMvc.perform(get("/admin/students").with(user("admin").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("config.read"))))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void quanlyCanAccessStudents() throws Exception {
        mockMvc.perform(get("/admin/students").with(user("quanly").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("student.read"))))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotAccessStudents() throws Exception {
        mockMvc.perform(get("/admin/students").with(user("staff").roles("STAFF")))
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
    void notFoundPageIsNotBlank() throws Exception {
        mockMvc.perform(get("/error/404"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("404")));
    }

    @Test
    void rootRedirectsAdminToDashboard() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void rootRedirectsSystemAdminToConfigs() throws Exception {
        mockMvc.perform(get("/").with(user("admin").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("config.read"))))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/configs"));
    }

    @Test
    void anonymousAdminRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }
}
