package com.ktx.web.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.Role;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.CheckInOutService;
import com.ktx.service.ContractService;

@WebMvcTest(controllers = AdminContractController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class AdminContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private CheckInOutService checkInOutService;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private RoomAssetRepository roomAssetRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private KtxUserDetails createAdminUserDetails() {
        User user = new User();
        user.setId(99L);
        user.setUsername("admin");
        user.setEmail("admin@ktx.com");
        user.setPasswordHash("hash");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        return new KtxUserDetails(user);
    }

    private Contract createSampleContract() {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
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
        return contract;
    }

    @Test
    void staffCannotAccessAdminContracts() throws Exception {
        mockMvc.perform(get("/admin/contracts").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @Test
    void adminViewsContractsListSuccessfully() throws Exception {
        when(contractService.searchContracts(any(), any(), any())).thenReturn(List.of());
        when(buildingRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/contracts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý hợp đồng lưu trú")));
    }

    @Test
    void adminViewsContractDetailSuccessfully() throws Exception {
        Contract contract = createSampleContract();
        when(contractService.getByIdWithDetails(1L)).thenReturn(contract);
        when(checkInOutService.findByContractId(1L)).thenReturn(List.of());
        when(invoiceRepository.findByContractIdOrderByDueDateDesc(1L)).thenReturn(List.of());
        when(roomAssetRepository.findByRoomIdOrderByIdAsc(10L)).thenReturn(List.of());

        mockMvc.perform(get("/admin/contracts/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HD-2026-000001")))
                .andExpect(content().string(containsString("Nguyen Van A")));
    }

    @Test
    void adminPerformsCheckInSuccessfully() throws Exception {
        KtxUserDetails admin = createAdminUserDetails();

        mockMvc.perform(post("/admin/contracts/1/check-in")
                        .with(csrf())
                        .with(user(admin))
                        .param("assetNote", "Bàn giao phòng")
                        .param("ok", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/contracts/1"));

        verify(checkInOutService).checkIn(eq(1L), eq(99L), eq("Bàn giao phòng"), eq(true), any());
    }

    @Test
    void adminPerformsCheckOutSuccessfully() throws Exception {
        KtxUserDetails admin = createAdminUserDetails();

        mockMvc.perform(post("/admin/contracts/1/check-out")
                        .with(csrf())
                        .with(user(admin))
                        .param("assetNote", "Trả phòng đầy đủ")
                        .param("ok", "true")
                        .param("force", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/contracts/1"));

        verify(checkInOutService).checkOut(eq(1L), eq(99L), eq("Trả phòng đầy đủ"), eq(true), any(), eq(false), any());
    }

    @Test
    void adminCancelsDraftSuccessfully() throws Exception {
        KtxUserDetails admin = createAdminUserDetails();

        mockMvc.perform(post("/admin/contracts/1/cancel-draft")
                        .with(csrf())
                        .with(user(admin)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/contracts/1"));

        verify(contractService).cancelDraft(1L);
    }

    @Test
    void adminTerminatesContractSuccessfully() throws Exception {
        KtxUserDetails admin = createAdminUserDetails();

        mockMvc.perform(post("/admin/contracts/1/terminate")
                        .with(csrf())
                        .with(user(admin))
                        .param("forfeitDeposit", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/contracts/1"));

        verify(contractService).terminate(1L, true);
    }

    @Test
    void adminViewsCheckInOutLogSuccessfully() throws Exception {
        when(checkInOutService.findRecent(null)).thenReturn(List.of());
        when(buildingRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/check-in-out").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lịch sử Check-in / Check-out")));
    }
}
