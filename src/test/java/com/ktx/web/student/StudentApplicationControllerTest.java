package com.ktx.web.student;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.Role;
import com.ktx.dto.BuildingGalleryDto;
import com.ktx.dto.RoomTypeGalleryDto;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.MediaGalleryService;
import com.ktx.service.RoomApplicationService;

@WebMvcTest(controllers = StudentApplicationController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StudentApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomApplicationService roomApplicationService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private RegistrationPeriodRepository periodRepository;

    @MockitoBean
    private BuildingRepository buildingRepository;

    @MockitoBean
    private ContractRepository contractRepository;

    @MockitoBean
    private MediaGalleryService mediaGalleryService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void testRenderList() throws Exception {
        User u = new User();
        u.setId(50L);
        u.setUsername("sv001");
        u.setEmail("sv001@ktx.vn");
        u.setPasswordHash("hashed");
        u.setRole(Role.STUDENT);
        u.setEnabled(true);
        KtxUserDetails details = new KtxUserDetails(u);

        Student s = new Student();
        s.setId(5L);
        s.setFullName("Nguyen Van A");
        s.setStudentCode("B22DCCN001");
        s.setGender(Gender.MALE);

        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));
        when(userRepository.findByUsernameOrEmail("sv001", "sv001")).thenReturn(Optional.of(u));
        when(roomApplicationService.getStudentApplications(5L)).thenReturn(Collections.emptyList());
        when(periodRepository.findAllWithCreator()).thenReturn(Collections.emptyList());

        BuildingGalleryDto bg = new BuildingGalleryDto();
        bg.setId(1L);
        bg.setCode("A");
        bg.setName("Tòa A");
        bg.setTotalImages(2);
        bg.setPrimaryImageUrl("/img.jpg");
        when(mediaGalleryService.getBuildingGalleries(any())).thenReturn(List.of(bg));

        RoomTypeGalleryDto rg = new RoomTypeGalleryDto();
        rg.setTypeCode("STANDARD_4");
        rg.setTitle("Phòng 4");
        rg.setPrimaryImageUrl("/room.jpg");
        when(mediaGalleryService.getRoomTypeGalleries()).thenReturn(List.of(rg));

        mockMvc.perform(get("/student/applications").with(user(details)))
                .andExpect(status().isOk());
    }

    @Test
    void testRenderCreateForm() throws Exception {
        User u = new User();
        u.setId(50L);
        u.setUsername("sv001");
        u.setEmail("sv001@ktx.vn");
        u.setPasswordHash("hashed");
        u.setRole(Role.STUDENT);
        u.setEnabled(true);
        KtxUserDetails details = new KtxUserDetails(u);

        Student s = new Student();
        s.setId(5L);
        s.setFullName("Nguyen Van A");
        s.setStudentCode("B22DCCN001");
        s.setGender(Gender.MALE);
        s.setConductScore(100);
        s.setBlockedFromHousing(false);

        RegistrationPeriod period = new RegistrationPeriod();
        period.setId(10L);
        period.setName("Đợt 1");
        period.setStatus(com.ktx.domain.enums.PeriodStatus.OPEN);
        period.setPeriodType(com.ktx.domain.enums.PeriodType.FRESHMAN);

        when(studentRepository.findByUserUsername("sv001")).thenReturn(Optional.of(s));
        when(userRepository.findByUsernameOrEmail("sv001", "sv001")).thenReturn(Optional.of(u));
        when(contractRepository.existsByStudentIdAndStatusIn(any(), any())).thenReturn(false);
        when(periodRepository.findAllWithCreator()).thenReturn(List.of(period));
        when(roomApplicationService.getStudentApplications(5L)).thenReturn(Collections.emptyList());

        BuildingGalleryDto bg = new BuildingGalleryDto();
        bg.setId(1L);
        bg.setCode("A");
        bg.setName("Tòa A");
        bg.setTotalImages(2);
        bg.setPrimaryImageUrl("/img.jpg");
        when(mediaGalleryService.getBuildingGalleries(any())).thenReturn(List.of(bg));

        RoomTypeGalleryDto rg = new RoomTypeGalleryDto();
        rg.setTypeCode("STANDARD_4");
        rg.setTitle("Phòng 4");
        rg.setPrimaryImageUrl("/room.jpg");
        when(mediaGalleryService.getRoomTypeGalleries()).thenReturn(List.of(rg));

        mockMvc.perform(get("/student/applications/new").with(user(details)))
                .andExpect(status().isOk());
    }
}
