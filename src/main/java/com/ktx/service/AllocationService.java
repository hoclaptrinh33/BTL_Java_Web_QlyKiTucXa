package com.ktx.service;

import java.util.List;

import com.ktx.domain.AllocationItem;
import com.ktx.domain.AllocationRun;
import com.ktx.domain.RegistrationPeriod;

public interface AllocationService {

    /**
     * Chạy thuật toán phân bổ (dry-run) và lưu lại kết quả preview (AllocationRun + AllocationItems)
     * Đảm bảo không thay đổi beds, contracts và ApplicationStatus.
     */
    AllocationRun previewAndStore(Long periodId, Long adminUserId);

    AllocationRun getRun(Long runId);

    List<AllocationItem> getRunItems(Long runId);

    void discardRun(Long runId);

    List<AllocationRun> getRunsByPeriod(Long periodId);

    List<RegistrationPeriod> getAvailablePeriods();

    RegistrationPeriod getPeriod(Long periodId);

    /**
     * Chốt phân bổ chính thức: tính lại engine, khóa giường (OCCUPIED), tạo HĐ DRAFT, cập nhật ApplicationStatus (§6.3.6)
     */
    AllocationRun commit(Long periodId, Long adminUserId);
}
