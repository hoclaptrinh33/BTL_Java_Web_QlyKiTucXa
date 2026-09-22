package com.ktx.web.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ktx.config.MailConfig;
import com.ktx.domain.SystemConfig;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.SystemConfigService;

@WebMvcTest(controllers = AdminConfigController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemConfigService systemConfigService;

    @MockitoBean
    private MailConfig mailConfig;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    @DisplayName("STUDENT role is forbidden from accessing /admin/configs")
    void student_forbiddenFromConfigs() throws Exception {
        mockMvc.perform(get("/admin/configs").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    @DisplayName("STAFF role is forbidden from accessing /admin/configs")
    void staff_forbiddenFromConfigs() throws Exception {
        mockMvc.perform(get("/admin/configs").with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor systemAdmin() {
        return user("admin").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("config.read"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("config.write")
        );
    }

    @Test
    @DisplayName("QUAN_LY (ROLE_ADMIN) is forbidden from accessing /admin/configs")
    void quanly_forbiddenFromConfigs() throws Exception {
        mockMvc.perform(get("/admin/configs").with(user("quanly").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN can view /admin/configs with grouped configs and mail status")
    void admin_canViewConfigs() throws Exception {
        Map<String, List<SystemConfig>> mockGrouped = new LinkedHashMap<>();
        mockGrouped.put("alloc", new ArrayList<>());
        mockGrouped.put("contract", new ArrayList<>());
        mockGrouped.put("billing", new ArrayList<>());
        mockGrouped.put("conduct", new ArrayList<>());
        mockGrouped.put("other", new ArrayList<>());

        when(systemConfigService.getConfigsGrouped()).thenReturn(mockGrouped);
        when(mailConfig.isMailEnabled()).thenReturn(false);

        mockMvc.perform(get("/admin/configs").with(systemAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/configs/index"))
                .andExpect(model().attributeExists("groupedConfigs"))
                .andExpect(model().attribute("mailEnabled", false))
                .andExpect(model().attribute("pageTitle", "Cài đặt hệ thống"));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN can batch update configs successfully")
    void admin_canBatchUpdateConfigs() throws Exception {
        mockMvc.perform(post("/admin/configs")
                        .with(systemAdmin())
                        .with(csrf())
                        .param("contract.deposit.ratio", "0.4")
                        .param("billing.water.price_per_m3", "16000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/configs"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(systemConfigService).updateConfigs(anyMap());
    }

    @Test
    @DisplayName("SYSTEM_ADMIN batch update with invalid data adds error flash attribute")
    void admin_batchUpdateInvalid_showsErrorMessage() throws Exception {
        doThrow(new IllegalArgumentException("Tỉ lệ đặt cọc không hợp lệ"))
                .when(systemConfigService).updateConfigs(anyMap());

        mockMvc.perform(post("/admin/configs")
                        .with(systemAdmin())
                        .with(csrf())
                        .param("contract.deposit.ratio", "2.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/configs"))
                .andExpect(flash().attribute("errorMessage", "Tỉ lệ đặt cọc không hợp lệ"));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN can update single config")
    void admin_canUpdateSingleConfig() throws Exception {
        mockMvc.perform(post("/admin/configs/single")
                        .with(systemAdmin())
                        .with(csrf())
                        .param("key", "alloc.preference.mode")
                        .param("value", "STRICT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/configs"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(systemConfigService).set(eq("alloc.preference.mode"), eq("STRICT"));
    }
}
