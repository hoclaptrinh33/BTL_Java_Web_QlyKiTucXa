package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.RoomForm;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.BuildingService;
import com.ktx.service.RoomService;

@WebMvcTest(controllers = AdminRoomController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void staffCannotAccessRooms() throws Exception {
        mockMvc.perform(get("/admin/rooms").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void newFormHasTypesAndNoPhotoUpload() throws Exception {
        when(buildingService.listAll()).thenReturn(List.of(building()));
        when(roomService.capacityOf(any())).thenReturn(6);

        mockMvc.perform(get("/admin/rooms/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thêm phòng mới")))
                .andExpect(content().string(containsString("Tòa nhà")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("STANDARD_6")))
                .andExpect(content().string(containsString("Không upload ảnh")))
                .andExpect(content().string(not(containsString("Khu vực"))))
                .andExpect(content().string(not(containsString("Chọn ảnh"))));
    }

    @Test
    void createRedirectsToRoomList() throws Exception {
        Building a = building();
        Room room = new Room();
        room.setBuilding(a);
        room.setRoomNumber("101");
        room.setCapacity(6);
        when(roomService.create(any(RoomForm.class))).thenReturn(room);

        mockMvc.perform(post("/admin/rooms").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("buildingId", "1")
                        .param("roomNumber", "101")
                        .param("floor", "1")
                        .param("roomType", "STANDARD_6")
                        .param("pricePerTerm", "1800000")
                        .param("status", "ACTIVE"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/rooms?buildingId=1"));
        verify(roomService).create(any(RoomForm.class));
    }

    @Test
    void createDuplicateNumberShowsFieldError() throws Exception {
        when(buildingService.listAll()).thenReturn(List.of(building()));
        when(roomService.capacityOf(any())).thenReturn(6);
        when(roomService.create(any(RoomForm.class)))
                .thenThrow(new DuplicateFieldException("roomNumber", RoomService.DUPLICATE_NUMBER));

        mockMvc.perform(post("/admin/rooms").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("buildingId", "1")
                        .param("roomNumber", "101")
                        .param("floor", "1")
                        .param("roomType", "STANDARD_6")
                        .param("pricePerTerm", "1800000")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Số phòng đã có trong tòa này")));
    }

    @Test
    void createWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/admin/rooms").with(user("admin").roles("ADMIN"))
                        .param("buildingId", "1")
                        .param("roomNumber", "101")
                        .param("floor", "1")
                        .param("roomType", "STANDARD_6")
                        .param("pricePerTerm", "1800000")
                        .param("status", "ACTIVE"))
                .andExpect(status().isForbidden());
        verify(roomService, never()).create(any());
    }

    @Test
    void listShowsDoorChip() throws Exception {
        when(roomService.listRows(isNull())).thenReturn(List.of());
        when(buildingService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/rooms").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thêm phòng")));
    }

    @Test
    void deleteOccupiedShowsFlashError() throws Exception {
        Building a = building();
        Room room = new Room();
        room.setBuilding(a);
        room.setRoomNumber("101");
        when(roomService.getById(9L)).thenReturn(room);
        doThrow(new BusinessException(RoomService.CANNOT_DELETE_OCCUPIED)).when(roomService).delete(9L);

        mockMvc.perform(post("/admin/rooms/9/delete").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/rooms"));
    }

    private static Building building() {
        Building a = new Building();
        a.setId(1L);
        a.setCode("A");
        a.setName("Tòa A — Nam");
        a.setGenderPolicy(BuildingGenderPolicy.MALE);
        return a;
    }
}
