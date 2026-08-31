package com.ktx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.SystemConfigRepository;

@Service
public class RoomApplicationService {

    public static final String NOT_FOUND = "Không tìm thấy đơn đăng ký nguyện vọng";
    public static final String PERIOD_NOT_FOUND = "Không tìm thấy đợt đăng ký";
    public static final String STUDENT_NOT_FOUND = "Không tìm thấy thông tin sinh viên";
    public static final String BUILDING_NOT_FOUND = "Không tìm thấy tòa nguyện vọng";
    public static final String PERIOD_NOT_OPEN = "Đợt đăng ký không trong thời gian mở nhận đơn";
    public static final String STUDENT_BLOCKED = "Sinh viên bị cấm đăng ký chỗ ở";
    public static final String CONDUCT_SCORE_ZERO = "Điểm rèn luyện bằng 0 không được phép nộp đơn";
    public static final String ALREADY_SUBMITTED = "Sinh viên đã nộp đơn cho đợt đăng ký này";
    public static final String ACTIVE_CONTRACT_EXISTS = "Sinh viên đang có hợp đồng hoạt động giữ giường hiệu lực";
    public static final String GENDER_MISMATCH = "Giới tính sinh viên không khớp với tòa nguyện vọng";
    public static final String CANNOT_WITHDRAW_CLOSED = "Không thể rút đơn khi đợt đăng ký đã đóng";
    public static final String ACCESS_DENIED = "Bạn không có quyền thao tác trên đơn đăng ký này";

    private final RoomApplicationRepository roomApplicationRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Autowired
    public RoomApplicationService(RoomApplicationRepository roomApplicationRepository,
                                  RegistrationPeriodRepository periodRepository,
                                  StudentRepository studentRepository,
                                  BuildingRepository buildingRepository,
                                  ContractRepository contractRepository,
                                  SystemConfigRepository systemConfigRepository) {
        this.roomApplicationRepository = roomApplicationRepository;
        this.periodRepository = periodRepository;
        this.studentRepository = studentRepository;
        this.buildingRepository = buildingRepository;
        this.contractRepository = contractRepository;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomApplication> getStudentApplications(Long studentId) {
        return roomApplicationRepository.findByStudentIdOrderByPeriodOpenAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public RoomApplication getById(Long id) {
        return roomApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<RoomApplication> listAllByPeriod(Long periodId) {
        return roomApplicationRepository.findByPeriodIdWithDetails(periodId);
    }

    @Transactional
    public RoomApplication submitApplication(Long studentId, Long periodId, Long preferredBuildingId, RoomType preferredRoomType, String note) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(STUDENT_NOT_FOUND));
        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException(PERIOD_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (period.getStatus() != PeriodStatus.OPEN || now.isBefore(period.getOpenAt()) || now.isAfter(period.getCloseAt())) {
            throw new BusinessException(PERIOD_NOT_OPEN);
        }

        if (Boolean.TRUE.equals(student.getBlockedFromHousing())) {
            throw new BusinessException(STUDENT_BLOCKED);
        }

        if (student.getConductScore() == null || student.getConductScore() <= 0) {
            throw new BusinessException(CONDUCT_SCORE_ZERO);
        }

        if (roomApplicationRepository.existsByPeriodIdAndStudentId(periodId, studentId)) {
            throw new BusinessException(ALREADY_SUBMITTED);
        }

        if (contractRepository.existsByStudentIdAndStatusIn(studentId, OccupyingStatuses.OCCUPYING)) {
            throw new BusinessException(ACTIVE_CONTRACT_EXISTS);
        }

        Building preferredBuilding = null;
        if (preferredBuildingId != null) {
            preferredBuilding = buildingRepository.findById(preferredBuildingId)
                    .orElseThrow(() -> new BusinessException(BUILDING_NOT_FOUND));
            
            BuildingGenderPolicy policy = preferredBuilding.getGenderPolicy();
            if ((student.getGender() == Gender.MALE && policy != BuildingGenderPolicy.MALE) ||
                (student.getGender() == Gender.FEMALE && policy != BuildingGenderPolicy.FEMALE)) {
                throw new BusinessException(GENDER_MISMATCH);
            }
        }

        RoomApplication app = new RoomApplication();
        app.setPeriod(period);
        app.setStudent(student);
        app.setPreferredBuilding(preferredBuilding);
        app.setPreferredRoomType(preferredRoomType);
        app.setNote(note != null ? note.trim() : null);

        // Chụp snapshot thông tin ưu tiên của sinh viên
        app.setPrioritySnapshot(student.getPriorityCategory());
        app.setPreviousStayGoodSnapshot(Boolean.TRUE.equals(student.getPreviousStayGood()));
        
        // Tính điểm nguyện vọng dựa trên snapshot
        app.setComputedScore(computeScore(student.getPriorityCategory(), Boolean.TRUE.equals(student.getPreviousStayGood())));
        
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(now);

        return roomApplicationRepository.save(app);
    }

    @Transactional
    public RoomApplication withdrawApplication(Long applicationId, Long studentId) {
        RoomApplication app = getById(applicationId);

        if (!app.getStudent().getId().equals(studentId)) {
            throw new BusinessException(ACCESS_DENIED);
        }

        if (app.getPeriod().getStatus() == PeriodStatus.CLOSED) {
            throw new BusinessException(CANNOT_WITHDRAW_CLOSED);
        }

        app.setStatus(ApplicationStatus.WITHDRAWN);
        return roomApplicationRepository.save(app);
    }

    @Transactional
    public RoomApplication rejectApplication(Long id) {
        RoomApplication app = getById(id);
        if (app.getPeriod().getStatus() != PeriodStatus.CLOSED && app.getPeriod().getStatus() != PeriodStatus.OPEN) {
            throw new BusinessException("Chỉ có thể từ chối đơn ở đợt đang mở hoặc đã đóng");
        }
        app.setStatus(ApplicationStatus.REJECTED);
        return roomApplicationRepository.save(app);
    }

    private int computeScore(PriorityCategory priority, boolean prevGood) {
        int score = 0;
        int policyWeight = 1000;
        int remoteWeight = 500;
        int prevGoodWeight = 200;

        try {
            policyWeight = Integer.parseInt(systemConfigRepository.findById("alloc.weight.policy")
                    .map(SystemConfig::getConfigValue).orElse("1000"));
            remoteWeight = Integer.parseInt(systemConfigRepository.findById("alloc.weight.remote")
                    .map(SystemConfig::getConfigValue).orElse("500"));
            prevGoodWeight = Integer.parseInt(systemConfigRepository.findById("alloc.weight.prev_good")
                    .map(SystemConfig::getConfigValue).orElse("200"));
        } catch (Exception e) {
            // fallback
        }

        if (priority == PriorityCategory.POLICY) {
            score += policyWeight;
        } else if (priority == PriorityCategory.REMOTE_AREA) {
            score += remoteWeight;
        }

        if (prevGood) {
            score += prevGoodWeight;
        }

        return score;
    }
}
