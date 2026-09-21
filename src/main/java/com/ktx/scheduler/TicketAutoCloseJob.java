package com.ktx.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ktx.domain.SystemConfig;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.service.TicketService;

@Component
public class TicketAutoCloseJob {

    private static final Logger log = LoggerFactory.getLogger(TicketAutoCloseJob.class);

    private final TicketService ticketService;
    private final SystemConfigRepository systemConfigRepository;

    public TicketAutoCloseJob(TicketService ticketService, SystemConfigRepository systemConfigRepository) {
        this.ticketService = ticketService;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Scheduled(cron = "${ticket.autoclose.cron:0 0 8 * * *}")
    public int runAutoClose() {
        int days = 7;
        try {
            days = Integer.parseInt(systemConfigRepository.findById("ticket.autoclose.days")
                    .map(SystemConfig::getConfigValue).orElse("7"));
        } catch (Exception e) {
            log.warn("Không thể đọc config ticket.autoclose.days, dùng mặc định 7 ngày", e);
        }

        log.info("Bắt đầu TicketAutoCloseJob với số ngày quá hạn: {}", days);
        int closedCount = ticketService.autoCloseResolvedTickets(days);
        log.info("TicketAutoCloseJob hoàn thành: đã đóng {} ticket", closedCount);
        return closedCount;
    }
}
