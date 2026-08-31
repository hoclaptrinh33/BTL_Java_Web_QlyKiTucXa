package com.ktx.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.dto.RegistrationPeriodForm;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;

@Service
public class RegistrationPeriodService {

    public static final String NOT_FOUND = "Không tìm thấy đợt đăng ký";
    public static final String DUPLICATE_OPEN_TYPE = "Đã có đợt cùng loại đang mở (OPEN)";
    public static final String INVALID_DATES = "Thời gian mở phải trước thời gian đóng";
    public static final String INVALID_TERM_DATES = "Ngày bắt đầu kỳ học phải trước ngày kết thúc";
    public static final String CANNOT_DELETE_NON_DRAFT = "Chỉ có thể xóa đợt đăng ký ở trạng thái Nháp (DRAFT)";
    public static final String CANNOT_DELETE_HAS_APPS = "Không thể xóa đợt đăng ký đã có đơn đăng ký";
    public static final String CANNOT_DELETE_HAS_RUNS = "Không thể xóa đợt đăng ký đã có lượt phân bổ";
    public static final String INVALID_TRANSITION = "Chuyển trạng thái không hợp lệ";

    private final RegistrationPeriodRepository periodRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final AllocationRunRepository allocationRunRepository;

    @Autowired
    public RegistrationPeriodService(RegistrationPeriodRepository periodRepository,
                                     RoomApplicationRepository roomApplicationRepository,
                                     AllocationRunRepository allocationRunRepository) {
        this.periodRepository = periodRepository;
        this.roomApplicationRepository = roomApplicationRepository;
        this.allocationRunRepository = allocationRunRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> listAll() {
        return periodRepository.findAllWithCreator();
    }

    @Transactional(readOnly = true)
    public RegistrationPeriod getById(Long id) {
        return periodRepository.findById(id)
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
    }
}
