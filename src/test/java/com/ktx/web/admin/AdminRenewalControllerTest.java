package com.ktx.web.admin;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.domain.Building;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.RenewalService;

@WebMvcTest(controllers = AdminRenewalController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminRenewalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RenewalService renewalService;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private KtxUserDetails adminUser() {
        User u = new User();
        u.setId(99L);
        u.setUsername("admin");
        u.setEmail("admin@ktx.vn");
        u.setPasswordHash("hashed");
        u.setRole(Role.ADMIN);
        u.setEnabled(true);
        return new KtxUserDetails(u);
    }

    private KtxUserDetails studentUser() {
        User u = new User();
        u.setId(10L);
        u.setUsername("sv001");
        u.setEmail("sv001@ktx.vn");
        u.setPasswordHash("hashed");
        u.setRole(Role.STUDENT);
        u.setEnabled(true);
        return new KtxUserDetails(u);
    }

    @Test
    void list_asAdmin_rendersView() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setName("Tòa A");

        when(buildingRepository.findAll()).thenReturn(List.of(b));
        when(renewalService.searchRequests(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/renewals").with(user(adminUser())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý Gia hạn hợp đồng")));
    }

    @Test
    void approveRenewal_asAdmin_approvesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/renewals/1/approve")
                        .with(user(adminUser()))
                        .with(csrf())
                        .param("adminNote", "Đồng ý gia hạn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/renewals"));

        verify(renewalService).approveRenewal(eq(1L), eq(99L), eq("Đồng ý gia hạn"));
    }

    @Test
    void rejectRenewal_asAdmin_rejectsAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/renewals/1/reject")
                        .with(user(adminUser()))
                        .with(csrf())
                        .param("adminNote", "Không đủ điều kiện"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/renewals"));

        verify(renewalService).rejectRenewal(eq(1L), eq(99L), eq("Không đủ điều kiện"));
    }

    @Test
    void list_asStudent_forbidden() throws Exception {
        mockMvc.perform(get("/admin/renewals").with(user(studentUser())))
                .andExpect(status().isForbidden());
    }
}
