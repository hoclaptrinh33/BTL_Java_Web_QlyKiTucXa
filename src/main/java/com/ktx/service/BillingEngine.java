package com.ktx.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.ktx.domain.Invoice;

public interface BillingEngine {

    /**
     * Bậc thang điện (VND nguyên) - PR-12
     */
    long tieredElectricity(int kwh);

    /**
     * Phát hành hóa đơn điện nước phòng theo tháng - PR-12
     */
    List<Invoice> issueUtilityInvoices(long roomId, YearMonth month);

    /**
     * Phát hành hóa đơn tiền phòng kỳ theo hợp đồng
     */
    Invoice issueRoomFee(long contractId);

    /**
     * Phát hành hóa đơn tiền đặt cọc = 50% giá kỳ theo hợp đồng
     */
    Invoice issueDeposit(long contractId);

    /**
     * Phạt chậm nộp phí - PR-13
     */
    void applyLateFees(LocalDate today);
}
