package com.ktx.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Contract;
import com.ktx.domain.Notification;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.NotificationType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.service.RenewalService;

@Component
public class ContractExpiryReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ContractExpiryReminderJob.class);

    public static final String CONFIG_KEY_EXPIRY_REMIND_DAYS = "contract.expiry.remind.days";
    public static final int DEFAULT_EXPIRY_REMIND_DAYS = 30;

    private final ContractRepository contractRepository;
    private final NotificationRepository notificationRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final RenewalService renewalService;

    public ContractExpiryReminderJob(ContractRepository contractRepository,
                                     NotificationRepository notificationRepository,
                                     SystemConfigRepository systemConfigRepository,
                                     RenewalService renewalService) {
        this.contractRepository = contractRepository;
        this.notificationRepository = notificationRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.renewalService = renewalService;
    }

    @Scheduled(cron = "${ktx.jobs.contract-expiry.cron:0 0 8 * * *}")
    public int remindExpiringContracts() {
        try {
            int expired = renewalService.processExpiredContractsAndRenewals(LocalDate.now());
            log.info("Đã chuyển {} hợp đồng sang EXPIRED", expired);
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý hợp đồng/gia hạn quá hạn: {}", ex.getMessage(), ex);
        }
        return remindExpiringContracts(LocalDate.now());
    }

    @Transactional
    public int remindExpiringContracts(LocalDate runDate) {
        if (runDate == null) {
            runDate = LocalDate.now();
        }

        int remindDays = getExpiryRemindDays();
        LocalDate targetEndDate = runDate.plusDays(remindDays);

        log.info("Bắt đầu job nhắc hết hạn HĐ cho ngày runDate={}, remindDays={}, targetEndDate={}",
                runDate, remindDays, targetEndDate);

        List<Contract> expiringContracts = contractRepository.findExpiringContracts(
                List.of(ContractStatus.ACTIVE), targetEndDate);

        LocalDateTime startOfDay = runDate.atStartOfDay();
        LocalDateTime endOfDay = runDate.plusDays(1).atStartOfDay();
        LocalDateTime createdAt = runDate.isEqual(LocalDate.now()) ? LocalDateTime.now() : runDate.atTime(8, 0);

        int createdCount = 0;
        for (Contract contract : expiringContracts) {
            if (contract.getStudent() == null || contract.getStudent().getUser() == null) {
                log.warn("Hợp đồng {} không có thông tin sinh viên hoặc User, bỏ qua", contract.getContractNo());
                continue;
            }

            Long userId = contract.getStudent().getUser().getId();
            String contractNo = contract.getContractNo();

            boolean alreadyNotified = notificationRepository.existsByUserIdAndTypeAndContractNoAndDate(
                    userId, NotificationType.CONTRACT_EXPIRY, contractNo, startOfDay, endOfDay);

            if (alreadyNotified) {
                log.info("Hợp đồng {} đã được tạo thông báo hết hạn trong ngày {}, bỏ qua (idempotent)",
                        contractNo, runDate);
                continue;
            }

            Notification notification = new Notification();
            notification.setUser(contract.getStudent().getUser());
            notification.setTitle("Thông báo hết hạn hợp đồng " + contractNo);
            notification.setBody("Hợp đồng số " + contractNo + " sẽ hết hạn vào ngày " + contract.getEndDate()
                    + ". Vui lòng nộp đơn gia hạn hoặc chuẩn bị thủ tục trả phòng theo quy định.");
            notification.setType(NotificationType.CONTRACT_EXPIRY);
            notification.setReadFlag(false);
            notification.setEmailSent(false);
            notification.setCreatedAt(createdAt);

            notificationRepository.save(notification);
            createdCount++;
            log.info("Đã tạo notification nhắc hết hạn cho hợp đồng {} (User ID: {})", contractNo, userId);
        }

        log.info("Hoàn tất job nhắc hết hạn HĐ: đã tạo {} thông báo mới", createdCount);
        return createdCount;
    }

    public int getExpiryRemindDays() {
        try {
            return systemConfigRepository.findById(CONFIG_KEY_EXPIRY_REMIND_DAYS)
                    .map(SystemConfig::getConfigValue)
                    .filter(val -> val != null && !val.isBlank())
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_EXPIRY_REMIND_DAYS);
        } catch (Exception e) {
            log.warn("Lỗi khi đọc cấu hình {}, dùng giá trị mặc định {}: {}",
                    CONFIG_KEY_EXPIRY_REMIND_DAYS, DEFAULT_EXPIRY_REMIND_DAYS, e.getMessage());
            return DEFAULT_EXPIRY_REMIND_DAYS;
        }
    }
}
