package com.ktx.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.dto.ViolationForm;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.ViolationRepository;

@Service
public class ViolationService {

    public static final String STUDENT_NOT_FOUND = "Không tìm thấy sinh viên";
    public static final String USER_NOT_FOUND = "Không tìm thấy người dùng ghi nhận vi phạm";
    private static final int DEFAULT_CONDUCT_INITIAL = 100;
    private static final int DEFAULT_WARN_THRESHOLD = 50;

    private static final Map<ViolationType, ViolationPreset> TYPE_PRESETS = Map.of(
            ViolationType.LATE_RETURN, new ViolationPreset(ViolationSeverity.MINOR, 5, ViolationAction.WARNING),
            ViolationType.ILLEGAL_COOKING, new ViolationPreset(ViolationSeverity.MAJOR, 20, ViolationAction.POINT_DEDUCT),
            ViolationType.DISTURBANCE, new ViolationPreset(ViolationSeverity.MAJOR, 25, ViolationAction.POINT_DEDUCT),
            ViolationType.DAMAGE, new ViolationPreset(ViolationSeverity.SEVERE, 50, ViolationAction.TERMINATE));

    private final ViolationRepository violationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;

    public ViolationService(ViolationRepository violationRepository, StudentRepository studentRepository,
            UserRepository userRepository, SystemConfigRepository systemConfigRepository) {
        this.violationRepository = violationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<Violation> listAll() {
        return violationRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Violation> listByStudent(Long studentId) {
        return violationRepository.findByStudentIdWithDetails(studentId);
    }

    @Transactional
    public RecordViolationResult recordViolation(ViolationForm form, String recorderUsername) {
        Student student = studentRepository.findById(form.getStudentId())
                .orElseThrow(() -> new BusinessException(STUDENT_NOT_FOUND));
        User recorder = userRepository.findByUsername(recorderUsername)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        ViolationPreset preset = TYPE_PRESETS.get(form.getViolationType());
        ViolationSeverity severity = form.getSeverity() != null
                ? form.getSeverity()
                : (preset != null ? preset.getSeverity() : ViolationSeverity.MINOR);
        ViolationAction action = form.getAction() != null
                ? form.getAction()
                : (preset != null ? preset.getAction() : fallbackAction(severity));
        int pointsDeducted = resolvePointsDeducted(form.getPointsDeducted(), form.getViolationType(), severity);

        int previousScore = Optional.ofNullable(student.getConductScore()).orElse(getConductInitial());
        int currentScore = Math.max(0, previousScore - pointsDeducted);
        student.setConductScore(currentScore);

        boolean blockedAndSuggestTerminate = currentScore == 0 && severity == ViolationSeverity.SEVERE;
        if (blockedAndSuggestTerminate) {
            student.setBlockedFromHousing(true);
        }
        studentRepository.save(student);

        Violation violation = new Violation();
        violation.setStudent(student);
        violation.setRecordedBy(recorder);
        violation.setViolationType(form.getViolationType());
        violation.setSeverity(severity);
        violation.setPointsDeducted(pointsDeducted);
        violation.setAction(action);
        violation.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        violation.setOccurredAt(form.getOccurredAt() != null ? form.getOccurredAt() : LocalDateTime.now());
        violationRepository.save(violation);

        boolean warning = currentScore < getWarnThreshold();
        return new RecordViolationResult(violation, previousScore, currentScore, warning, blockedAndSuggestTerminate);
    }

    @Transactional
    public Student resetConductScore(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(STUDENT_NOT_FOUND));
        student.setConductScore(getConductInitial());
        student.setBlockedFromHousing(false);
        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public ViolationForm prefilledForm() {
        ViolationPreset preset = TYPE_PRESETS.get(ViolationType.LATE_RETURN);
        ViolationForm form = new ViolationForm();
        form.setViolationType(ViolationType.LATE_RETURN);
        form.setSeverity(preset.getSeverity());
        form.setPointsDeducted(preset.getPointsDeducted());
        form.setAction(preset.getAction());
        form.setOccurredAt(LocalDateTime.now().withSecond(0).withNano(0));
        return form;
    }

    @Transactional(readOnly = true)
    public Map<ViolationType, ViolationPreset> typePresets() {
        return TYPE_PRESETS;
    }

    @Transactional(readOnly = true)
    public int getWarnThreshold() {
        return readIntConfig("conduct.warn.threshold", DEFAULT_WARN_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public int getConductInitial() {
        return readIntConfig("conduct.initial", DEFAULT_CONDUCT_INITIAL);
    }

    private int resolvePointsDeducted(Integer rawPoints, ViolationType type, ViolationSeverity severity) {
        if (rawPoints != null) {
            return Math.max(0, rawPoints);
        }
        ViolationPreset preset = TYPE_PRESETS.get(type);
        if (preset != null) {
            return preset.getPointsDeducted();
        }
        return switch (severity) {
            case MINOR -> 5;
            case MAJOR -> 20;
            case SEVERE -> 50;
        };
    }

    private static ViolationAction fallbackAction(ViolationSeverity severity) {
        return severity == ViolationSeverity.SEVERE ? ViolationAction.TERMINATE : ViolationAction.POINT_DEDUCT;
    }

    private int readIntConfig(String key, int fallback) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException ex) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    public static class ViolationPreset {
        private final ViolationSeverity severity;
        private final int pointsDeducted;
        private final ViolationAction action;

        public ViolationPreset(ViolationSeverity severity, int pointsDeducted, ViolationAction action) {
            this.severity = severity;
            this.pointsDeducted = pointsDeducted;
            this.action = action;
        }

        public ViolationSeverity getSeverity() {
            return severity;
        }

        public int getPointsDeducted() {
            return pointsDeducted;
        }

        public ViolationAction getAction() {
            return action;
        }
    }

    public static class RecordViolationResult {
        private final Violation violation;
        private final int previousScore;
        private final int currentScore;
        private final boolean warning;
        private final boolean blockedAndSuggestTerminate;

        public RecordViolationResult(Violation violation, int previousScore, int currentScore,
                boolean warning, boolean blockedAndSuggestTerminate) {
            this.violation = violation;
            this.previousScore = previousScore;
            this.currentScore = currentScore;
            this.warning = warning;
            this.blockedAndSuggestTerminate = blockedAndSuggestTerminate;
        }

        public Violation getViolation() {
            return violation;
        }

        public int getPreviousScore() {
            return previousScore;
        }

        public int getCurrentScore() {
            return currentScore;
        }

        public boolean isWarning() {
            return warning;
        }

        public boolean isBlockedAndSuggestTerminate() {
            return blockedAndSuggestTerminate;
        }
    }
}
