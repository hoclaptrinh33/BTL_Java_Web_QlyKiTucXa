package com.ktx.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Building;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.PeriodGenderScope;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.domain.enums.Role;
import com.ktx.dto.RegistrationPeriodForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.DashboardService;
import com.ktx.service.RegistrationPeriodService;
import com.ktx.service.RoomService;
import com.ktx.service.StudentService;

@WebMvcTest(controllers = AdminPeriodController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class, AdminMenuAdvice.class})
class AdminPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationPeriodService periodService;

    @MockitoBean
    private BuildingRepository buildingRepository;

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

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    private KtxUserDetails createAdminUserDetails() {
        User u = new User();
        u.setId(1L);
        u.setUsername("admin");
        u.setRole(Role.ADMIN);
        u.setEnabled(true);
        return new KtxUserDetails(u);
    }

    @Test
    void listPeriods_success() throws Exception {
        User creator = new User();
        creator.setUsername("admin");

        RegistrationPeriod p = new RegistrationPeriod();
        p.setId(1L);
        p.setName("Đợt tân SV 2026");
        p.setPeriodType(PeriodType.FRESHMAN);
        p.setAcademicYear("2026-2027");
        p.setOpenAt(LocalDateTime.now().minusDays(1));
        p.setCloseAt(LocalDateTime.now().plusDays(2));
        p.setTermStart(LocalDate.now());
        p.setTermEnd(LocalDate.now().plusMonths(5));
        p.setStatus(PeriodStatus.DRAFT);
        p.setCreatedBy(creator);
        p.setGenderScope(PeriodGenderScope.ALL);

        when(periodService.listAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/admin/periods").with(user(createAdminUserDetails())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đợt tân SV 2026")))
                .andExpect(content().string(containsString("Thêm đợt đăng ký")));
    }

    @Test
    void newPeriodForm_displaysTabsAndFields() throws Exception {
        Building b = new Building();
        b.setId(1L);
        b.setName("Tòa A1");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);
        b.setActive(true);

        when(buildingRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(b));

        mockMvc.perform(get("/admin/periods/new").with(user(createAdminUserDetails())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1. Thông tin cơ bản & Thời gian nhận đơn")))
                .andExpect(content().string(containsString("2. Đối tượng & Phạm vi quỹ phòng")))
                .andExpect(content().string(containsString("3. Lộ trình xác nhận & Chính sách tài chính")))
                .andExpect(content().string(containsString("4. Cam kết nội quy & Thông tin hỗ trợ")))
                .andExpect(content().string(containsString("Tòa A1")));
    }

    @Test
    void createPeriod_success() throws Exception {
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("admin");

        RegistrationPeriod created = new RegistrationPeriod();
        created.setId(10L);
        created.setName("Đợt tân SV 2026");

        when(periodService.create(any(RegistrationPeriodForm.class), any(User.class))).thenReturn(created);

        mockMvc.perform(post("/admin/periods")
                        .with(user(createAdminUserDetails()))
                        .with(csrf())
                        .param("name", "Đợt tân SV 2026")
                        .param("periodType", "FRESHMAN")
                        .param("academicYear", "2026-2027")
                        .param("openAt", "2026-08-20T08:00")
                        .param("closeAt", "2026-08-30T17:00")
                        .param("termStart", "2026-09-01")
                        .param("termEnd", "2027-01-15")
                        .param("genderScope", "ALL")
                        .param("minConductScore", "60")
                        .param("targetQuota", "300")
                        .param("depositRatio", "0.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/periods"));

        verify(periodService).create(any(RegistrationPeriodForm.class), any(User.class));
    }

    @Test
    void editPeriodForm_loadsDetails() throws Exception {
        User creator = new User();
        creator.setUsername("admin");

        Building b = new Building();
        b.setId(1L);
        b.setName("Tòa A1");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);
        b.setActive(true);

        RegistrationPeriod p = new RegistrationPeriod();
        p.setId(5L);
        p.setName("Đợt chỉnh sửa");
        p.setPeriodType(PeriodType.NEW_ACADEMIC_YEAR);
        p.setAcademicYear("2026-2027");
        p.setOpenAt(LocalDateTime.now().minusDays(1));
        p.setCloseAt(LocalDateTime.now().plusDays(2));
        p.setTermStart(LocalDate.now());
        p.setTermEnd(LocalDate.now().plusMonths(5));
        p.setStatus(PeriodStatus.DRAFT);
        p.setCreatedBy(creator);
        p.setGenderScope(PeriodGenderScope.FEMALE_ONLY);
        p.setBuildings(Set.of(b));

        when(periodService.getByIdWithDetails(5L)).thenReturn(p);
        when(buildingRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(b));

        mockMvc.perform(get("/admin/periods/5/edit").with(user(createAdminUserDetails())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sửa đợt đăng ký")))
                .andExpect(content().string(containsString("Đợt chỉnh sửa")));
    }

    @Test
    void updatePeriod_success() throws Exception {
        RegistrationPeriod updated = new RegistrationPeriod();
        updated.setId(5L);
        updated.setName("Đợt đã cập nhật");

        when(periodService.update(eq(5L), any(RegistrationPeriodForm.class))).thenReturn(updated);

        mockMvc.perform(post("/admin/periods/5/edit")
                        .with(user(createAdminUserDetails()))
                        .with(csrf())
                        .param("name", "Đợt đã cập nhật")
                        .param("periodType", "SUMMER")
                        .param("academicYear", "2026-2027")
                        .param("openAt", "2026-06-01T08:00")
                        .param("closeAt", "2026-06-15T17:00")
                        .param("termStart", "2026-07-01")
                        .param("termEnd", "2026-08-31")
                        .param("genderScope", "MALE_ONLY")
                        .param("depositRatio", "0.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/periods"));

        verify(periodService).update(eq(5L), any(RegistrationPeriodForm.class));
    }
}
