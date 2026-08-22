package com.ktx.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Student;
import com.ktx.domain.enums.Gender;
import com.ktx.dto.StudentRow;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.StudentRepository;

@Service
public class StudentService {

    public static final String STAY_ALL = "all";
    public static final String STAY_OCCUPYING = "occupying";
    public static final String STAY_VACANT = "vacant";

    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;

    public StudentService(StudentRepository studentRepository, ContractRepository contractRepository) {
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentRow> list(String stay) {
        Set<Long> occupyingIds = new HashSet<>(
                contractRepository.findStudentIdsByStatusIn(OccupyingStatuses.OCCUPYING));
        String filter = stay == null ? STAY_ALL : stay;
        List<StudentRow> rows = new ArrayList<>();
        for (Student student : studentRepository.findAllWithUser()) {
            boolean occupying = occupyingIds.contains(student.getId());
            if (STAY_OCCUPYING.equals(filter) && !occupying) {
                continue;
            }
            if (STAY_VACANT.equals(filter) && occupying) {
                continue;
            }
            rows.add(toRow(student, occupying));
        }
        return rows;
    }

    private static StudentRow toRow(Student student, boolean occupying) {
        StudentRow row = new StudentRow();
        row.setId(student.getId());
        row.setStudentCode(student.getStudentCode());
        row.setFullName(student.getFullName());
        row.setInitials(DashboardService.initials(student.getFullName()));
        row.setGenderLabel(student.getGender() == Gender.MALE ? "Nam" : "Nữ");
        row.setFacultyCode(student.getFacultyCode());
        row.setClassCode(student.getClassCode());
        row.setPhone(student.getPhone());
        row.setPriorityLabel(DashboardService.priorityLabel(student.getPriorityCategory()));
        row.setPriorityTone(DashboardService.priorityTone(student.getPriorityCategory()));
        row.setConductScore(student.getConductScore() == null ? 0 : student.getConductScore());
        row.setOccupying(occupying);
        row.setBlocked(Boolean.TRUE.equals(student.getBlockedFromHousing()));
        row.setEnabled(student.getUser() != null && Boolean.TRUE.equals(student.getUser().getEnabled()));
        return row;
    }
}
