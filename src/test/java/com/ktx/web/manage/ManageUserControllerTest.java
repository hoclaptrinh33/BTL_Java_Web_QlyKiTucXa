package com.ktx.web.manage;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Building;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.dto.BqlUserForm;
import com.ktx.dto.RoleDto;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.BuildingService;
import com.ktx.service.RoleService;
import com.ktx.service.UserService;

@WebMvcTest(controllers = ManageUserController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class ManageUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @Test
    void canViewInternalUsersList() throws Exception {
        BqlUserForm u = new BqlUserForm();
        u.setId(1L);
        u.setUsername("canbo_a");
        u.setEmail("canboa@example.com");
        u.setRole(Role.STAFF);
        u.setEnabled(true);

        when(userService.listBqlUsers()).thenReturn(List.of(u));
        when(roleService.getRolesForUser(1L)).thenReturn(List.of("CAN_BO"));

        mockMvc.perform(get("/manage/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("canbo_a")))
                .andExpect(content().string(containsString("canboa@example.com")));
    }

    @Test
    void newFormExcludesSystemAdminRole() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of(
                new RoleDto("SYSTEM_ADMIN", "Quản trị hệ thống", "System", true, "ALL", Set.of()),
                new RoleDto("QUAN_LY", "Quản lý KTX", "Manage", true, "ALL", Set.of()),
                new RoleDto("CAN_BO", "Cán bộ tòa", "Staff", true, "BUILDINGS", Set.of())
        ));
        when(buildingService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/manage/users/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("QUAN_LY")))
                .andExpect(content().string(containsString("CAN_BO")));
    }

    @Test
    void cannotAssignSystemAdminRoleFromThisForm() throws Exception {
        mockMvc.perform(post("/manage/users")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "bad_admin")
                        .param("email", "bad@example.com")
                        .param("password", "ValidPass123")
                        .param("roles", "SYSTEM_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(model().hasErrors())
                .andExpect(content().string(containsString("Không thể gán vai trò Quản trị hệ thống (SYSTEM_ADMIN)")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void studentAccountCannotReceiveInternalRole() throws Exception {
        when(studentRepository.existsByStudentCode("SV2026001")).thenReturn(true);

        mockMvc.perform(post("/manage/users")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "SV2026001")
                        .param("email", "sv@example.com")
                        .param("password", "ValidPass123")
                        .param("roles", "CAN_BO"))
                .andExpect(status().isOk())
                .andExpect(model().hasErrors())
                .andExpect(content().string(containsString("Tài khoản sinh viên không nhận vai nội bộ")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void canCreateInternalUserWithMultiRolesAndBuildingScope() throws Exception {
        when(userRepository.existsByUsername("new_staff")).thenReturn(false);
        when(userRepository.existsByEmail("new_staff@example.com")).thenReturn(false);
        when(studentRepository.existsByStudentCode("new_staff")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setUsername("new_staff");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Building b1 = new Building();
        b1.setId(1L);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b1));

        mockMvc.perform(post("/manage/users")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "new_staff")
                        .param("email", "new_staff@example.com")
                        .param("fullName", "Cán bộ mới")
                        .param("password", "PassWord123")
                        .param("roles", "CAN_BO")
                        .param("scope", "BUILDINGS")
                        .param("assignedBuildingIds", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userRepository).save(any(User.class));
        verify(roleService).setRolesForUser(eq(10L), eq(List.of("CAN_BO")), eq("BUILDINGS"), eq(List.of(1L)));
    }
}
