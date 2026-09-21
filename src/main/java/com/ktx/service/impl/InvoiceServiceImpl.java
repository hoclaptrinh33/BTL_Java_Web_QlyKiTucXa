package com.ktx.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Payment;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.PaymentRepository;
import com.ktx.service.InvoiceService;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceItemRepository invoiceItemRepository,
                              PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void cancel(long invoiceId, long actorId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn #" + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Hóa đơn #" + invoice.getInvoiceNo() + " đã bị hủy trước đó");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Không thể hủy hóa đơn #" + invoice.getInvoiceNo() + " đã thanh toán hoàn tất");
        }

        BigDecimal totalPaid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        if (totalPaid != null && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Không thể hủy hóa đơn #" + invoice.getInvoiceNo() + " đã có khoản thanh toán được ghi nhận");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        String currentKey = invoice.getIdempotencyKey();
        if (currentKey != null && !currentKey.contains(":cancelled:")) {
            invoice.setIdempotencyKey(currentKey + ":cancelled:" + invoice.getId());
        }
        invoiceRepository.save(invoice);
    }

    @Override
    public Invoice getInvoiceById(long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn #" + invoiceId));
    }

    @Override
    public List<Invoice> getInvoicesByStudent(long studentId) {
        return invoiceRepository.findByStudentIdOrderByDueDateDesc(studentId);
    }

    @Override
    public List<Invoice> searchInvoices(InvoiceStatus status, InvoiceType type, String keyword) {
        String trimmed = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return invoiceRepository.searchInvoices(status, type, trimmed);
    }

    @Override
    public BigDecimal getTotalPaid(long invoiceId) {
        BigDecimal sum = paymentRepository.sumAmountByInvoiceId(invoiceId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getRemainingAmount(long invoiceId) {
        Invoice inv = getInvoiceById(invoiceId);
        BigDecimal totalPaid = getTotalPaid(invoiceId);
        BigDecimal remaining = inv.getTotal().subtract(totalPaid);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    @Override
    public List<InvoiceItem> getInvoiceItems(long invoiceId) {
        return invoiceItemRepository.findByInvoiceIdOrderByIdAsc(invoiceId);
    }

    @Override
    public List<Payment> getInvoicePayments(long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaidAtDesc(invoiceId);
    }
}
