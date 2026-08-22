package com.ktx.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.enums.BedStatus;
import com.ktx.repository.BedRepository;

@Service
public class BedService {

    public static final String NOT_FOUND = "Không tìm thấy giường";
    public static final String STALE = "Dữ liệu đã thay đổi";
    public static final String CANNOT_CHANGE_OCCUPIED = "Không đổi trạng thái giường đang có người";
    public static final String INVALID_STATUS = "Chỉ chuyển giữa trống và bảo trì";

    private final BedRepository bedRepository;

    public BedService(BedRepository bedRepository) {
        this.bedRepository = bedRepository;
    }

    @Transactional
    public Bed updateStatus(Long roomId, Long bedId, BedStatus status, Long version) {
        if (status != BedStatus.VACANT && status != BedStatus.MAINTENANCE) {
            throw new BusinessException(INVALID_STATUS);
        }
        Bed bed = bedRepository.findByIdAndRoomId(bedId, roomId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw new BusinessException(CANNOT_CHANGE_OCCUPIED);
        }
        if (version == null || !version.equals(bed.getVersion())) {
            throw new BusinessException(STALE);
        }
        bed.setStatus(status);
        try {
            return bedRepository.saveAndFlush(bed);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException(STALE);
        }
    }

    public static String statusLabel(BedStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case VACANT -> "Trống";
            case OCCUPIED -> "Đang ở";
            case MAINTENANCE -> "Bảo trì";
        };
    }

    public static String statusClass(BedStatus status) {
        if (status == null) {
            return "is-inactive";
        }
        return switch (status) {
            case VACANT -> "is-vacant";
            case OCCUPIED -> "is-occupied";
            case MAINTENANCE -> "is-maintenance";
        };
    }
}
