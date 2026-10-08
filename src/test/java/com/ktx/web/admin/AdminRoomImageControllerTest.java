package com.ktx.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Building;
import com.ktx.domain.RoomImage;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomImageRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.MediaGalleryService;

@WebMvcTest(controllers = AdminRoomImageController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class, AdminMenuAdvice.class})
class AdminRoomImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomImageRepository roomImageRepository;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private MediaGalleryService mediaGalleryService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/room-types/images"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanViewRoomTypeImagesPage() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);
        b.setActive(true);

        RoomImage img = new RoomImage(RoomType.STANDARD_4, b, null, "/images/rooms/room-standard-4.jpg", "Ảnh mẫu", true, 0);
        img.setId(10L);

        when(roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(RoomType.STANDARD_4))
                .thenReturn(List.of(img));
        when(buildingRepository.findAll()).thenReturn(List.of(b));

        mockMvc.perform(get("/admin/room-types/images?type=STANDARD_4")
                        .with(user("admin").roles("ADMIN").authorities(() -> "ROLE_ADMIN", () -> "room.read")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rooms/type_images"))
                .andExpect(model().attributeExists("selectedType", "roomTypes", "images", "buildings"))
                .andExpect(model().attribute("selectedType", RoomType.STANDARD_4));
    }

    @Test
    void adminCanSetPrimaryImage() throws Exception {
        mockMvc.perform(post("/admin/room-types/images/10/primary")
                        .with(user("admin").roles("ADMIN").authorities(() -> "ROLE_ADMIN", () -> "room.write"))
                        .with(csrf())
                        .param("roomType", "STANDARD_4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/room-types/images?type=STANDARD_4"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(mediaGalleryService).setRoomTypePrimaryImage(RoomType.STANDARD_4, 10L);
    }

    @Test
    void adminCanDeleteImage() throws Exception {
        mockMvc.perform(post("/admin/room-types/images/10/delete")
                        .with(user("admin").roles("ADMIN").authorities(() -> "ROLE_ADMIN", () -> "room.write"))
                        .with(csrf())
                        .param("roomType", "STANDARD_4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/room-types/images?type=STANDARD_4"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(mediaGalleryService).deleteRoomImage(10L);
    }
}
