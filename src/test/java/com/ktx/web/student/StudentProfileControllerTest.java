package com.ktx.web.student;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AuthService;
import com.ktx.service.StudentProfileService;

@WebMvcTest(controllers = StudentProfileController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StudentProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentProfileService studentProfileService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private com.ktx.security.LoginAttemptService loginAttemptService;

    @MockitoBean
    private com.ktx.repository.NotificationRepository notificationRepository;

    @Test
    void getProfileSucceedsForStudent() throws Exception {
        Student student = new Student();
        student.setStudentCode("D22CQCN001");
        student.setFullName("Nguyen Van A");
        student.setGender(Gender.MALE);
        student.setPriorityCategory(PriorityCategory.NONE);
        student.setPreviousStayGood(true);
        student.setConductScore(100);
        student.setBlockedFromHousing(false);

        User user = new User();
        user.setEmail("sv@example.com");
        student.setUser(user);

        when(studentProfileService.getStudentByUsername("D22CQCN001")).thenReturn(student);

        mockMvc.perform(get("/student/profile").with(user("D22CQCN001").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("D22CQCN001")))
                .andExpect(content().string(containsString("Nguyen Van A")))
                .andExpect(content().string(containsString("sv@example.com")));
    }

    @Test
    void updateProfileSucceedsWithValidData() throws Exception {
        mockMvc.perform(post("/student/profile").with(csrf()).with(user("D22CQCN001").roles("STUDENT"))
                        .param("phone", "0987654321")
                        .param("hometown", "Hanoi")
                        .param("emergencyName", "Nguyen Van B")
                        .param("emergencyPhone", "0912345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/profile"));
    }

    @Test
    void updateProfileFailsWithInvalidPhone() throws Exception {
        Student student = new Student();
        student.setStudentCode("D22CQCN001");
        student.setFullName("Nguyen Van A");
        student.setGender(Gender.MALE);
        student.setPriorityCategory(PriorityCategory.NONE);
        student.setPreviousStayGood(true);
        student.setConductScore(100);
        student.setBlockedFromHousing(false);

        User user = new User();
        user.setEmail("sv@example.com");
        student.setUser(user);

        when(studentProfileService.getStudentByUsername("D22CQCN001")).thenReturn(student);

        mockMvc.perform(post("/student/profile").with(csrf()).with(user("D22CQCN001").roles("STUDENT"))
                        .param("phone", "invalid-phone")
                        .param("hometown", "Hanoi")
                        .param("emergencyName", "Nguyen Van B")
                        .param("emergencyPhone", "0912345678"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Số điện thoại không hợp lệ")));
    }

    @Test
    void getPasswordPageSucceedsForStudent() throws Exception {
        mockMvc.perform(get("/student/password").with(user("D22CQCN001").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thay đổi mật khẩu")));
    }

    @Test
    void changePassword_wrongCurrentPassword_showsFieldError() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Mật khẩu hiện tại không chính xác"))
                .when(authService).changePassword(org.mockito.Mockito.eq("D22CQCN001"), any(com.ktx.dto.PasswordChangeForm.class));

        mockMvc.perform(post("/student/password").with(csrf()).with(user("D22CQCN001").roles("STUDENT"))
                        .param("currentPassword", "wrongPass")
                        .param("newPassword", "newPass123")
                        .param("confirmPassword", "newPass123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu hiện tại không chính xác")));
    }

    @Test
    void changePassword_correctCredentials_redirectsToPasswordPage() throws Exception {
        mockMvc.perform(post("/student/password").with(csrf()).with(user("D22CQCN001").roles("STUDENT"))
                        .param("currentPassword", "oldPass")
                        .param("newPassword", "newPass123")
                        .param("confirmPassword", "newPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/password"));
    }
}
