package com.ktx.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ktx.common.exception.NotFoundException;
import com.ktx.domain.Student;
import com.ktx.dto.StudentProfileForm;
import com.ktx.repository.StudentRepository;

@Service
public class StudentProfileService {

    private final StudentRepository studentRepository;

    public StudentProfileService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public Student getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sinh viên có tài khoản: " + username));
    }

    @Transactional
    public void updateStudentProfile(String username, StudentProfileForm form) {
        Student student = getStudentByUsername(username);

        student.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
        student.setEmergencyName(StringUtils.hasText(form.getEmergencyName()) ? form.getEmergencyName().trim() : null);
        student.setEmergencyPhone(StringUtils.hasText(form.getEmergencyPhone()) ? form.getEmergencyPhone().trim() : null);
        student.setHometown(StringUtils.hasText(form.getHometown()) ? form.getHometown().trim() : null);

        studentRepository.save(student);
    }
}
