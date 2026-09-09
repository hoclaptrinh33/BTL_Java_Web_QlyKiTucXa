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
import com.ktx.dto.AllocationRunResult;
import com.ktx.repository.AllocationItemRepository;
import com.ktx.repository.AllocationRunRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.AllocationEngine;
import com.ktx.service.AllocationService;

@Service
public class AllocationServiceImpl implements AllocationService {

    private final AllocationEngine allocationEngine;
    private final AllocationRunRepository allocationRunRepository;
    private final AllocationItemRepository allocationItemRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;

    public AllocationServiceImpl(AllocationEngine allocationEngine,
                                 AllocationRunRepository allocationRunRepository,
                                 AllocationItemRepository allocationItemRepository,
                                 RegistrationPeriodRepository periodRepository,
                                 UserRepository userRepository,
                                 SystemConfigRepository systemConfigRepository) {
        this.allocationEngine = allocationEngine;
        this.allocationRunRepository = allocationRunRepository;
        this.allocationItemRepository = allocationItemRepository;
        this.periodRepository = periodRepository;
        this.userRepository = userRepository;
        this.systemConfigRepository = systemConfigRepository;
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
