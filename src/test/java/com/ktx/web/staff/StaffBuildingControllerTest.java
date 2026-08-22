package com.ktx.web.staff;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Building;
import com.ktx.domain.Staff;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.RoomDiagramDto;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.BuildingService;
import com.ktx.service.RoomService;

@WebMvcTest(controllers = StaffBuildingController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StaffBuildingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private StaffScope staffScope;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void studentCannotAccessStaffRooms() throws Exception {
        mockMvc.perform(get("/staff/buildings/1/rooms").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void redirectRoomsFetchesStaffProfileAndRedirects() throws Exception {
        Staff staff = new Staff();
        Building b = new Building();
        b.setId(2L);
        staff.setAssignedBuilding(b);

        when(staffRepository.findByUserUsername("staffA")).thenReturn(Optional.of(staff));

        mockMvc.perform(get("/staff/buildings/rooms").with(user("staffA").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/buildings/2/rooms"));
    }

    @Test
    void staffViewsRoomsSuccessfully() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);

        when(buildingService.getById(1L)).thenReturn(b);
        when(roomService.getRoomDiagramGroupByFloor(1L)).thenReturn(Map.of());

        mockMvc.perform(get("/staff/buildings/1/rooms").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sơ đồ phòng")))
                .andExpect(content().string(containsString("Tòa A")));
    }

    @Test
    void staffAccessDeniedThrowsForbidden() throws Exception {
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(staffScope).assertBuilding(org.mockito.ArgumentMatchers.any(Authentication.class), org.mockito.ArgumentMatchers.eq(2L));

        mockMvc.perform(get("/staff/buildings/2/rooms").with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }
}
