package com.ktx.service;

import java.math.BigDecimal;
import java.util.List;

import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Payment;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;

public interface InvoiceService {

    /**
     * Hủy hóa đơn: status=CANCELLED và đổi idempotency_key = old + ":cancelled:" + id, cùng TX.
     */
    void cancel(long invoiceId, long actorId);

    Invoice getInvoiceById(long invoiceId);

    List<Invoice> getInvoicesByStudent(long studentId);

    List<Invoice> searchInvoices(InvoiceStatus status, InvoiceType type, String keyword);

    BigDecimal getTotalPaid(long invoiceId);

    BigDecimal getRemainingAmount(long invoiceId);

    List<InvoiceItem> getInvoiceItems(long invoiceId);

    List<Payment> getInvoicePayments(long invoiceId);
}
