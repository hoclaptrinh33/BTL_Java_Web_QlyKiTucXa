package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.Role;
import com.ktx.dto.PasswordChangeForm;
import com.ktx.dto.RegisterForm;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StaffRepository staffRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, studentRepository, staffRepository, ENCODER);
    }

    @Test
    void register_createsStudentOnly_withBcrypt10() {
        RegisterForm form = validForm();
        when(userRepository.existsByUsername("D22CQCN001")).thenReturn(false);
        when(studentRepository.existsByStudentCode("D22CQCN001")).thenReturn(false);
        when(userRepository.existsByEmail("sv@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(11L);
            return user;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(form);

        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("D22CQCN001", saved.getUsername());
        assertEquals("sv@example.com", saved.getEmail());
        assertTrue(saved.getEnabled());
        assertTrue(saved.getPasswordHash().startsWith("$2a$10$")
                || saved.getPasswordHash().startsWith("$2b$10$"));
        assertTrue(ENCODER.matches("password1", saved.getPasswordHash()));

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student student = studentCaptor.getValue();
        assertEquals("D22CQCN001", student.getStudentCode());
        assertEquals("Nguyen Van A", student.getFullName());
        assertEquals(Gender.MALE, student.getGender());
        assertEquals(PriorityCategory.NONE, student.getPriorityCategory());
        assertEquals(100, student.getConductScore());
        assertFalse(student.getPreviousStayGood());
        assertFalse(student.getBlockedFromHousing());
    }

    @Test
    void register_duplicateStudentCode_fieldErrorWithoutRole() {
        RegisterForm form = validForm();
        when(userRepository.existsByUsername("D22CQCN001")).thenReturn(true);

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class, () -> authService.register(form));

        assertEquals("studentCode", ex.getField());
        assertEquals(AuthService.DUPLICATE_STUDENT_CODE, ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("admin"));
        assertFalse(ex.getMessage().toLowerCase().contains("staff"));
        assertFalse(ex.getMessage().toLowerCase().contains("role"));
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_fieldErrorWithoutRole() {
        RegisterForm form = validForm();
        when(userRepository.existsByUsername("D22CQCN001")).thenReturn(false);
        when(studentRepository.existsByStudentCode("D22CQCN001")).thenReturn(false);
        when(userRepository.existsByEmail("sv@example.com")).thenReturn(true);

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class, () -> authService.register(form));

        assertEquals("email", ex.getField());
        assertEquals(AuthService.DUPLICATE_EMAIL, ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("admin"));
        assertFalse(ex.getMessage().toLowerCase().contains("role"));
        verify(userRepository, never()).save(any());
    }

    private static RegisterForm validForm() {
        RegisterForm form = new RegisterForm();
        form.setFullName("Nguyen Van A");
        form.setEmail("sv@example.com");
        form.setGender(Gender.MALE);
        form.setStudentCode("D22CQCN001");
        form.setPassword("password1");
        form.setConfirmPassword("password1");
        form.setTermsAccepted(true);
        return form;
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsIllegalArgumentException() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(ENCODER.encode("oldPassword"));

        when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));

        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword("wrongPassword");
        form.setNewPassword("newPassword");
        form.setConfirmPassword("newPassword");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("testuser", form));

        assertEquals("Mật khẩu hiện tại không chính xác", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_correctCredentials_savesHashedPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(ENCODER.encode("oldPassword"));

        when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword("oldPassword");
        form.setNewPassword("newPassword");
        form.setConfirmPassword("newPassword");

        authService.changePassword("testuser", form);

        assertTrue(ENCODER.matches("newPassword", user.getPasswordHash()));
        verify(userRepository).save(user);
    }
}
