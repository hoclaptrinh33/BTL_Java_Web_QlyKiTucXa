package com.ktx.scheduler;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ktx.service.BillingEngine;

@Component
public class InvoiceOverdueJob {

    private static final Logger log = LoggerFactory.getLogger(InvoiceOverdueJob.class);

    private final BillingEngine billingEngine;

    public InvoiceOverdueJob(BillingEngine billingEngine) {
        this.billingEngine = billingEngine;
    }

    /**
     * Quét các hóa đơn UNPAID quá hạn lúc 1:00 sáng hàng đêm, chuyển sang OVERDUE
     * và tính phí phạt chậm nộp 5% (idempotent, chỉ áp 1 lần).
     */
    @Scheduled(cron = "${billing.overdue.cron:0 0 1 * * ?}")
    public void scanOverdueInvoices() {
        log.info("Bắt đầu chạy InvoiceOverdueJob quét hóa đơn quá hạn...");
        try {
            billingEngine.applyLateFees(LocalDate.now());
            log.info("InvoiceOverdueJob hoàn tất thành công.");
        } catch (Exception ex) {
            log.error("Lỗi khi thực thi InvoiceOverdueJob: {}", ex.getMessage(), ex);
        }
    }
}
