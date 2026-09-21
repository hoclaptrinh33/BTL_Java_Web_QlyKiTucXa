package com.ktx.service;

import java.math.BigDecimal;
import java.util.List;

import com.ktx.domain.Payment;
import com.ktx.domain.enums.PaymentMethod;

public interface PaymentService {

    /**
     * Ghi nhận thanh toán hóa đơn (hỗ trợ trả góp nhiều lần).
     * Khi tổng số tiền đã đóng đủ tổng tiền hóa đơn, trạng thái hóa đơn chuyển sang PAID.
     */
    Payment recordPayment(Long invoiceId, BigDecimal amount, PaymentMethod method, String referenceNo, Long actorId);

    List<Payment> searchPayments(String keyword);

    List<Payment> getPaymentsByInvoice(Long invoiceId);
}
