package com.ktx.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Notification;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.NotificationType;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.ViolationRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.ConductService;
import com.ktx.service.ViolationRule;

@Service
@Transactional
public class ConductServiceImpl implements ConductService {

    private final ViolationRepository violationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final NotificationRepository notificationRepository;
    private final StaffScope staffScope;

    public ConductServiceImpl(ViolationRepository violationRepository,
                              StudentRepository studentRepository,
                              UserRepository userRepository,
                              ContractRepository contractRepository,
                              SystemConfigRepository systemConfigRepository,
                              NotificationRepository notificationRepository,
                              StaffScope staffScope) {
        this.violationRepository = violationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.notificationRepository = notificationRepository;
        this.staffScope = staffScope;
    }

    @Override
    public Violation recordViolation(Long studentId,
                                     String recordedByUsername,
                                     ViolationType type,
                                     ViolationSeverity severity,
                                     Integer pointsDeducted,
                                     ViolationAction action,
                                     String description,
                                     LocalDateTime occurredAt,
                                     Authentication auth) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên"));

        User recordedBy = userRepository.findByUsername(recordedByUsername)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản người ghi nhận"));

        // Kiểm tra StaffScope nếu người dùng là STAFF
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                studentId, OccupyingStatuses.OCCUPYING);
        Long studentBuildingId = contracts.isEmpty() ? null : contracts.get(0).getBed().getRoom().getBuilding().getId();
        if (studentBuildingId != null) {
            staffScope.assertBuilding(auth, studentBuildingId);
        } else {
            // Sinh viên chưa có phòng hoặc không thuộc tòa nào
            staffScope.assertBuilding(auth, -1L);
        }

        if (severity == null) {
            severity = ViolationRule.getDefaultSeverity(type);
        }

        if (pointsDeducted == null || pointsDeducted < 0) {
            pointsDeducted = ViolationRule.getDefaultPoints(type, severity);
        }

        int currentScore = student.getConductScore() != null ? student.getConductScore() : 100;
        int newScore = Math.max(0, currentScore - pointsDeducted);
        student.setConductScore(newScore);

        if (action == null) {
            action = ViolationRule.getDefaultAction(type, severity, newScore);
        }

        // 0 điểm hoặc SEVERE + TERMINATE -> blocked_from_housing = true
        if (newScore == 0 || (severity == ViolationSeverity.SEVERE && action == ViolationAction.TERMINATE)) {
            student.setBlockedFromHousing(true);
        }

        // Cảnh báo nếu điểm rơi xuống dưới ngưỡng (< 50)
        int warnThreshold = 50;
        try {
            warnThreshold = Integer.parseInt(systemConfigRepository.findById("conduct.warn.threshold")
                    .map(SystemConfig::getConfigValue).orElse("50"));
        } catch (Exception ignored) {
        }

        if (newScore < warnThreshold && student.getUser() != null) {
            Notification n = new Notification();
            n.setUser(student.getUser());
            n.setTitle("Cảnh báo điểm rèn luyện ký túc xá");
            n.setBody("Điểm rèn luyện của bạn hiện còn " + newScore + " điểm (dưới ngưỡng " + warnThreshold + " điểm).");
            n.setType(NotificationType.GENERIC);
            n.setReadFlag(false);
            n.setCreatedAt(LocalDateTime.now());
            n.setEmailSent(false);
            notificationRepository.save(n);
        }

        Violation violation = new Violation();
        violation.setStudent(student);
        violation.setRecordedBy(recordedBy);
        violation.setViolationType(type != null ? type : ViolationType.OTHER);
        violation.setSeverity(severity);
        violation.setPointsDeducted(pointsDeducted);
        violation.setDescription(description != null ? description.trim() : null);
        violation.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
        violation.setAction(action);

        studentRepository.save(student);
        return violationRepository.save(violation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Violation> getViolationsForStudent(Long studentId) {
        return violationRepository.findByStudentIdWithDetails(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Violation> getViolationsForStaff(Authentication auth) {
        Optional<Long> buildingIdOpt = staffScope.buildingId(auth);
        if (buildingIdOpt.isPresent()) {
            return violationRepository.findByBuildingIdWithDetails(buildingIdOpt.get(), OccupyingStatuses.OCCUPYING);
        }
        return violationRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Violation> getViolationsForAdmin(Long buildingId) {
        if (buildingId != null) {
            return violationRepository.findByBuildingIdWithDetails(buildingId, OccupyingStatuses.OCCUPYING);
        }
        return violationRepository.findAllWithDetails();
    }

    @Override
    public void resetAllConductScores() {
        int initialScore = 100;
        try {
            initialScore = Integer.parseInt(systemConfigRepository.findById("conduct.initial")
                    .map(SystemConfig::getConfigValue).orElse("100"));
        } catch (Exception ignored) {
        }

        List<Student> students = studentRepository.findAll();
        for (Student s : students) {
            s.setConductScore(initialScore);
        }
        studentRepository.saveAll(students);
    }

    @Override
    public void resetConductScoreForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên"));
        int initialScore = 100;
        try {
            initialScore = Integer.parseInt(systemConfigRepository.findById("conduct.initial")
                    .map(SystemConfig::getConfigValue).orElse("100"));
        } catch (Exception ignored) {
        }
        student.setConductScore(initialScore);
        studentRepository.save(student);
    }
}
