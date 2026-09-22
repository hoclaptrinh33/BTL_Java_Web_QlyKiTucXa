package com.ktx.web.manage;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.common.exception.BusinessException;
import com.ktx.dto.PermissionDto;
import com.ktx.dto.RoleDto;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.RoleService;

@WebMvcTest(controllers = ManageRoleController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class ManageRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @Test
    void canViewRolesList() throws Exception {
        RoleDto quanLy = new RoleDto("QUAN_LY", "Quản lý KTX", "Mô tả", true, "ALL", Set.of("room.read"));
        when(roleService.listRoles()).thenReturn(List.of(quanLy));

        mockMvc.perform(get("/manage/roles").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("QUAN_LY")))
                .andExpect(content().string(containsString("Quản lý KTX")));
    }

    @Test
    void newRoleFormLoadsOperationPermissions() throws Exception {
        when(roleService.getOperationPermissions()).thenReturn(List.of(
                new PermissionDto("room.read", "Xem danh sách phòng", "Phòng ở", false)
        ));

        mockMvc.perform(get("/manage/roles/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("room.read")))
                .andExpect(content().string(containsString("Xem danh sách phòng")));
    }

    @Test
    void canCreateOperationRole() throws Exception {
        mockMvc.perform(post("/manage/roles")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("code", "KY_THUAT")
                        .param("name", "Kỹ thuật viên")
                        .param("description", "Sửa chữa")
                        .param("scope", "BUILDINGS")
                        .param("permissions", "ticket.handle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/roles"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(roleService).createRole(any(RoleDto.class), any());
    }

    @Test
    void createRoleWithSystemPermissionFails() throws Exception {
        when(roleService.createRole(any(RoleDto.class), any()))
                .thenThrow(new BusinessException("Không thể gắn quyền SYSTEM ('config.read') cho vai trò vận hành"));

        mockMvc.perform(post("/manage/roles")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("code", "INVALID_ROLE")
                        .param("name", "Vai trò sai")
                        .param("permissions", "config.read"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không thể gắn quyền SYSTEM")));
    }

    @Test
    void cannotDeleteSystemLockedRole() throws Exception {
        doThrow(new BusinessException("Không thể xóa vai trò mặc định của hệ thống (QUAN_LY)"))
                .when(roleService).deleteRole("QUAN_LY");

        mockMvc.perform(post("/manage/roles/QUAN_LY/delete")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/roles"))
                .andExpect(flash().attribute("errorMessage", "Không thể xóa vai trò mặc định của hệ thống (QUAN_LY)"));
    }
}
