package com.ktx.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.common.exception.NotFoundException;
import com.ktx.domain.Staff;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.Role;
import com.ktx.dto.AdminProfileForm;
import com.ktx.dto.PasswordChangeForm;
import com.ktx.dto.RegisterForm;
import com.ktx.dto.StaffProfileForm;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;

@Service
public class AuthService {

    public static final String DUPLICATE_STUDENT_CODE = "Mã sinh viên đã được sử dụng";
    public static final String DUPLICATE_EMAIL = "Email đã được sử dụng";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, StudentRepository studentRepository,
            StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
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

    @Transactional
    public void changePassword(String username, PasswordChangeForm form) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản: " + username));

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getAdminProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản quản trị: " + username));
    }

    @Transactional
    public void updateAdminProfile(String username, AdminProfileForm form) {
        User user = getAdminProfile(username);
        String newEmail = form.getEmail().trim();

        if (!user.getEmail().equalsIgnoreCase(newEmail)) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new DuplicateFieldException("email", DUPLICATE_EMAIL);
            }
            user.setEmail(newEmail);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Staff getStaffProfile(String username) {
        return staffRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin cán bộ: " + username));
    }

    @Transactional
    public void updateStaffProfile(String username, StaffProfileForm form) {
        Staff staff = getStaffProfile(username);

        staff.setFullName(form.getFullName().trim());
        staff.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);

        staff.getUser().setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
    }
}
