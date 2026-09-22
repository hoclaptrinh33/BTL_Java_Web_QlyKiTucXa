package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;

@WebMvcTest(controllers = AdminAccountController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class, AdminMenuAdvice.class})
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @Test
    void adminCanViewAccountsList() throws Exception {
        User admin1 = new User();
        admin1.setId(1L);
        admin1.setUsername("admin1");
        admin1.setEmail("admin1@example.com");
        admin1.setRole(Role.ADMIN);
        admin1.setEnabled(true);

        when(userRepository.findByRoleIn(List.of(Role.ADMIN))).thenReturn(List.of(admin1));

        mockMvc.perform(get("/admin/accounts").with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin1")))
                .andExpect(content().string(containsString("admin1@example.com")));
    }

    @Test
    void adminCanCreateNewAdmin() throws Exception {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(userRepository.existsByEmail("newadmin@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        mockMvc.perform(post("/admin/accounts")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "newadmin")
                        .param("email", "newadmin@example.com")
                        .param("password", "StrongPass123")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void cannotLockLastActiveAdmin() throws Exception {
        User onlyAdmin = new User();
        onlyAdmin.setId(1L);
        onlyAdmin.setUsername("admin");
        onlyAdmin.setRole(Role.ADMIN);
        onlyAdmin.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.findByRoleIn(List.of(Role.ADMIN))).thenReturn(List.of(onlyAdmin));

        mockMvc.perform(post("/admin/accounts/1/toggle-status")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("errorMessage", "Không thể khóa tài khoản quản trị viên hệ thống cuối cùng"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void canLockWhenMultipleActiveAdminsExist() throws Exception {
        User admin1 = new User();
        admin1.setId(1L);
        admin1.setUsername("admin1");
        admin1.setRole(Role.ADMIN);
        admin1.setEnabled(true);

        User admin2 = new User();
        admin2.setId(2L);
        admin2.setUsername("admin2");
        admin2.setRole(Role.ADMIN);
        admin2.setEnabled(true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(admin2));
        when(userRepository.findByRoleIn(List.of(Role.ADMIN))).thenReturn(List.of(admin1, admin2));

        mockMvc.perform(post("/admin/accounts/2/toggle-status")
                        .with(csrf())
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userRepository).save(admin2);
    }
}
