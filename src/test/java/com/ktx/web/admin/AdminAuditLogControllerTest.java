package com.ktx.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.AuditLog;
import com.ktx.dto.AuditLogSummaryDto;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AuditLogService;

@WebMvcTest(controllers = AdminAuditLogController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class, AdminMenuAdvice.class})
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/logs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanAccessAuditLogsPage() throws Exception {
        AuditLog sampleLog = new AuditLog(1L, "admin", "ADMIN", "LOGIN_SUCCESS", "AUTH", "admin", "Đăng nhập", "127.0.0.1", "SUCCESS");
        when(auditLogService.getLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));
        when(auditLogService.getSummary())
                .thenReturn(new AuditLogSummaryDto(10L, 5L, 1L, 2L));
        when(auditLogService.getDistinctActions())
                .thenReturn(List.of("LOGIN_SUCCESS", "CONFIG_UPDATE"));
        when(auditLogService.getDistinctTargetTypes())
                .thenReturn(List.of("AUTH", "SYSTEM_CONFIG"));

        mockMvc.perform(get("/admin/logs")
                        .with(user("admin").roles("ADMIN").authorities(() -> "ROLE_ADMIN", () -> "log.read")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/logs/index"))
                .andExpect(model().attributeExists("logPage", "summary", "actionsList", "targetTypesList"));
    }
}
