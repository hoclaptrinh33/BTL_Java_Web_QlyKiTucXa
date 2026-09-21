package com.ktx.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.AllocationItem;
import com.ktx.domain.AllocationRun;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.enums.AllocationResult;
import com.ktx.domain.enums.AllocationRunStatus;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.dto.AllocationRunResult;
import com.ktx.repository.AllocationItemRepository;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.BedRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.AllocationEngine;
import com.ktx.service.AllocationService;
import com.ktx.service.ContractService;

@Service
public class AllocationServiceImpl implements AllocationService {

    private final AllocationEngine allocationEngine;
    private final AllocationRunRepository allocationRunRepository;
    private final AllocationItemRepository allocationItemRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ContractService contractService;
    private final BedRepository bedRepository;
    private final RoomApplicationRepository roomApplicationRepository;

    public AllocationServiceImpl(AllocationEngine allocationEngine,
                                 AllocationRunRepository allocationRunRepository,
                                 AllocationItemRepository allocationItemRepository,
                                 RegistrationPeriodRepository periodRepository,
                                 UserRepository userRepository,
                                 SystemConfigRepository systemConfigRepository,
                                 ContractService contractService,
                                 BedRepository bedRepository,
                                 RoomApplicationRepository roomApplicationRepository) {
        this.allocationEngine = allocationEngine;
        this.allocationRunRepository = allocationRunRepository;
        this.allocationItemRepository = allocationItemRepository;
        this.periodRepository = periodRepository;
        this.userRepository = userRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.contractService = contractService;
        this.bedRepository = bedRepository;
        this.roomApplicationRepository = roomApplicationRepository;
    }

    @Override
    @Transactional
    public AllocationRun previewAndStore(Long periodId, Long adminUserId) {
        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đợt đăng ký"));

        LocalDateTime startedAt = LocalDateTime.now();

        // 1. Chạy thuật toán phân bổ in-memory
        AllocationRunResult result = allocationEngine.plan(periodId);
        LocalDateTime finishedAt = LocalDateTime.now();

        // 2. Thu thập cấu hình trọng số để audit (weights_json)
        String policyStr = systemConfigRepository.findById("alloc.weight.policy")
                .map(SystemConfig::getConfigValue).orElse("1000");
        String remoteStr = systemConfigRepository.findById("alloc.weight.remote")
                .map(SystemConfig::getConfigValue).orElse("500");
        String prevGoodStr = systemConfigRepository.findById("alloc.weight.prev_good")
                .map(SystemConfig::getConfigValue).orElse("200");
        String mode = systemConfigRepository.findById("alloc.preference.mode")
                .map(SystemConfig::getConfigValue).orElse("SOFT");

        int policy = 1000;
        int remote = 500;
        int prevGood = 200;
        try {
            policy = Integer.parseInt(policyStr);
        } catch (NumberFormatException ignored) {}
        try {
            remote = Integer.parseInt(remoteStr);
        } catch (NumberFormatException ignored) {}
        try {
            prevGood = Integer.parseInt(prevGoodStr);
        } catch (NumberFormatException ignored) {}

        String weightsJson = serializeWeightsJson(policy, remote, prevGood, mode);

        // 3. Tính toán thống kê summary
        List<AllocationItem> items = result.getItems();
        long total = items != null ? items.size() : 0;
        long assigned = items != null ? items.stream().filter(i -> i.getResult() == AllocationResult.ASSIGNED).count() : 0;
        long waitlisted = items != null ? items.stream().filter(i -> i.getResult() == AllocationResult.WAITLISTED).count() : 0;
        long skipped = items != null ? items.stream().filter(i -> i.getResult() == AllocationResult.SKIPPED).count() : 0;

        String summaryJson = serializeSummaryJson(total, assigned, waitlisted, skipped);

        // 4. Tạo và lưu AllocationRun
        AllocationRun run = new AllocationRun();
        run.setPeriod(period);
        run.setDryRun(true);
        run.setStatus(AllocationRunStatus.COMPLETED);
        run.setStartedAt(startedAt);
        run.setFinishedAt(finishedAt);
        if (adminUserId != null) {
            userRepository.findById(adminUserId).ifPresent(run::setRunBy);
        }
        run.setWeightsJson(weightsJson);
        run.setSummaryJson(summaryJson);
        run.setSeedNote("Preview dry-run (không khóa giường, không tạo HĐ)");

        AllocationRun savedRun = allocationRunRepository.save(run);

        // 5. Lưu các AllocationItem gắn với AllocationRun vừa tạo
        if (items != null) {
            for (AllocationItem item : items) {
                item.setRun(savedRun);
            }
            allocationItemRepository.saveAll(items);
        }

        // Tuyệt đối không thay đổi beds, contracts và ApplicationStatus
        return savedRun;
    }

    @Override
    @Transactional(readOnly = true)
    public AllocationRun getRun(Long runId) {
        return allocationRunRepository.findById(runId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lượt phân bổ"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationItem> getRunItems(Long runId) {
        return allocationItemRepository.findByRunIdWithDetailsOrderByRankNoAsc(runId);
    }

    @Override
    @Transactional
    public void discardRun(Long runId) {
        AllocationRun run = getRun(runId);
        if (run.getStatus() == AllocationRunStatus.COMMITTED) {
            throw new BusinessException("Không thể hủy lượt phân bổ đã chốt (COMMITTED)");
        }
        run.setStatus(AllocationRunStatus.DISCARDED);
        allocationRunRepository.save(run);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationRun> getRunsByPeriod(Long periodId) {
        return allocationRunRepository.findByPeriodIdOrderByIdDesc(periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getAvailablePeriods() {
        return periodRepository.findAllWithCreator();
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationPeriod getPeriod(Long periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đợt đăng ký"));
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AllocationRun commit(Long periodId, Long adminUserId) {
        // 1. Khóa đợt bằng findByIdForUpdate (§6.3.6)
        RegistrationPeriod period = periodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đợt đăng ký #" + periodId));

        if (period.getStatus() != PeriodStatus.CLOSED && period.getStatus() != PeriodStatus.ALLOCATING) {
            throw new BusinessException("Chỉ có thể chốt phân bổ khi đợt ở trạng thái CLOSED hoặc ALLOCATING (Hiện tại: " + period.getStatus() + ")");
        }

        period.setStatus(PeriodStatus.ALLOCATING);
        periodRepository.save(period);

        LocalDateTime startedAt = LocalDateTime.now();

        // 2. Chạy lại thuật toán phân bổ với cấu hình sống (§6.3.2 quy tắc 10)
        AllocationRunResult result = allocationEngine.plan(periodId);
        LocalDateTime finishedAt = LocalDateTime.now();

        // 3. Thu thập cấu hình trọng số để audit (weights_json)
        String policyStr = systemConfigRepository.findById("alloc.weight.policy")
                .map(SystemConfig::getConfigValue).orElse("1000");
        String remoteStr = systemConfigRepository.findById("alloc.weight.remote")
                .map(SystemConfig::getConfigValue).orElse("500");
        String prevGoodStr = systemConfigRepository.findById("alloc.weight.prev_good")
                .map(SystemConfig::getConfigValue).orElse("200");
        String mode = systemConfigRepository.findById("alloc.preference.mode")
                .map(SystemConfig::getConfigValue).orElse("SOFT");

        int policy = 1000;
        int remote = 500;
        int prevGood = 200;
        try {
            policy = Integer.parseInt(policyStr);
        } catch (NumberFormatException ignored) {}
        try {
            remote = Integer.parseInt(remoteStr);
        } catch (NumberFormatException ignored) {}
        try {
            prevGood = Integer.parseInt(prevGoodStr);
        } catch (NumberFormatException ignored) {}

        String weightsJson = serializeWeightsJson(policy, remote, prevGood, mode);

        // 4. Thống kê summary
        List<AllocationItem> items = result.getItems();
        long total = items != null ? items.size() : 0;
        long assigned = items != null ? items.stream().filter(i -> i.getResult() == AllocationResult.ASSIGNED).count() : 0;
        long waitlisted = items != null ? items.stream().filter(i -> i.getResult() == AllocationResult.WAITLISTED).count() : 0;
        long skipped = items != null ? items.stream().filter(i -> i.getResult() == AllocationResult.SKIPPED).count() : 0;

        String summaryJson = serializeSummaryJson(total, assigned, waitlisted, skipped);

        // 5. Tạo AllocationRun chính thức (dryRun = false, status = COMMITTED)
        AllocationRun run = new AllocationRun();
        run.setPeriod(period);
        run.setDryRun(false);
        run.setStatus(AllocationRunStatus.COMMITTED);
        run.setStartedAt(startedAt);
        run.setFinishedAt(finishedAt);
        if (adminUserId != null) {
            userRepository.findById(adminUserId).ifPresent(run::setRunBy);
        }
        run.setWeightsJson(weightsJson);
        run.setSummaryJson(summaryJson);
        run.setSeedNote("Commit chính thức (Đã khóa giường, tạo HĐ DRAFT)");

        AllocationRun savedRun = allocationRunRepository.save(run);

        // 6. Khóa giường tăng dần theo ID để chống deadlock (§6.3.6 bước 5) và tạo HĐ DRAFT
        if (items != null && !items.isEmpty()) {
            List<Long> assignedBedIds = items.stream()
                    .filter(i -> i.getResult() == AllocationResult.ASSIGNED && i.getBed() != null)
                    .map(i -> i.getBed().getId())
                    .distinct()
                    .sorted()
                    .toList();

            if (!assignedBedIds.isEmpty()) {
                bedRepository.findByIdInForUpdate(assignedBedIds);
            }

            for (AllocationItem item : items) {
                item.setRun(savedRun);
                if (item.getResult() == AllocationResult.ASSIGNED && item.getBed() != null) {
                    contractService.createDraftFromAllocation(
                            item.getApplication(),
                            item.getBed(),
                            period.getTermStart(),
                            period.getTermEnd()
                    );
                    if (item.getApplication() != null) {
                        item.getApplication().setStatus(ApplicationStatus.ALLOCATED);
                        roomApplicationRepository.save(item.getApplication());
                    }
                } else if (item.getResult() == AllocationResult.WAITLISTED) {
                    if (item.getApplication() != null) {
                        item.getApplication().setStatus(ApplicationStatus.WAITLISTED);
                        roomApplicationRepository.save(item.getApplication());
                    }
                } else if (item.getResult() == AllocationResult.SKIPPED) {
                    if (item.getApplication() != null) {
                        item.getApplication().setStatus(ApplicationStatus.REJECTED);
                        roomApplicationRepository.save(item.getApplication());
                    }
                }
            }
            allocationItemRepository.saveAll(items);
        }

        // 7. Hoàn tất đợt: chuyển status sang COMPLETED
        period.setStatus(PeriodStatus.COMPLETED);
        periodRepository.save(period);

        return savedRun;
    }

    private String serializeWeightsJson(int policy, int remote, int prevGood, String mode) {
        String safeMode = mode != null ? mode.replace("\"", "\\\"") : "SOFT";
        return String.format(
                "{\"alloc.weight.policy\":%d,\"alloc.weight.remote\":%d,\"alloc.weight.prev_good\":%d,\"alloc.preference.mode\":\"%s\"}",
                policy, remote, prevGood, safeMode
        );
    }

    private String serializeSummaryJson(long total, long assigned, long waitlisted, long skipped) {
        return String.format(
                "{\"total\":%d,\"assigned\":%d,\"waitlisted\":%d,\"skipped\":%d}",
                total, assigned, waitlisted, skipped
        );
    }
}
