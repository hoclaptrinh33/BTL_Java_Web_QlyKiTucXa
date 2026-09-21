package com.ktx.web.staff;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.CheckInOutService;
import com.ktx.service.ContractService;

@WebMvcTest(controllers = StaffCheckInController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StaffCheckInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private CheckInOutService checkInOutService;

    @MockitoBean
    private StaffScope staffScope;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private RoomAssetRepository roomAssetRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void studentCannotAccessStaffCheckIn() throws Exception {
        mockMvc.perform(get("/staff/checkin").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void staffViewsCheckInListSuccessfully() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");

        when(staffScope.buildingId(any())).thenReturn(Optional.of(1L));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(contractService.findByBuildingAndStatus(eq(1L), any())).thenReturn(List.of());
        when(checkInOutService.findRecent(1L)).thenReturn(List.of());

        mockMvc.perform(get("/staff/checkin").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Check-in")))
                .andExpect(content().string(containsString("Tòa A")));
    }

    @Test
    void staffAccessDeniedWhenAccessingContractFromAnotherBuilding() throws Exception {
        Building b = new Building();
        b.setId(2L);
        b.setCode("B");

        Room room = new Room();
        room.setId(20L);
        room.setBuilding(b);

        Bed bed = new Bed();
        bed.setId(200L);
        bed.setRoom(room);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setContractNo("HD-2026-000010");
        contract.setBed(bed);
        contract.setStatus(ContractStatus.DRAFT);

        when(contractService.getByIdWithDetails(10L)).thenReturn(contract);
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(staffScope).assertBuilding(any(Authentication.class), eq(2L));

        mockMvc.perform(get("/staff/checkin/10").with(user("staffA").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void staffCanViewCheckInFormForOwnBuilding() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);

        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setBuilding(b);

        Bed bed = new Bed();
        bed.setId(100L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.OCCUPIED);
        bed.setRoom(room);

        Student student = new Student();
        student.setId(5L);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");
        student.setGender(Gender.MALE);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setContractNo("HD-2026-000001");
        contract.setStudent(student);
        contract.setBed(bed);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setRoomFee(new BigDecimal("1200000"));
        contract.setDepositAmount(new BigDecimal("600000"));
        contract.setStartDate(LocalDate.of(2026, 9, 1));
        contract.setEndDate(LocalDate.of(2027, 1, 31));

        when(contractService.getByIdWithDetails(1L)).thenReturn(contract);
        when(roomAssetRepository.findByRoomIdOrderByIdAsc(10L)).thenReturn(List.of());

        mockMvc.perform(get("/staff/checkin/1").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thủ tục Check-in")))
                .andExpect(content().string(containsString("HD-2026-000001")))
                .andExpect(content().string(containsString("Nguyen Van A")));
    }
}
