package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.BuildingService;
import com.ktx.service.UserService;

@WebMvcTest(controllers = AdminUserController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void studentCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void staffCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void adminCanAccessUserList() throws Exception {
        when(userService.listBqlUsers()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Người dùng")))
                .andExpect(content().string(containsString("Quản lý tài khoản Admin và Cán bộ tòa")));
    }

    @Test
    void createStaffWithoutBuilding_validationFails() throws Exception {
        when(buildingService.listAll()).thenReturn(List.of());

        mockMvc.perform(post("/admin/users")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("username", "staffC")
                .param("email", "staffc@example.com")
                .param("fullName", "Cán bộ C")
                .param("role", "STAFF")
                .param("password", "password123")
                .param("assignedBuildingId", "")) // Empty building
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bắt buộc phải chọn tòa nhà")));
    }

    @Test
    void createStaffWithoutFullName_validationFails() throws Exception {
        when(buildingService.listAll()).thenReturn(List.of());

        mockMvc.perform(post("/admin/users")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("username", "staffC")
                .param("email", "staffc@example.com")
                .param("fullName", "") // Empty full name
                .param("role", "STAFF")
                .param("password", "password123")
                .param("assignedBuildingId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Họ tên cán bộ không được để trống")));
    }
}
