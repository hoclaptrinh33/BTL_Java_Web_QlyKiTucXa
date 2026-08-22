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

import com.ktx.repository.UserRepository;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;

@WebMvcTest
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private BuildingService buildingService;

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
