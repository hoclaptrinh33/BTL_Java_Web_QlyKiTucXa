package com.ktx.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.PeriodGenderScope;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.dto.RegistrationPeriodForm;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;

@Service
public class RegistrationPeriodService {

    public static final String NOT_FOUND = "Không tìm thấy đợt đăng ký";
    public static final String DUPLICATE_OPEN_TYPE = "Đã có đợt cùng loại đang mở (OPEN)";
    public static final String INVALID_DATES = "Thời gian mở phải trước thời gian đóng";
    public static final String INVALID_TERM_DATES = "Ngày bắt đầu kỳ học phải trước ngày kết thúc";
    public static final String INVALID_CHECKIN_DATES = "Ngày bắt đầu nhận phòng phải trước hoặc bằng ngày kết thúc nhận phòng";
    public static final String INVALID_PAYMENT_DEADLINE = "Hạn nộp cọc giữ chỗ phải sau thời gian đóng nhận đơn";
    public static final String CANNOT_DELETE_NON_DRAFT = "Chỉ có thể xóa đợt đăng ký ở trạng thái Nháp (DRAFT)";
    public static final String CANNOT_DELETE_HAS_APPS = "Không thể xóa đợt đăng ký đã có đơn đăng ký";
    public static final String CANNOT_DELETE_HAS_RUNS = "Không thể xóa đợt đăng ký đã có lượt phân bổ";
    public static final String INVALID_TRANSITION = "Chuyển trạng thái không hợp lệ";

    private final RegistrationPeriodRepository periodRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final AllocationRunRepository allocationRunRepository;
    private final BuildingRepository buildingRepository;

    @Autowired
    public RegistrationPeriodService(RegistrationPeriodRepository periodRepository,
                                     RoomApplicationRepository roomApplicationRepository,
                                     AllocationRunRepository allocationRunRepository,
                                     BuildingRepository buildingRepository) {
        this.periodRepository = periodRepository;
        this.roomApplicationRepository = roomApplicationRepository;
        this.allocationRunRepository = allocationRunRepository;
        this.buildingRepository = buildingRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> listAll() {
        return periodRepository.findAllWithCreator();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RegistrationPeriod> pageAll(org.springframework.data.domain.Pageable pageable) {
        return periodRepository.findAllWithCreator(pageable);
    }

    @Transactional(readOnly = true)
    public RegistrationPeriod getById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public RegistrationPeriod getByIdWithDetails(Long id) {
        return periodRepository.findByIdWithBuildingsAndCreator(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional
    public RegistrationPeriod create(RegistrationPeriodForm form, User creator) {
        validateDates(form);

        // Mặc định ban đầu trạng thái là DRAFT
        RegistrationPeriod period = new RegistrationPeriod();
        period.setName(form.getName().trim());
        period.setPeriodType(form.getPeriodType());
        period.setAcademicYear(form.getAcademicYear().trim());
        period.setOpenAt(form.getOpenAt());
        period.setCloseAt(form.getCloseAt());
        period.setTermStart(form.getTermStart());
        period.setTermEnd(form.getTermEnd());
        period.setStatus(PeriodStatus.DRAFT);
        period.setCreatedBy(creator);

        // Các trường nâng cấp
        period.setGenderScope(form.getGenderScope() != null ? form.getGenderScope() : PeriodGenderScope.ALL);
        period.setMinConductScore(form.getMinConductScore() != null ? form.getMinConductScore() : 0);
        period.setTargetCohort(form.getTargetCohort() != null ? form.getTargetCohort().trim() : null);
        period.setTargetQuota(form.getTargetQuota());
        period.setPaymentDeadline(form.getPaymentDeadline());
        period.setCheckinStart(form.getCheckinStart());
        period.setCheckinEnd(form.getCheckinEnd());
        period.setDepositRatio(form.getDepositRatio());
        period.setPaymentGuide(form.getPaymentGuide() != null ? form.getPaymentGuide().trim() : null);
        period.setRequireDocumentProof(Boolean.TRUE.equals(form.getRequireDocumentProof()));
        period.setTermsAndConditions(form.getTermsAndConditions() != null ? form.getTermsAndConditions().trim() : null);
        period.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        period.setContactPhone(form.getContactPhone() != null ? form.getContactPhone().trim() : null);
        period.setContactEmail(form.getContactEmail() != null ? form.getContactEmail().trim() : null);

        if (form.getBuildingIds() != null && !form.getBuildingIds().isEmpty()) {
            List<Building> selectedBuildings = buildingRepository.findAllById(form.getBuildingIds());
            period.setBuildings(new HashSet<>(selectedBuildings));
        } else {
            period.setBuildings(new HashSet<>());
        }

        return periodRepository.save(period);
    }

    @Transactional
    public RegistrationPeriod update(Long id, RegistrationPeriodForm form) {
        RegistrationPeriod period = getById(id);
        validateDates(form);

        // Nếu đợt đã mở hoặc đóng, có thể giới hạn chỉnh sửa một số trường hoặc kiểm tra các ràng buộc khác
        if (period.getStatus() == PeriodStatus.OPEN && form.getStatus() != PeriodStatus.OPEN) {
            // Khi chuyển trạng thái từ OPEN sang trạng thái khác qua form
            if (form.getStatus() == PeriodStatus.DRAFT) {
                throw new BusinessException(INVALID_TRANSITION);
            }
        }

        if (form.getStatus() == PeriodStatus.OPEN) {
            boolean hasDuplicate = periodRepository.existsByStatusAndPeriodTypeAndIdNot(
                    PeriodStatus.OPEN, form.getPeriodType(), id);
            if (hasDuplicate) {
                throw new BusinessException(DUPLICATE_OPEN_TYPE);
            }
        }

        period.setName(form.getName().trim());
        period.setPeriodType(form.getPeriodType());
        period.setAcademicYear(form.getAcademicYear().trim());
        period.setOpenAt(form.getOpenAt());
        period.setCloseAt(form.getCloseAt());
        period.setTermStart(form.getTermStart());
        period.setTermEnd(form.getTermEnd());
        if (form.getStatus() != null) {
            period.setStatus(form.getStatus());
        }

        // Các trường nâng cấp
        period.setGenderScope(form.getGenderScope() != null ? form.getGenderScope() : PeriodGenderScope.ALL);
        period.setMinConductScore(form.getMinConductScore() != null ? form.getMinConductScore() : 0);
        period.setTargetCohort(form.getTargetCohort() != null ? form.getTargetCohort().trim() : null);
        period.setTargetQuota(form.getTargetQuota());
        period.setPaymentDeadline(form.getPaymentDeadline());
        period.setCheckinStart(form.getCheckinStart());
        period.setCheckinEnd(form.getCheckinEnd());
        period.setDepositRatio(form.getDepositRatio());
        period.setPaymentGuide(form.getPaymentGuide() != null ? form.getPaymentGuide().trim() : null);
        period.setRequireDocumentProof(Boolean.TRUE.equals(form.getRequireDocumentProof()));
        period.setTermsAndConditions(form.getTermsAndConditions() != null ? form.getTermsAndConditions().trim() : null);
        period.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        period.setContactPhone(form.getContactPhone() != null ? form.getContactPhone().trim() : null);
        period.setContactEmail(form.getContactEmail() != null ? form.getContactEmail().trim() : null);

        if (form.getBuildingIds() != null) {
            List<Building> selectedBuildings = buildingRepository.findAllById(form.getBuildingIds());
            period.setBuildings(new HashSet<>(selectedBuildings));
        }

        return periodRepository.save(period);
    }

    @Transactional
    public void delete(Long id) {
        RegistrationPeriod period = getById(id);

        if (period.getStatus() != PeriodStatus.DRAFT) {
            throw new BusinessException(CANNOT_DELETE_NON_DRAFT);
        }
        if (roomApplicationRepository.existsByPeriodId(id)) {
            throw new BusinessException(CANNOT_DELETE_HAS_APPS);
        }
        if (allocationRunRepository.existsByPeriodId(id)) {
            throw new BusinessException(CANNOT_DELETE_HAS_RUNS);
        }

        periodRepository.delete(period);
    }

    @Transactional
    public RegistrationPeriod transitionToOpen(Long id) {
        RegistrationPeriod period = getById(id);

        if (period.getStatus() != PeriodStatus.DRAFT) {
            throw new BusinessException(INVALID_TRANSITION);
        }

        // Chặn trùng đợt cùng loại đang mở
        boolean hasDuplicate = periodRepository.existsByStatusAndPeriodType(
                PeriodStatus.OPEN, period.getPeriodType());
        if (hasDuplicate) {
            throw new BusinessException(DUPLICATE_OPEN_TYPE);
        }

        // Validate lại thời gian trước khi mở
        if (period.getOpenAt().isAfter(period.getCloseAt()) || period.getOpenAt().isEqual(period.getCloseAt())) {
            throw new BusinessException(INVALID_DATES);
        }
        if (period.getTermStart().isAfter(period.getTermEnd()) || period.getTermStart().isEqual(period.getTermEnd())) {
            throw new BusinessException(INVALID_TERM_DATES);
        }

        period.setStatus(PeriodStatus.OPEN);
        return periodRepository.save(period);
    }

    @Transactional
    public RegistrationPeriod transitionToClose(Long id) {
        RegistrationPeriod period = getById(id);

        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessException(INVALID_TRANSITION);
        }

        period.setStatus(PeriodStatus.CLOSED);
        return periodRepository.save(period);
    }

    private void validateDates(RegistrationPeriodForm form) {
        if (form.getOpenAt() != null && form.getCloseAt() != null) {
            if (form.getOpenAt().isAfter(form.getCloseAt()) || form.getOpenAt().isEqual(form.getCloseAt())) {
                throw new BusinessException(INVALID_DATES);
            }
        }
        if (form.getTermStart() != null && form.getTermEnd() != null) {
            if (form.getTermStart().isAfter(form.getTermEnd()) || form.getTermStart().isEqual(form.getTermEnd())) {
                throw new BusinessException(INVALID_TERM_DATES);
            }
        }
        if (form.getCheckinStart() != null && form.getCheckinEnd() != null) {
            if (form.getCheckinStart().isAfter(form.getCheckinEnd())) {
                throw new BusinessException(INVALID_CHECKIN_DATES);
            }
        }
        if (form.getPaymentDeadline() != null && form.getCloseAt() != null) {
            if (form.getPaymentDeadline().isBefore(form.getCloseAt())) {
                throw new BusinessException(INVALID_PAYMENT_DEADLINE);
            }
        }
    }
}
