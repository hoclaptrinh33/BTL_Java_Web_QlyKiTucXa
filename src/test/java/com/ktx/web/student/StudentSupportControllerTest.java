package com.ktx.web.student;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.domain.enums.TicketPriority;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.ConductService;
import com.ktx.service.TicketService;

@WebMvcTest(controllers = StudentSupportController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StudentSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private ContractRepository contractRepository;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private ConductService conductService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private Student student;
    private Contract contract;
    private Room room1;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student1");
        user.setRole(Role.STUDENT);

        student = new Student();
        student.setId(10L);
        student.setUser(user);
        student.setFullName("Nguyen Van A");
        student.setStudentCode("B22DCCN001");
        student.setConductScore(90);
        student.setBlockedFromHousing(false);

        Building building = new Building();
        building.setId(1L);
        building.setName("Tòa A");
        building.setCode("A");

        room1 = new Room();
        room1.setId(101L);
        room1.setRoomNumber("101");
        room1.setBuilding(building);

        Bed bed = new Bed();
        bed.setId(1001L);
        bed.setBedCode("G1");
        bed.setRoom(room1);

        contract = new Contract();
        contract.setId(50L);
        contract.setStudent(student);
        contract.setBed(bed);

        when(studentRepository.findByUserUsername("student1")).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));
    }

    @Test
    void studentCanViewTickets() throws Exception {
        when(ticketService.getTicketsForStudent(10L)).thenReturn(List.of());

        mockMvc.perform(get("/student/tickets").with(user("student1").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCanViewViolations() throws Exception {
        when(conductService.getViolationsForStudent(10L)).thenReturn(List.of());

        mockMvc.perform(get("/student/violations").with(user("student1").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCreateTicket_ownRoom_success() throws Exception {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(1L);
        when(ticketService.createTicket(eq(10L), eq(101L), eq("Bóng đèn cháy"), eq("Đèn hỏng"), any(TicketPriority.class)))
                .thenReturn(ticket);

        mockMvc.perform(post("/student/tickets")
                        .with(user("student1").roles("STUDENT"))
                        .with(csrf())
                        .param("roomId", "101")
                        .param("title", "Bóng đèn cháy")
                        .param("description", "Đèn hỏng")
                        .param("priority", "HIGH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/tickets"));

        verify(ticketService).createTicket(eq(10L), eq(101L), eq("Bóng đèn cháy"), eq("Đèn hỏng"), eq(TicketPriority.HIGH));
    }

    @Test
    void studentCreateTicket_wrongRoom_forbidden403() throws Exception {
        // Sinh viên ở phòng 101 nhưng gửi request cho phòng 999 -> 403
        mockMvc.perform(post("/student/tickets")
                        .with(user("student1").roles("STUDENT"))
                        .with(csrf())
                        .param("roomId", "999")
                        .param("title", "Bóng đèn cháy")
                        .param("description", "Đèn hỏng")
                        .param("priority", "HIGH"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void studentCloseTicket_resolved_success() throws Exception {
        mockMvc.perform(post("/student/tickets/1/close")
                        .with(user("student1").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/tickets"));

        verify(ticketService).closeTicketByStudent(1L, 10L);
    }
}
