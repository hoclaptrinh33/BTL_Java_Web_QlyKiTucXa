package com.ktx.web.admin;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AllocationService;

@WebMvcTest(controllers = AdminAllocationController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminAllocationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AllocationService allocationService;

    @MockitoBean
    private RoomApplicationRepository roomApplicationRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    @DisplayName("STAFF không được phép truy cập /admin/allocations (403)")
    void staffCannotAccessAllocations() throws Exception {
        mockMvc.perform(get("/admin/allocations").with(user("staff1").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    @DisplayName("STUDENT không được phép truy cập /admin/allocations (403)")
    void studentCannotAccessAllocations() throws Exception {
        mockMvc.perform(get("/admin/allocations").with(user("sv1").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    @DisplayName("Người dùng chưa đăng nhập truy cập /admin/allocations bị chuyển hướng về /login")
    void anonymousRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/allocations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ADMIN được phép truy cập /admin/allocations")
    void adminCanAccessAllocations() throws Exception {
        when(allocationService.getAvailablePeriods()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/allocations").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
