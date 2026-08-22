package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Building;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.BuildingForm;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.BuildingService;
import com.ktx.service.DashboardService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;

@WebMvcTest(controllers = AdminBuildingController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminBuildingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void staffCannotAccessBuildings() throws Exception {
        mockMvc.perform(get("/admin/buildings").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void adminListsBuildings() throws Exception {
        Building a = building(1L, "A", "Tòa A — Nam", BuildingGenderPolicy.MALE);
        when(buildingService.listAll()).thenReturn(List.of(a));

        mockMvc.perform(get("/admin/buildings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tòa A — Nam")))
                .andExpect(content().string(containsString("ktx-chip")))
                .andExpect(content().string(containsString("bldg-kpi-grid")))
                .andExpect(content().string(containsString("Thêm tòa nhà")))
                .andExpect(content().string(not(containsString("value=\"MIXED\""))));
    }

    @Test
    void newFormHasMaleAndFemaleOnly() throws Exception {
        mockMvc.perform(get("/admin/buildings/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nam")))
                .andExpect(content().string(containsString("Nữ")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("value=\"MALE\"")))
                .andExpect(content().string(containsString("value=\"FEMALE\"")))
                .andExpect(content().string(not(containsString("value=\"MIXED\""))));
    }

    @Test
    void createBuildingRedirects() throws Exception {
        Building saved = building(1L, "A", "Tòa A — Nam", BuildingGenderPolicy.MALE);
        when(buildingService.create(any(BuildingForm.class))).thenReturn(saved);

        mockMvc.perform(post("/admin/buildings").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("code", "A")
                        .param("name", "Tòa A — Nam")
                        .param("genderPolicy", "MALE")
                        .param("active", "true"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/buildings"));
        verify(buildingService).create(any(BuildingForm.class));
    }

    @Test
    void createDuplicateCodeShowsFieldError() throws Exception {
        when(buildingService.create(any(BuildingForm.class)))
                .thenThrow(new DuplicateFieldException("code", BuildingService.DUPLICATE_CODE));

        mockMvc.perform(post("/admin/buildings").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("code", "A")
                        .param("name", "Tòa A")
                        .param("genderPolicy", "MALE")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mã tòa đã được sử dụng")));
    }

    @Test
    void createWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/admin/buildings").with(user("admin").roles("ADMIN"))
                        .param("code", "A")
                        .param("name", "Tòa A")
                        .param("genderPolicy", "MALE"))
                .andExpect(status().isForbidden());
        verify(buildingService, never()).create(any());
    }

    @Test
    void deleteBuildingRedirects() throws Exception {
        Building b = building(1L, "A", "Tòa A", BuildingGenderPolicy.MALE);
        when(buildingService.getById(1L)).thenReturn(b);

        mockMvc.perform(post("/admin/buildings/1/delete").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/buildings"));
        verify(buildingService).delete(1L);
    }

    private static Building building(Long id, String code, String name, BuildingGenderPolicy gender) {
        Building building = new Building();
        building.setId(id);
        building.setCode(code);
        building.setName(name);
        building.setGenderPolicy(gender);
        building.setActive(true);
        return building;
    }
}
