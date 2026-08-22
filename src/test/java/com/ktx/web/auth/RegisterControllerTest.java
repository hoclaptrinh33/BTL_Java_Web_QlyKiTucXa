package com.ktx.web.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.User;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.Role;
import com.ktx.dto.RegisterForm;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.service.AuthService;
import com.ktx.service.BuildingService;

@WebMvcTest
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BuildingService buildingService;

    @Test
    void registerPageIsPermittedAndHasCsrf() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Đăng ký tài khoản")))
                .andExpect(content().string(containsString("Giới tính")))
                .andExpect(content().string(not(containsString("name=\"role\""))));
    }

    @Test
    void registerGoogleIsPermitted() throws Exception {
        mockMvc.perform(get("/register/google"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?google"));
    }

    @Test
    void authenticatedUserIsRedirectedFromRegister() throws Exception {
        mockMvc.perform(get("/register").with(user("sv").roles("STUDENT")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void registerIgnoresAdminRoleAndCreatesStudent() throws Exception {
        User user = new User();
        user.setRole(Role.STUDENT);
        when(authService.register(any(RegisterForm.class))).thenReturn(user);

        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Nguyen Van A")
                        .param("email", "sv@example.com")
                        .param("gender", "MALE")
                        .param("studentCode", "D22CQCN001")
                        .param("password", "password1")
                        .param("confirmPassword", "password1")
                        .param("termsAccepted", "true")
                        .param("role", "ADMIN"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<RegisterForm> captor = ArgumentCaptor.forClass(RegisterForm.class);
        verify(authService).register(captor.capture());
        RegisterForm form = captor.getValue();
        assertEquals("D22CQCN001", form.getStudentCode());
        assertEquals(Gender.MALE, form.getGender());
    }

    @Test
    void duplicateEmailShowsFieldErrorWithoutRole() throws Exception {
        when(authService.register(any(RegisterForm.class)))
                .thenThrow(new DuplicateFieldException("email", AuthService.DUPLICATE_EMAIL));

        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Nguyen Van A")
                        .param("email", "admin@ktx.local")
                        .param("gender", "FEMALE")
                        .param("studentCode", "D22CQCN002")
                        .param("password", "password1")
                        .param("confirmPassword", "password1")
                        .param("termsAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email đã được sử dụng")))
                .andExpect(content().string(not(containsString("ADMIN"))))
                .andExpect(content().string(not(containsString("role khác"))));
    }

    @Test
    void shortPasswordDoesNotCallService() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Nguyen Van A")
                        .param("email", "sv@example.com")
                        .param("gender", "MALE")
                        .param("studentCode", "D22CQCN001")
                        .param("password", "short")
                        .param("confirmPassword", "short")
                        .param("termsAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu tối thiểu 8 ký tự")));

        verify(authService, never()).register(any());
    }
}
