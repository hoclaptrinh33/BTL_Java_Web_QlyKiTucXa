package com.ktx.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.ConductService;

@WebMvcTest(controllers = AdminViolationController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminViolationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConductService conductService;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void adminCanViewViolations() throws Exception {
        when(buildingRepository.findAll()).thenReturn(List.of());
        when(conductService.getViolationsForAdmin(any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/violations").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanResetConductScores() throws Exception {
        mockMvc.perform(post("/admin/violations/reset")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/violations"));

        verify(conductService).resetAllConductScores();
    }

    @Test
    void adminCanRecordViolation() throws Exception {
        mockMvc.perform(post("/admin/violations")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("studentId", "10")
                        .param("violationType", "DAMAGE")
                        .param("severity", "SEVERE")
                        .param("pointsDeducted", "50")
                        .param("action", "TERMINATE")
                        .param("description", "Lam hong tai san")
                        .param("occurredAt", "2026-09-21T08:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/violations"));

        verify(conductService).recordViolation(
                eq(10L), eq("admin"), eq(ViolationType.DAMAGE), eq(ViolationSeverity.SEVERE),
                eq(50), eq(ViolationAction.TERMINATE), eq("Lam hong tai san"),
                eq(LocalDateTime.of(2026, 9, 21, 8, 30)), any(Authentication.class));
    }
}
