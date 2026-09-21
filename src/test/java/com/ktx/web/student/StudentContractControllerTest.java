package com.ktx.web.student;

import java.util.List;
import java.util.Optional;

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
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.CheckInOutRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.RoomChangeService;

@WebMvcTest(controllers = StudentContractController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StudentContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private ContractRepository contractRepository;

    @MockitoBean
    private CheckInOutRepository checkInOutRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private RoomAssetRepository roomAssetRepository;

    @MockitoBean
    private RoomChangeService roomChangeService;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private KtxUserDetails studentUser() {
        User u = new User();
        u.setId(50L);
        u.setUsername("sv001");
        u.setEmail("sv001@ktx.vn");
        u.setPasswordHash("hashed");
        u.setRole(Role.STUDENT);
        u.setEnabled(true);
        return new KtxUserDetails(u);
    }

    private Student mockStudent() {
        Student s = new Student();
        s.setId(5L);
        s.setFullName("Nguyen Van A");
        s.setStudentCode("B22DCCN001");
        s.setGender(Gender.MALE);
        return s;
    }

    private Contract mockContract(Student s) {
        Building b = new Building();
        b.setId(1L);
        b.setName("Tòa A");

        Room r = new Room();
        r.setId(10L);
        r.setRoomNumber("101");
        r.setBuilding(b);

        Bed bed = new Bed();
        bed.setId(100L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.OCCUPIED);
        bed.setRoom(r);

        Contract c = new Contract();
        c.setId(1L);
        c.setContractNo("HD-001");
        c.setStatus(ContractStatus.ACTIVE);
        c.setStudent(s);
        c.setBed(bed);
        return c;
    }

    @Test
    void roomChangePage_rendersSuccessfully() throws Exception {
        Student s = mockStudent();
        Contract c = mockContract(s);

        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(5L), any())).thenReturn(List.of(c));
        when(roomChangeService.findByStudentIdAndKind(5L, RoomChangeKind.CHANGE)).thenReturn(List.of());
        when(buildingRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/student/room-change").with(user(studentUser())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đơn xin chuyển phòng ở")));
    }

    @Test
    void submitRoomChange_submitsAndRedirects() throws Exception {
        Student s = mockStudent();
        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));

        mockMvc.perform(post("/student/room-change")
                        .with(user(studentUser()))
                        .with(csrf())
                        .param("targetRoomType", "STANDARD_4")
                        .param("reason", "Muốn ở cùng bạn cùng lớp")
                        .param("preferredBuildingId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/room-change"));

        verify(roomChangeService).submitRoomChangeRequest(eq(5L), eq(1L), eq(RoomType.STANDARD_4), eq("Muốn ở cùng bạn cùng lớp"));
    }

    @Test
    void cancelRoomChange_cancelsAndRedirects() throws Exception {
        Student s = mockStudent();
        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));

        mockMvc.perform(post("/student/room-change/10/cancel")
                        .with(user(studentUser()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/room-change"));

        verify(roomChangeService).cancelRequest(10L, 5L);
    }

    @Test
    void returnRoomPage_rendersSuccessfully() throws Exception {
        Student s = mockStudent();
        Contract c = mockContract(s);

        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(5L), any())).thenReturn(List.of(c));
        when(roomChangeService.findByStudentIdAndKind(5L, RoomChangeKind.RETURN)).thenReturn(List.of());

        mockMvc.perform(get("/student/return-room").with(user(studentUser())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thủ tục trả phòng")));
    }

    @Test
    void submitReturnRoom_submitsAndRedirects() throws Exception {
        Student s = mockStudent();
        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));

        mockMvc.perform(post("/student/return-room")
                        .with(user(studentUser()))
                        .with(csrf())
                        .param("returnDate", "2026-06-30")
                        .param("reason", "Hết kỳ học")
                        .param("bankAccount", "123456789")
                        .param("bankName", "Vietcombank"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/return-room"));

        verify(roomChangeService).submitReturnRoomRequest(eq(5L), eq("2026-06-30"), eq("Hết kỳ học"), eq("Vietcombank"), eq("123456789"));
    }

    @Test
    void cancelReturnRoom_cancelsAndRedirects() throws Exception {
        Student s = mockStudent();
        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));

        mockMvc.perform(post("/student/return-room/25/cancel")
                        .with(user(studentUser()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/return-room"));

        verify(roomChangeService).cancelRequest(25L, 5L);
    }
}