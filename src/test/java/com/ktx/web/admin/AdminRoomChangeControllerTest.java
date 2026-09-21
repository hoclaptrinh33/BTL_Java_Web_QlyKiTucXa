package com.ktx.web.admin;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.RoomChangeService;

@WebMvcTest(controllers = AdminRoomChangeController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminRoomChangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomChangeService roomChangeService;

    @MockitoBean
    private BedRepository bedRepository;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private KtxUserDetails adminUser() {
        User u = new User();
        u.setId(99L);
        u.setUsername("admin");
        u.setEmail("admin@ktx.vn");
        u.setPasswordHash("hashed");
        u.setRole(Role.ADMIN);
        u.setEnabled(true);
        return new KtxUserDetails(u);
    }

    @Test
    void list_asAdmin_rendersView() throws Exception {
        Building building = new Building();
        building.setId(1L);
        building.setName("Tòa A");

        Room room = new Room();
        room.setRoomNumber("101");
        room.setBuilding(building);
        room.setRoomType(RoomType.STANDARD_4);

        Bed bed = new Bed();
        bed.setId(10L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.VACANT);
        bed.setRoom(room);

        Student student = new Student();
        student.setId(5L);
        student.setFullName("Nguyen Van A");
        student.setStudentCode("SV001");
        student.setGender(Gender.MALE);

        Contract contract = new Contract();
        contract.setId(20L);
        contract.setContractNo("HD-2026-001");
        contract.setStudent(student);
        contract.setBed(bed);

        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(100L);
        req.setRequestKind(RoomChangeKind.CHANGE);
        req.setStatus(RoomChangeStatus.SUBMITTED);
        req.setStudent(student);
        req.setContract(contract);
        req.setCurrentBed(bed);

        when(roomChangeService.searchRequests(any(), any(), any())).thenReturn(List.of(req));
        when(bedRepository.findVacantBedsWithDetails()).thenReturn(List.of(bed));
        when(buildingRepository.findAll()).thenReturn(List.of(building));

        mockMvc.perform(get("/admin/room-changes").with(user(adminUser())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý Đổi / Trả phòng")))
                .andExpect(content().string(containsString("Nguyen Van A")));
    }

    @Test
    void list_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/room-changes"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void approveRoomChange_asAdmin_callsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/room-changes/100/approve")
                        .with(user(adminUser()))
                        .with(csrf())
                        .param("targetBedId", "15")
                        .param("adminNote", "Duyệt đổi phòng"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/room-changes"));

        verify(roomChangeService).approveAndExecuteRoomChange(eq(100L), eq(15L), eq(99L), eq("Duyệt đổi phòng"));
    }

    @Test
    void approveReturnRoom_asAdmin_callsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/room-changes/200/checkout")
                        .with(user(adminUser()))
                        .with(csrf())
                        .param("assetNote", "Tài sản bàn giao tốt")
                        .param("ok", "true")
                        .param("depositDecision", "REFUNDED")
                        .param("force", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/room-changes"));

        verify(roomChangeService).approveReturnRoom(
                eq(200L), eq(99L), eq("Tài sản bàn giao tốt"), eq(true), eq(DepositStatus.REFUNDED), eq(false), any()
        );
    }

    @Test
    void rejectRequest_asAdmin_callsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/room-changes/300/reject")
                        .with(user(adminUser()))
                        .with(csrf())
                        .param("adminNote", "Không đủ điều kiện"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/room-changes"));

        verify(roomChangeService).rejectRequest(eq(300L), eq(99L), eq("Không đủ điều kiện"));
    }
}