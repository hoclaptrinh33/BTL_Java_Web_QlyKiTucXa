package com.ktx.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.Role;
import com.ktx.dto.RegisterForm;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;

@Service
public class AuthService {

    public static final String DUPLICATE_STUDENT_CODE = "Mã sinh viên đã được sử dụng";
    public static final String DUPLICATE_EMAIL = "Email đã được sử dụng";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        String studentCode = form.getStudentCode().trim();
        String email = form.getEmail().trim();

        if (userRepository.existsByUsername(studentCode) || studentRepository.existsByStudentCode(studentCode)) {
            throw new DuplicateFieldException("studentCode", DUPLICATE_STUDENT_CODE);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateFieldException("email", DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(studentCode);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(studentCode);
        student.setFullName(form.getFullName().trim());
        student.setGender(form.getGender());
        student.setDateOfBirth(form.getDateOfBirth());
        student.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
        student.setPriorityCategory(PriorityCategory.NONE);
        student.setPreviousStayGood(false);
        student.setConductScore(100);
        student.setBlockedFromHousing(false);
        studentRepository.save(student);

        return user;
    }
}
